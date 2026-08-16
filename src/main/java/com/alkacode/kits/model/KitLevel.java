package com.alkacode.kits.model;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Um nivel de kit - "comprar" avanca o jogador ate aqui (paga {@code cost} na
 * {@code currencyId}, respeita {@code buyDelaySeconds} desde a ultima compra e os
 * requisitos do nivel), "reivindicar" da os itens (respeita {@code cooldownType}/
 * {@code cooldownSeconds} e {@code maxUses}). Nivel 1 pode ter custo 0 - nesse caso a
 * "compra" e instantanea, sem checagem de economia (mesmo racional generico do
 * AdvancedKits, sem caso especial pro primeiro nivel).
 */
public record KitLevel(
        int level,
        List<Requirement> requirements,
        String currencyId,
        double cost,
        long buyDelaySeconds,
        CooldownType cooldownType,
        long cooldownSeconds,
        int maxUses,
        List<ItemStack> items,
        ItemStack helmet,
        ItemStack chestplate,
        ItemStack leggings,
        ItemStack boots,
        ItemStack offhand,
        List<String> commands
) {
    public boolean isFree() {
        return cost <= 0 || currencyId == null;
    }

    public boolean isUnlimitedUses() {
        return maxUses < 0;
    }
}
