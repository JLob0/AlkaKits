package com.alkacode.kits.model;

import org.bukkit.inventory.ItemStack;

/** Sub-menu de kits dentro de uma categoria (ex: "Campones" agrupando os 3 kits
 * diario/semanal/mensal daquele rank) - ver {@link Kit#getGroup()}. */
public record KitGroup(String id, String displayName, ItemStack icon, int slot) {
}
