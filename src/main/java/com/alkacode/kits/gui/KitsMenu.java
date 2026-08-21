package com.alkacode.kits.gui;

import com.alkacode.kits.AlkaKitsPlugin;
import com.alkacode.kits.manager.KitManager;
import com.alkacode.kits.manager.KitProgressManager;
import com.alkacode.kits.model.Kit;
import com.alkacode.kits.model.KitProgress;
import com.alkacode.kits.model.KitStatus;
import com.alkacode.kits.service.KitClaimService;
import com.alkacode.kits.util.GuiStyle;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Kits de uma categoria - clique abre {@link PreviewMenu} no nivel atual (ou 1, se ainda nao comprou nenhum). */
public final class KitsMenu extends KitGui {

    private final String category;
    private final KitManager kitManager;
    private final KitProgressManager progressManager;
    private final KitClaimService claimService;
    private final Consumer<Player> openCategories;
    private final java.util.function.BiConsumer<Player, Kit> openPreview;

    public KitsMenu(AlkaKitsPlugin plugin, Player player, String category,
                     KitManager kitManager, KitProgressManager progressManager, KitClaimService claimService,
                     Consumer<Player> openCategories, java.util.function.BiConsumer<Player, Kit> openPreview) {
        super(plugin, player, "alkakits-kits");
        this.category = category;
        this.kitManager = kitManager;
        this.progressManager = progressManager;
        this.claimService = claimService;
        this.openCategories = openCategories;
        this.openPreview = openPreview;
    }

    @Override
    public void render() {
        setAt('H', buildHeaderItem());
        setAt('V', commonVoltar(), e -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            openCategories.accept(player);
        });

        for (Kit kit : kitManager.getKitsInCategory(category)) {
            if (kit.getSlot() < 0 || kit.getSlot() >= inventory.getSize()) {
                continue;
            }
            setItem(kit.getSlot(), buildKitIcon(kit), e -> openPreview.accept(player, kit));
        }

        fillRest();
    }

    private ItemStack buildHeaderItem() {
        com.alkacode.kits.model.KitCategory categoryInfo = kitManager.getCategories().get(category);
        ItemStack item = categoryInfo != null ? categoryInfo.icon().clone() : new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.lore(menu().lore(id + ".header", null));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildKitIcon(Kit kit) {
        ItemStack item = kit.getIcon().clone();
        ItemMeta meta = item.getItemMeta();

        KitProgress progress = progressManager.getProgress(player.getUniqueId(), kit.getId());
        int maxLevel = kit.getMaxLevel();

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>(meta.hasLore() && meta.lore() != null ? meta.lore() : List.of());
        lore.addAll(menu().lore(id + ".kit-nivel-lore",
                Map.of("atual", String.valueOf(progress.unlockedLevel()), "maximo", String.valueOf(maxLevel))));
        meta.lore(lore);

        boolean actionable = claimService.evaluateClaimStatus(player, kit) == KitStatus.CLAIMABLE
                || claimService.evaluateBuyStatus(player, kit) == KitStatus.PURCHASABLE;
        if (actionable) {
            GuiStyle.glow(meta);
        }

        item.setItemMeta(meta);
        return item;
    }
}
