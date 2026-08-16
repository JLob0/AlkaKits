package com.alkacode.kits.manager;

import com.alkacode.kits.model.CooldownType;
import com.alkacode.kits.model.Kit;
import com.alkacode.kits.model.KitCategory;
import com.alkacode.kits.model.KitLevel;
import com.alkacode.kits.model.Requirement;
import com.alkacode.kits.util.ItemBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carrega kits.yml pra memoria - "quantos kits/niveis o admin quiser", mesma
 * filosofia config-driven do rewards.yml do AlkaTime. Niveis sao 1-indexados e nao
 * podem ter buracos (nivel 3 sem nivel 2 antes) - avisa e ignora o kit inteiro se
 * a numeracao vier quebrada, pra nao deixar Kit#getMaxLevel mentiroso.
 */
public final class KitManager {

    private final JavaPlugin plugin;
    private final Map<String, Kit> kits = new LinkedHashMap<>();
    private final Map<String, KitCategory> categories = new LinkedHashMap<>();

    public KitManager(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "kits.yml");
        if (!file.exists()) {
            try (var in = plugin.getResource("kits.yml")) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Nao foi possivel criar kits.yml: " + e.getMessage());
            }
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        kits.clear();
        categories.clear();
        loadCategories(config.getConfigurationSection("categorias"));
        loadKits(config.getConfigurationSection("kits"));
    }

    private void loadCategories(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection cat = section.getConfigurationSection(id);
            if (cat == null) {
                continue;
            }
            ItemStack icon = ItemBuilder.fromConfig(cat.getConfigurationSection("icone"), plugin.getLogger());
            categories.put(id, new KitCategory(id, cat.getString("nome", id), icon, cat.getInt("slot", 0)));
        }
    }

    private void loadKits(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection kitSection = section.getConfigurationSection(id);
            if (kitSection == null) {
                continue;
            }
            Kit kit = parseKit(id, kitSection);
            if (kit != null) {
                kits.put(id, kit);
            }
        }
    }

    private Kit parseKit(String id, ConfigurationSection kitSection) {
        String category = kitSection.getString("categoria", "default");
        ItemStack icon = ItemBuilder.fromConfig(kitSection.getConfigurationSection("icone"), plugin.getLogger());
        List<Requirement> requirements = parseRequirements(kitSection.getMapList("requisitos"));

        ConfigurationSection levelsSection = kitSection.getConfigurationSection("niveis");
        if (levelsSection == null) {
            plugin.getLogger().warning("Kit '" + id + "' sem secao 'niveis' - ignorado.");
            return null;
        }

        Map<Integer, KitLevel> levels = new LinkedHashMap<>();
        for (String levelKey : levelsSection.getKeys(false)) {
            int levelNumber;
            try {
                levelNumber = Integer.parseInt(levelKey);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Chave de nivel invalida '" + levelKey + "' no kit '" + id + "' - ignorada.");
                continue;
            }
            ConfigurationSection levelSection = levelsSection.getConfigurationSection(levelKey);
            levels.put(levelNumber, parseLevel(levelNumber, levelSection));
        }

        for (int i = 1; i <= levels.size(); i++) {
            if (!levels.containsKey(i)) {
                plugin.getLogger().warning("Kit '" + id + "' tem numeracao de niveis quebrada (falta nivel " + i + ") - ignorado.");
                return null;
            }
        }

        return new Kit(id, category, kitSection.getInt("slot", 0), icon, requirements, levels);
    }

    private KitLevel parseLevel(int levelNumber, ConfigurationSection section) {
        List<Requirement> requirements = parseRequirements(section.getMapList("requisitos"));
        String currencyId = section.getString("moeda");
        double cost = section.getDouble("custo", 0);
        long buyDelay = section.getLong("buy-delay-segundos", 0);

        CooldownType cooldownType = parseCooldownType(section.getString("cooldown-tipo", "DAILY"));
        long cooldownSeconds = resolveCooldownSeconds(cooldownType, section.getLong("cooldown-segundos", 0));

        int maxUses = section.getInt("usos-maximos", -1);

        List<ItemStack> items = new ArrayList<>();
        for (Map<?, ?> rawItem : section.getMapList("itens")) {
            items.add(ItemBuilder.fromConfig(toSection(rawItem), plugin.getLogger()));
        }

        ConfigurationSection armor = section.getConfigurationSection("armadura");
        ItemStack helmet = armor != null ? nullableItem(armor.getConfigurationSection("capacete")) : null;
        ItemStack chestplate = armor != null ? nullableItem(armor.getConfigurationSection("peitoral")) : null;
        ItemStack leggings = armor != null ? nullableItem(armor.getConfigurationSection("calca")) : null;
        ItemStack boots = armor != null ? nullableItem(armor.getConfigurationSection("bota")) : null;
        ItemStack offhand = nullableItem(section.getConfigurationSection("offhand"));

        List<String> commands = section.getStringList("comandos");

        return new KitLevel(levelNumber, requirements, currencyId, cost, buyDelay,
                cooldownType, cooldownSeconds, maxUses, items, helmet, chestplate, leggings, boots, offhand, commands);
    }

    private ItemStack nullableItem(ConfigurationSection section) {
        return section == null ? null : ItemBuilder.fromConfig(section, plugin.getLogger());
    }

    /** getMapList() devolve Map cru (nao ConfigurationSection) - embrulha num YamlConfiguration solto pra reusar ItemBuilder.fromConfig sem duplicar o parsing. */
    private ConfigurationSection toSection(Map<?, ?> raw) {
        YamlConfiguration temp = new YamlConfiguration();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            temp.set(String.valueOf(entry.getKey()), entry.getValue());
        }
        return temp;
    }

    private CooldownType parseCooldownType(String raw) {
        try {
            return CooldownType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("cooldown-tipo invalido '" + raw + "' - usando DAILY.");
            return CooldownType.DAILY;
        }
    }

    private long resolveCooldownSeconds(CooldownType type, long custom) {
        return switch (type) {
            case ONCE -> Long.MAX_VALUE;
            case DAILY -> 86400L;
            case WEEKLY -> 604800L;
            case MONTHLY -> 2592000L;
            case CUSTOM -> custom;
        };
    }

    private List<Requirement> parseRequirements(List<Map<?, ?>> rawList) {
        List<Requirement> requirements = new ArrayList<>();
        if (rawList == null) {
            return requirements;
        }
        for (Map<?, ?> raw : rawList) {
            String typeRaw = String.valueOf(raw.get("tipo"));
            String value = String.valueOf(raw.get("valor"));
            try {
                Requirement.Type type = Requirement.Type.valueOf(typeRaw.toUpperCase());
                if (type == Requirement.Type.PLACEHOLDER) {
                    Object operatorRaw = raw.get("operador");
                    Requirement.Operator operator = Requirement.Operator.valueOf(
                            (operatorRaw != null ? String.valueOf(operatorRaw) : "EQUALS").toUpperCase());
                    String comparisonValue = String.valueOf(raw.get("comparar"));
                    requirements.add(Requirement.placeholder(value, operator, comparisonValue));
                } else if (type == Requirement.Type.WORLD) {
                    requirements.add(Requirement.world(value));
                } else {
                    requirements.add(Requirement.permission(value));
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Requisito invalido no kits.yml: " + raw);
            }
        }
        return requirements;
    }

    public Kit getKit(String id) {
        return kits.get(id);
    }

    public boolean hasKit(String id) {
        return kits.containsKey(id);
    }

    public Map<String, Kit> getKits() {
        return kits;
    }

    public List<Kit> getKitsInCategory(String category) {
        return kits.values().stream().filter(k -> k.getCategory().equals(category)).toList();
    }

    public Map<String, KitCategory> getCategories() {
        return categories;
    }
}
