package com.alkacode.kits.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.kits.manager.KitManager;
import com.alkacode.kits.manager.KitProgressManager;
import com.alkacode.kits.model.Kit;
import com.alkacode.kits.model.KitProgress;
import com.alkacode.kits.model.KitStatus;
import com.alkacode.kits.service.KitClaimService;
import com.alkacode.kits.util.GuiStyle;
import com.alkacode.kits.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Kits de uma categoria - clique abre {@link PreviewMenu} no nivel atual (ou 1, se ainda nao comprou nenhum). */
public final class KitsMenu extends BaseGui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final String category;
    private final KitManager kitManager;
    private final KitProgressManager progressManager;
    private final KitClaimService claimService;
    private final Messages messages;
    private final Consumer<Player> openCategories;
    private final java.util.function.BiConsumer<Player, Kit> openPreview;

    public KitsMenu(JavaPlugin plugin, Player player, String title, int rows, String category,
                     KitManager kitManager, KitProgressManager progressManager, KitClaimService claimService,
                     Messages messages, Consumer<Player> openCategories, java.util.function.BiConsumer<Player, Kit> openPreview) {
        super(plugin, player, title, rows, "alkakits_kits");
        this.category = category;
        this.kitManager = kitManager;
        this.progressManager = progressManager;
        this.claimService = claimService;
        this.messages = messages;
        this.openCategories = openCategories;
        this.openPreview = openPreview;
    }

    @Override
    public void render() {
        setItem(4, buildHeaderItem());

        int lastRow = (inventory.getSize() / 9) - 1;
        setItem(lastRow * 9 + 4, buildBackButton(), e -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            openCategories.accept(player);
        });

        for (Kit kit : kitManager.getKitsInCategory(category)) {
            if (kit.getSlot() < 0 || kit.getSlot() >= inventory.getSize()) {
                continue;
            }
            setItem(kit.getSlot(), buildKitIcon(kit), e -> openPreview.accept(player, kit));
        }

        fill(GuiStyle.filler(plugin));
    }

    private ItemStack buildHeaderItem() {
        com.alkacode.kits.model.KitCategory categoryInfo = kitManager.getCategories().get(category);
        ItemStack item = categoryInfo != null ? categoryInfo.icon().clone() : new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.lore(messages.getList("menu.categorias-header-lore"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildKitIcon(Kit kit) {
        ItemStack item = kit.getIcon().clone();
        ItemMeta meta = item.getItemMeta();

        KitProgress progress = progressManager.getProgress(player.getUniqueId(), kit.getId());
        int maxLevel = kit.getMaxLevel();

        List<Component> lore = new ArrayList<>(meta.hasLore() && meta.lore() != null ? meta.lore() : List.of());
        lore.addAll(messages.getList("menu.kit-nivel-lore",
                "<atual>", String.valueOf(progress.unlockedLevel()), "<maximo>", String.valueOf(maxLevel)));
        meta.lore(lore);

        boolean actionable = claimService.evaluateClaimStatus(player, kit) == KitStatus.CLAIMABLE
                || claimService.evaluateBuyStatus(player, kit) == KitStatus.PURCHASABLE;
        if (actionable) {
            GuiStyle.glow(meta);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(messages.raw("menu.voltar-nome")).decoration(TextDecoration.ITALIC, false));
        meta.lore(messages.getList("menu.voltar-lore"));
        item.setItemMeta(meta);
        return item;
    }
}
