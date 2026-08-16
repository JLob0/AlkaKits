package com.alkacode.kits.placeholder;

import com.alkacode.kits.manager.KitManager;
import com.alkacode.kits.manager.KitProgressManager;
import com.alkacode.kits.model.Kit;
import com.alkacode.kits.model.KitProgress;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * %alkakits_nivel_&lt;kit&gt;%, %alkakits_max_&lt;kit&gt;%, %alkakits_cooldown_&lt;kit&gt;% (segundos
 * restantes ate poder reivindicar de novo, 0 se ja disponivel) - le so do cache em
 * memoria (KitProgressManager), nunca consulta o banco na hora do request (R7 vale
 * pra qualquer thread que o PAPI use pra chamar isso).
 */
public final class AlkaKitsExpansion extends PlaceholderExpansion {

    private final KitManager kitManager;
    private final KitProgressManager progressManager;

    public AlkaKitsExpansion(KitManager kitManager, KitProgressManager progressManager) {
        this.kitManager = kitManager;
        this.progressManager = progressManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "alkakits";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MestreDEV";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null || params == null) {
            return "";
        }
        int separator = params.lastIndexOf('_');
        if (separator <= 0 || separator == params.length() - 1) {
            return null;
        }
        String type = params.substring(0, separator).toLowerCase();
        String kitId = params.substring(separator + 1);

        Kit kit = kitManager.getKit(kitId);
        if (kit == null) {
            return null;
        }
        KitProgress progress = progressManager.getProgress(player.getUniqueId(), kitId);

        return switch (type) {
            case "nivel" -> String.valueOf(progress.unlockedLevel());
            case "max" -> String.valueOf(kit.getMaxLevel());
            case "cooldown" -> String.valueOf(remainingCooldown(kit, progress));
            default -> null;
        };
    }

    private long remainingCooldown(Kit kit, KitProgress progress) {
        if (progress.unlockedLevel() < 1) {
            return 0;
        }
        var levelDef = kit.getLevel(progress.unlockedLevel());
        if (levelDef == null || progress.lastClaimEpochSeconds() <= 0) {
            return 0;
        }
        long now = System.currentTimeMillis() / 1000L;
        long remaining = levelDef.cooldownSeconds() - (now - progress.lastClaimEpochSeconds());
        return Math.max(0, remaining);
    }
}
