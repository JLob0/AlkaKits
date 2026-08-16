package com.alkacode.kits.service;

import com.alkacode.kits.manager.KitProgressManager;
import com.alkacode.kits.model.Kit;
import com.alkacode.kits.model.KitLevel;
import com.alkacode.kits.model.KitProgress;
import com.alkacode.kits.model.KitStatus;
import com.alkacode.kits.model.Requirement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ponto unico de decisao/acao sobre kits - GUI e comando chamam so isto, nunca
 * duplicam a logica de status em dois lugares (mesma licao do EnderChestService no
 * AlkaEnderChest). Assume {@link KitProgressManager#isLoaded} true - callers checam
 * isso antes (GUI/comando mostram "carregando..." em vez de agir sobre progresso
 * potencialmente incompleto).
 */
public final class KitClaimService {

    private final KitProgressManager progressManager;
    private final RequirementService requirementService;
    private final KitsEconomyService economyService;
    private final FeedbackService feedbackService;

    public KitClaimService(KitProgressManager progressManager, RequirementService requirementService,
                            KitsEconomyService economyService, FeedbackService feedbackService) {
        this.progressManager = progressManager;
        this.requirementService = requirementService;
        this.economyService = economyService;
        this.feedbackService = feedbackService;
    }

    // --------------------------------------------------------------- avaliacao

    public KitStatus evaluateBuyStatus(Player player, Kit kit) {
        KitProgress progress = progressManager.getProgress(player.getUniqueId(), kit.getId());
        int nextLevel = progress.unlockedLevel() + 1;
        if (!kit.hasLevel(nextLevel)) {
            return KitStatus.MAX_LEVEL_REACHED;
        }
        KitLevel levelDef = kit.getLevel(nextLevel);

        KitStatus requirementFailure = checkRequirements(player, kit, levelDef);
        if (requirementFailure != null) {
            return requirementFailure;
        }

        long now = nowSeconds();
        if (progress.lastBuyEpochSeconds() > 0 && levelDef.buyDelaySeconds() > 0
                && (now - progress.lastBuyEpochSeconds()) < levelDef.buyDelaySeconds()) {
            return KitStatus.ON_BUY_COOLDOWN;
        }

        if (!levelDef.isFree() && !economyService.has(player.getUniqueId(), levelDef.currencyId(), levelDef.cost())) {
            return KitStatus.INSUFFICIENT_FUNDS;
        }

        return KitStatus.PURCHASABLE;
    }

    public KitStatus evaluateClaimStatus(Player player, Kit kit) {
        KitProgress progress = progressManager.getProgress(player.getUniqueId(), kit.getId());
        if (progress.unlockedLevel() < 1) {
            return KitStatus.LOCKED;
        }
        KitLevel levelDef = kit.getLevel(progress.unlockedLevel());

        long now = nowSeconds();
        if (progress.lastClaimEpochSeconds() > 0 && (now - progress.lastClaimEpochSeconds()) < levelDef.cooldownSeconds()) {
            return KitStatus.ON_CLAIM_COOLDOWN;
        }
        if (!levelDef.isUnlimitedUses() && progress.usesAtCurrentLevel() >= levelDef.maxUses()) {
            return KitStatus.MAX_USES_REACHED;
        }
        if (!hasInventorySpace(player, levelDef)) {
            return KitStatus.FULL_INVENTORY;
        }
        return KitStatus.CLAIMABLE;
    }

    private KitStatus checkRequirements(Player player, Kit kit, KitLevel levelDef) {
        List<Requirement> combined = new ArrayList<>(kit.getRequirements());
        combined.addAll(levelDef.requirements());
        for (Requirement requirement : combined) {
            if (requirement.type() == Requirement.Type.PERMISSION && !requirementService.meets(player, requirement)) {
                return KitStatus.LOCKED;
            }
        }
        for (Requirement requirement : combined) {
            if (requirement.type() != Requirement.Type.PERMISSION && !requirementService.meets(player, requirement)) {
                return KitStatus.REQUIREMENT_NOT_MET;
            }
        }
        return null;
    }

    private boolean hasInventorySpace(Player player, KitLevel levelDef) {
        if (levelDef.items().isEmpty()) {
            return true;
        }
        return player.getInventory().firstEmpty() != -1;
    }

    // -------------------------------------------------------------------- acoes

    /** Compra o proximo nivel (unlockedLevel+1). Reavalia antes de agir - se o status nao for PURCHASABLE, so dispara o feedback do motivo e nao faz nada. */
    public KitStatus buyLevel(Player player, Kit kit) {
        KitStatus status = evaluateBuyStatus(player, kit);
        UUID uuid = player.getUniqueId();
        KitProgress progress = progressManager.getProgress(uuid, kit.getId());
        int nextLevel = progress.unlockedLevel() + 1;
        KitLevel levelDef = kit.hasLevel(nextLevel) ? kit.getLevel(nextLevel) : null;

        if (status != KitStatus.PURCHASABLE) {
            feedbackService.fire(player, status, kit, levelDef);
            return status;
        }

        if (!levelDef.isFree()) {
            economyService.withdraw(uuid, levelDef.currencyId(), levelDef.cost());
        }

        KitProgress newProgress = new KitProgress(nextLevel, progress.lastClaimEpochSeconds(), 0, nowSeconds());
        progressManager.saveProgress(uuid, kit.getId(), newProgress);

        feedbackService.fire(player, KitStatus.PURCHASABLE, kit, levelDef);
        return KitStatus.PURCHASABLE;
    }

    /** Reivindica os itens do nivel atualmente desbloqueado. */
    public KitStatus claim(Player player, Kit kit) {
        KitStatus status = evaluateClaimStatus(player, kit);
        UUID uuid = player.getUniqueId();
        KitProgress progress = progressManager.getProgress(uuid, kit.getId());
        KitLevel levelDef = progress.unlockedLevel() >= 1 ? kit.getLevel(progress.unlockedLevel()) : null;

        if (status != KitStatus.CLAIMABLE) {
            feedbackService.fire(player, status, kit, levelDef);
            return status;
        }

        giveItems(player, levelDef);

        KitProgress newProgress = new KitProgress(progress.unlockedLevel(), nowSeconds(),
                progress.usesAtCurrentLevel() + 1, progress.lastBuyEpochSeconds());
        progressManager.saveProgress(uuid, kit.getId(), newProgress);

        feedbackService.fire(player, KitStatus.CLAIMABLE, kit, levelDef);
        return KitStatus.CLAIMABLE;
    }

    /**
     * Concede um nivel especifico sem checar requisito/custo/cooldown - usado por
     * vouchers e grant-triggers (ambos ja sao, por definicao, uma concessao
     * incondicional). Eleva {@code unlockedLevel} se o nivel concedido for maior que
     * o atual; se for igual ou menor, so da os itens de novo (reclaim) sem regredir o
     * progresso do jogador.
     */
    public boolean grantDirect(Player player, Kit kit, int level) {
        KitLevel levelDef = kit.getLevel(level);
        if (levelDef == null) {
            return false;
        }
        giveItems(player, levelDef);

        UUID uuid = player.getUniqueId();
        KitProgress progress = progressManager.getProgress(uuid, kit.getId());
        int newUnlocked = Math.max(progress.unlockedLevel(), level);
        int newUses = newUnlocked == progress.unlockedLevel() ? progress.usesAtCurrentLevel() + 1 : 1;
        KitProgress newProgress = new KitProgress(newUnlocked, nowSeconds(), newUses, progress.lastBuyEpochSeconds());
        progressManager.saveProgress(uuid, kit.getId(), newProgress);
        return true;
    }

    private void giveItems(Player player, KitLevel levelDef) {
        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : levelDef.items()) {
            var leftover = inventory.addItem(item.clone());
            for (ItemStack overflow : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), overflow);
            }
        }
        if (levelDef.helmet() != null) inventory.setHelmet(levelDef.helmet().clone());
        if (levelDef.chestplate() != null) inventory.setChestplate(levelDef.chestplate().clone());
        if (levelDef.leggings() != null) inventory.setLeggings(levelDef.leggings().clone());
        if (levelDef.boots() != null) inventory.setBoots(levelDef.boots().clone());
        if (levelDef.offhand() != null) inventory.setItemInOffHand(levelDef.offhand().clone());

        for (String command : levelDef.commands()) {
            String parsed = command.replace("%player%", player.getName()).replace("%uuid%", player.getUniqueId().toString());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }

    private long nowSeconds() {
        return System.currentTimeMillis() / 1000L;
    }
}
