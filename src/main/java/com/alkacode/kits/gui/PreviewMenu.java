package com.alkacode.kits.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.kits.model.Kit;
import com.alkacode.kits.model.KitLevel;
import com.alkacode.kits.model.KitProgress;
import com.alkacode.kits.model.KitStatus;
import com.alkacode.kits.manager.KitProgressManager;
import com.alkacode.kits.service.KitClaimService;
import com.alkacode.kits.service.KitsEconomyService;
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

import java.util.function.Consumer;

/**
 * Preview paginado por nivel de um kit - nivel &lt; atual e so informativo (ja
 * superado), nivel == atual mostra o botao de reivindicar, nivel == atual+1 mostra o
 * botao de comprar. Navegacao nunca vai alem de atual+1 (nao da pra "espiar" niveis
 * que ainda dependem de comprar os anteriores).
 */
public final class PreviewMenu extends BaseGui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Kit kit;
    private final KitClaimService claimService;
    private final KitProgressManager progressManager;
    private final KitsEconomyService economyService;
    private final Messages messages;
    private final Consumer<Player> openKitsMenu;

    private int viewingLevel;

    public PreviewMenu(JavaPlugin plugin, Player player, String title, int rows, Kit kit, int initialLevel,
                        KitClaimService claimService, KitProgressManager progressManager,
                        KitsEconomyService economyService, Messages messages, Consumer<Player> openKitsMenu) {
        super(plugin, player, title, rows, "alkakits_preview");
        this.kit = kit;
        this.viewingLevel = Math.max(1, initialLevel);
        this.claimService = claimService;
        this.progressManager = progressManager;
        this.economyService = economyService;
        this.messages = messages;
        this.openKitsMenu = openKitsMenu;
    }

    @Override
    public void render() {
        KitProgress progress = progressManager.getProgress(player.getUniqueId(), kit.getId());
        int maxViewable = Math.max(1, Math.min(kit.getMaxLevel(), progress.unlockedLevel() + 1));
        viewingLevel = Math.max(1, Math.min(viewingLevel, maxViewable));
        KitLevel levelDef = kit.getLevel(viewingLevel);

        setItem(4, buildInfoItem());

        int slot = 10;
        for (ItemStack item : levelDef.items()) {
            if (slot > 16) break;
            setItem(slot++, item.clone());
        }
        if (levelDef.helmet() != null) setItem(19, levelDef.helmet().clone());
        if (levelDef.chestplate() != null) setItem(20, levelDef.chestplate().clone());
        if (levelDef.leggings() != null) setItem(21, levelDef.leggings().clone());
        if (levelDef.boots() != null) setItem(22, levelDef.boots().clone());
        if (levelDef.offhand() != null) setItem(23, levelDef.offhand().clone());

        if (viewingLevel > 1) {
            setItem(29, navButton("menu.nivel-anterior-nome"), e -> {
                viewingLevel--;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
                refresh();
            });
        }
        if (viewingLevel < maxViewable) {
            setItem(33, navButton("menu.proximo-nivel-nome"), e -> {
                viewingLevel++;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
                refresh();
            });
        }

        renderActionButton(progress, levelDef);

        int lastRow = (inventory.getSize() / 9) - 1;
        setItem(lastRow * 9 + 4, backButton(), e -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            openKitsMenu.accept(player);
        });

        fill(GuiStyle.filler(plugin));
    }

    private void renderActionButton(KitProgress progress, KitLevel levelDef) {
        if (viewingLevel < progress.unlockedLevel()) {
            setItem(31, pastLevelItem());
            return;
        }
        if (viewingLevel == progress.unlockedLevel() && viewingLevel >= 1) {
            KitStatus status = claimService.evaluateClaimStatus(player, kit);
            setItem(31, statusItem(status, levelDef), status == KitStatus.CLAIMABLE ? e -> {
                claimService.claim(player, kit);
                refresh();
            } : null);
            return;
        }
        // unico caso restante: viewingLevel == progress.unlockedLevel() + 1 (proximo nivel comspravel)
        KitStatus status = claimService.evaluateBuyStatus(player, kit);
        setItem(31, statusItem(status, levelDef), status == KitStatus.PURCHASABLE ? e -> {
            claimService.buyLevel(player, kit);
            refresh();
        } : null);
    }

    private ItemStack statusItem(KitStatus status, KitLevel levelDef) {
        Material material = switch (status) {
            case CLAIMABLE, PURCHASABLE -> Material.EMERALD;
            case LOCKED, REQUIREMENT_NOT_MET -> Material.BARRIER;
            case ON_BUY_COOLDOWN, ON_CLAIM_COOLDOWN -> Material.CLOCK;
            case INSUFFICIENT_FUNDS -> Material.GOLD_NUGGET;
            case FULL_INVENTORY -> Material.CHEST;
            case MAX_USES_REACHED -> Material.REDSTONE_BLOCK;
            case MAX_LEVEL_REACHED -> Material.NETHER_STAR;
        };
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String preco = !levelDef.isFree() ? economyService.formatAmount(levelDef.cost()) : "";
        String moeda = !levelDef.isFree() ? economyService.getCurrencyDisplayName(levelDef.currencyId()) : "";

        meta.displayName(MM.deserialize(messages.raw("menu.botao." + status.name() + ".nome")).decoration(TextDecoration.ITALIC, false));
        meta.lore(messages.getList("menu.botao." + status.name() + ".lore", "<preco>", preco, "<moeda>", moeda));

        if (status == KitStatus.CLAIMABLE || status == KitStatus.PURCHASABLE) {
            GuiStyle.glow(meta);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack pastLevelItem() {
        ItemStack item = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(messages.raw("menu.nivel-superado-nome")).decoration(TextDecoration.ITALIC, false));
        meta.lore(messages.getList("menu.nivel-superado-lore"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildInfoItem() {
        ItemStack item = kit.getIcon().clone();
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(messages.raw("menu.preview-info-nome")
                        .replace("<nivel>", String.valueOf(viewingLevel)).replace("<maximo>", String.valueOf(kit.getMaxLevel())))
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(messages.getList("menu.preview-info-lore"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack navButton(String messageKey) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(messages.raw(messageKey)).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.OAK_DOOR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(messages.raw("menu.voltar-nome")).decoration(TextDecoration.ITALIC, false));
        meta.lore(messages.getList("menu.voltar-lore"));
        item.setItemMeta(meta);
        return item;
    }
}
