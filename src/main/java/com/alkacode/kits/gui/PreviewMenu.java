package com.alkacode.kits.gui;

import com.alkacode.kits.AlkaKitsPlugin;
import com.alkacode.kits.model.Kit;
import com.alkacode.kits.model.KitLevel;
import com.alkacode.kits.model.KitProgress;
import com.alkacode.kits.model.KitStatus;
import com.alkacode.kits.manager.KitProgressManager;
import com.alkacode.kits.service.KitClaimService;
import com.alkacode.kits.service.KitsEconomyService;
import com.alkacode.kits.util.GuiStyle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Preview paginado por nivel de um kit - nivel &lt; atual e so informativo (ja
 * superado), nivel == atual mostra o botao de reivindicar, nivel == atual+1 mostra o
 * botao de comprar. Navegacao nunca vai alem de atual+1 (nao da pra "espiar" niveis
 * que ainda dependem de comprar os anteriores).
 */
public final class PreviewMenu extends KitGui {

    private final Kit kit;
    private final KitClaimService claimService;
    private final KitProgressManager progressManager;
    private final KitsEconomyService economyService;
    private final Consumer<Player> openKitsMenu;

    private int viewingLevel;

    public PreviewMenu(AlkaKitsPlugin plugin, Player player, Kit kit, int initialLevel,
                        KitClaimService claimService, KitProgressManager progressManager,
                        KitsEconomyService economyService, Consumer<Player> openKitsMenu) {
        super(plugin, player, "alkakits-preview");
        this.kit = kit;
        this.viewingLevel = Math.max(1, initialLevel);
        this.claimService = claimService;
        this.progressManager = progressManager;
        this.economyService = economyService;
        this.openKitsMenu = openKitsMenu;
    }

    @Override
    public void render() {
        KitProgress progress = progressManager.getProgress(player.getUniqueId(), kit.getId());
        int maxViewable = Math.max(1, Math.min(kit.getMaxLevel(), progress.unlockedLevel() + 1));
        viewingLevel = Math.max(1, Math.min(viewingLevel, maxViewable));
        KitLevel levelDef = kit.getLevel(viewingLevel);

        setAt('H', buildInfoItem());

        int i = 0;
        var itemSlots = layout().findSlots('0');
        for (ItemStack item : levelDef.items()) {
            if (i >= itemSlots.size()) break;
            setItem(itemSlots.get(i++), item.clone());
        }
        if (levelDef.helmet() != null) setAt('W', levelDef.helmet().clone());
        if (levelDef.chestplate() != null) setAt('C', levelDef.chestplate().clone());
        if (levelDef.leggings() != null) setAt('L', levelDef.leggings().clone());
        if (levelDef.boots() != null) setAt('B', levelDef.boots().clone());
        if (levelDef.offhand() != null) setAt('O', levelDef.offhand().clone());

        if (viewingLevel > 1) {
            setAt('P', icon("nav-anterior"), e -> {
                viewingLevel--;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
                refresh();
            });
        }
        if (viewingLevel < maxViewable) {
            setAt('N', icon("nav-proximo"), e -> {
                viewingLevel++;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
                refresh();
            });
        }

        renderActionButton(progress, levelDef);

        setAt('V', icon("voltar"), e -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            openKitsMenu.accept(player);
        });

        fillRest();
    }

    private void renderActionButton(KitProgress progress, KitLevel levelDef) {
        if (viewingLevel < progress.unlockedLevel()) {
            setAt('A', icon("nivel-superado"));
            return;
        }
        if (viewingLevel == progress.unlockedLevel() && viewingLevel >= 1) {
            KitStatus status = claimService.evaluateClaimStatus(player, kit);
            setAt('A', statusItem(status, levelDef), status == KitStatus.CLAIMABLE ? e -> {
                claimService.claim(player, kit);
                refresh();
            } : null);
            return;
        }
        // unico caso restante: viewingLevel == progress.unlockedLevel() + 1 (proximo nivel comspravel)
        KitStatus status = claimService.evaluateBuyStatus(player, kit);
        setAt('A', statusItem(status, levelDef), status == KitStatus.PURCHASABLE ? e -> {
            claimService.buyLevel(player, kit);
            refresh();
        } : null);
    }

    private ItemStack statusItem(KitStatus status, KitLevel levelDef) {
        String preco = !levelDef.isFree() ? economyService.formatAmount(levelDef.cost()) : "";
        String moeda = !levelDef.isFree() ? economyService.getCurrencyDisplayName(levelDef.currencyId()) : "";

        ItemStack item = icon("botao." + status.name(), Map.of("preco", preco, "moeda", moeda));
        if (status == KitStatus.CLAIMABLE || status == KitStatus.PURCHASABLE) {
            ItemMeta meta = item.getItemMeta();
            GuiStyle.glow(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildInfoItem() {
        ItemStack item = kit.getIcon().clone();
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize("<!i>" + menu().name(id + ".header", Map.of(
                        "nivel", String.valueOf(viewingLevel), "maximo", String.valueOf(kit.getMaxLevel())))));
        meta.lore(menu().lore(id + ".header", null));
        item.setItemMeta(meta);
        return item;
    }
}
