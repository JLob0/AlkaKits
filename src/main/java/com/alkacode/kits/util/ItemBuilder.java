package com.alkacode.kits.util;

import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Constroi ItemStack a partir de config - versao mais rica que a de outros plugins
 * Alka* (amount/encantamentos "unsafe"/unbreakable/flags) porque itens de kit
 * precisam disso pra reproduzir gear com encantamento acima do vanilla max, algo que
 * o AdvancedKits/KTempo faziam via NBT bruto. Aqui fica tudo declarativo em YAML.
 */
public final class ItemBuilder {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private ItemBuilder() {
    }

    public static ItemStack fromConfig(ConfigurationSection section, Logger logger) {
        if (section == null) {
            return new ItemStack(Material.STONE);
        }

        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        if (material == null) {
            logger.warning("Material invalido em " + section.getCurrentPath() + " - usando STONE.");
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material, Math.max(1, section.getInt("amount", 1)));
        ItemMeta meta = item.getItemMeta();

        String name = section.getString("name");
        if (name != null) {
            meta.displayName(MM.deserialize(name).decoration(TextDecoration.ITALIC, false));
        }

        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            meta.lore(lore.stream()
                    .map(line -> MM.deserialize(line).decoration(TextDecoration.ITALIC, false))
                    .collect(Collectors.toList()));
        }

        if (section.contains("custom_model_data")) {
            meta.setCustomModelData(section.getInt("custom_model_data"));
        }

        meta.setUnbreakable(section.getBoolean("unbreakable", false));

        for (String flagName : section.getStringList("item_flags")) {
            try {
                meta.addItemFlags(ItemFlag.valueOf(flagName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                logger.warning("ItemFlag invalido '" + flagName + "' em " + section.getCurrentPath());
            }
        }

        item.setItemMeta(meta);

        ConfigurationSection enchants = section.getConfigurationSection("enchantments");
        if (enchants != null) {
            for (String key : enchants.getKeys(false)) {
                Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(key.toLowerCase()));
                if (enchantment == null) {
                    logger.warning("Encantamento invalido '" + key + "' em " + section.getCurrentPath());
                    continue;
                }
                item.addUnsafeEnchantment(enchantment, enchants.getInt(key, 1));
            }
        }

        return item;
    }
}
