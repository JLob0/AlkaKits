package com.alkacode.kits.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/** Elementos visuais compartilhados entre as 3 GUIs do AlkaKits - so estrutura (material/brilho), nunca texto (isso mora sempre em messages.yml). */
public final class GuiStyle {

    private GuiStyle() {
    }

    /** Material do preenchimento de borda - config-driven (menu.filler-material) em vez de fixo no Java, pra dar pra trocar sem recompilar. */
    public static ItemStack filler(JavaPlugin plugin) {
        String raw = plugin.getConfig().getString("menu.filler-material", "GRAY_STAINED_GLASS_PANE");
        Material material = Material.matchMaterial(raw);
        ItemStack item = new ItemStack(material != null ? material : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    /** Trick classico de "brilho sem encantamento visivel" pra marcar um item como disponivel/conquistado - mesmo padrao do EnderChestUpgradeGui. */
    public static void glow(ItemMeta meta) {
        meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
    }
}
