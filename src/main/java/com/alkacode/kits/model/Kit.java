package com.alkacode.kits.model;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Um kit - grupo ordenado de {@link KitLevel} (chave = numero do nivel, 1-indexado,
 * sem buracos por construcao - ver KitManager). {@code requirements} aqui se somam
 * aos do nivel individual (ex: kit inteiro exige um mundo, cada nivel ainda pode
 * pedir permissao propria).
 */
public final class Kit {

    private final String id;
    private final String category;
    private final int slot;
    private final ItemStack icon;
    private final List<Requirement> requirements;
    private final TreeMap<Integer, KitLevel> levels;

    public Kit(String id, String category, int slot, ItemStack icon, List<Requirement> requirements, Map<Integer, KitLevel> levels) {
        this.id = id;
        this.category = category;
        this.slot = slot;
        this.icon = icon;
        this.requirements = requirements;
        this.levels = new TreeMap<>(levels);
    }

    public int getSlot() {
        return slot;
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    public int getMaxLevel() {
        return levels.isEmpty() ? 0 : levels.lastKey();
    }

    public KitLevel getLevel(int level) {
        return levels.get(level);
    }

    public boolean hasLevel(int level) {
        return levels.containsKey(level);
    }

    public List<KitLevel> getOrderedLevels() {
        return List.copyOf(levels.values());
    }
}
