package com.alkacode.kits.gui;

import com.alkacode.kits.AlkaKitsPlugin;
import com.alkacode.kits.manager.KitManager;
import com.alkacode.kits.model.KitCategory;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;

/** Menu raiz do AlkaKits - uma categoria por icone configuravel (kits.yml -&gt; categorias), clique abre {@link KitsMenu}. */
public final class CategoriesMenu extends KitGui {

    private final KitManager kitManager;
    private final BiConsumer<Player, String> openKitsMenu;

    public CategoriesMenu(AlkaKitsPlugin plugin, Player player, KitManager kitManager, BiConsumer<Player, String> openKitsMenu) {
        super(plugin, player, "alkakits-categorias");
        this.kitManager = kitManager;
        this.openKitsMenu = openKitsMenu;
    }

    @Override
    public void render() {
        setAt('H', icon("header"));

        for (KitCategory category : kitManager.getCategories().values()) {
            if (category.slot() < 0 || category.slot() >= inventory.getSize()) {
                continue;
            }
            setItem(category.slot(), category.icon(), e -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                openKitsMenu.accept(player, category.id());
            });
        }
        fillRest();
    }
}
