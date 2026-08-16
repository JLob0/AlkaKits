package com.alkacode.kits.model;

import org.bukkit.inventory.ItemStack;

public record KitCategory(String id, String displayName, ItemStack icon, int slot) {
}
