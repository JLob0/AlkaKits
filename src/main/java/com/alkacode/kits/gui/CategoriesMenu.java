package com.alkacode.kits.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.kits.manager.KitManager;
import com.alkacode.kits.model.KitCategory;
import com.alkacode.kits.util.GuiStyle;
import com.alkacode.kits.util.Messages;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.BiConsumer;

/** Menu raiz do AlkaKits - uma categoria por icone configuravel (kits.yml -&gt; categorias), clique abre {@link KitsMenu}. */
public final class CategoriesMenu extends BaseGui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final KitManager kitManager;
    private final Messages messages;
    private final BiConsumer<Player, String> openKitsMenu;

    public CategoriesMenu(JavaPlugin plugin, Player player, String title, int rows,
                           KitManager kitManager, Messages messages, BiConsumer<Player, String> openKitsMenu) {
        super(plugin, player, title, rows, "alkakits_categorias");
        this.kitManager = kitManager;
        this.messages = messages;
        this.openKitsMenu = openKitsMenu;
    }

    @Override
    public void render() {
        setItem(4, buildHeaderItem());

        for (KitCategory category : kitManager.getCategories().values()) {
            if (category.slot() < 0 || category.slot() >= inventory.getSize()) {
                continue;
            }
            setItem(category.slot(), category.icon(), e -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                openKitsMenu.accept(player, category.id());
            });
        }
        fill(GuiStyle.filler(plugin));
    }

    private ItemStack buildHeaderItem() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(messages.raw("menu.categorias-header-nome")).decoration(TextDecoration.ITALIC, false));
        meta.lore(messages.getList("menu.categorias-header-lore"));
        item.setItemMeta(meta);
        return item;
    }
}
