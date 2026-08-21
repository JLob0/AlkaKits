package com.alkacode.kits.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.kits.AlkaKitsPlugin;
import com.alkacode.kits.gui.layout.GuiLayoutLoader;
import com.alkacode.kits.util.GuiStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Base das GUIs do AlkaKits: titulo/linhas vem de gui-layouts.yml, icone/texto de
 * menus.yml (ver R8 no CLAUDE.md) - essa classe so oferece os helpers de wiring
 * (layout/setAt/icon), igual ao ClanGui do AlkaClans.
 */
public abstract class KitGui extends BaseGui {

    protected final AlkaKitsPlugin plugin;
    protected final String id;

    protected KitGui(AlkaKitsPlugin plugin, Player player, String id) {
        super(plugin, player, plugin.getMenuConfig().title(id + ".title", null),
                plugin.getGuiLayoutLoader().getLayout(id).rows(), id);
        this.plugin = plugin;
        this.id = id;
    }

    protected com.alkacode.kits.config.MenuConfig menu() {
        return plugin.getMenuConfig();
    }

    protected GuiLayoutLoader.GuiLayout layout() {
        return plugin.getGuiLayoutLoader().getLayout(id);
    }

    protected void setAt(char c, ItemStack item, Consumer<InventoryClickEvent> action) {
        int slot = layout().firstSlot(c);
        if (slot >= 0) setItem(slot, item, action);
    }

    protected void setAt(char c, ItemStack item) {
        setAt(c, item, null);
    }

    /** Icone de menus.yml.<id>.<path> com placeholders. */
    protected ItemStack icon(String path, Map<String, String> placeholders) {
        return menu().item(id + "." + path, placeholders);
    }

    protected ItemStack icon(String path) {
        return icon(path, null);
    }

    protected ItemStack commonVoltar() {
        return menu().item("common.voltar", null);
    }

    /** Preenche o restante do inventario com o filler config-driven (menu.filler-material). */
    protected void fillRest() {
        fill(GuiStyle.filler(plugin));
    }
}
