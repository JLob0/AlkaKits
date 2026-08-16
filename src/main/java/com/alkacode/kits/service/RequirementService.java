package com.alkacode.kits.service;

import com.alkacode.kits.model.Requirement;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * Avalia requisitos de permissao/mundo/placeholder (R4 nao se aplica aqui - isso e
 * fora do escopo de moeda). PLACEHOLDER so funciona com PlaceholderAPI instalado
 * (compileOnly de verdade, nao reflexao - mesmo padrao ja usado por AlkaEconomy/
 * AlkaVips pra hooks de PAPI, diferente do Citizens que nao tem artefato Maven
 * confiavel) - sem o plugin presente, falha fechado (nunca desbloqueia por engano).
 */
public final class RequirementService {

    private final boolean placeholderApiAvailable;

    public RequirementService() {
        this.placeholderApiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public boolean meetsAll(Player player, List<Requirement> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            return true;
        }
        for (Requirement requirement : requirements) {
            if (!meets(player, requirement)) {
                return false;
            }
        }
        return true;
    }

    public boolean meets(Player player, Requirement requirement) {
        return switch (requirement.type()) {
            case PERMISSION -> player.hasPermission(requirement.value());
            case WORLD -> player.getWorld().getName().equalsIgnoreCase(requirement.value());
            case PLACEHOLDER -> meetsPlaceholder(player, requirement);
        };
    }

    private boolean meetsPlaceholder(Player player, Requirement requirement) {
        if (!placeholderApiAvailable) {
            return false;
        }
        String resolved = PlaceholderAPI.setPlaceholders(player, requirement.value());

        Double resolvedNumber = tryParse(resolved);
        Double comparisonNumber = tryParse(requirement.comparisonValue());
        if (resolvedNumber != null && comparisonNumber != null) {
            int cmp = Double.compare(resolvedNumber, comparisonNumber);
            return switch (requirement.operator()) {
                case EQUALS -> cmp == 0;
                case NOT_EQUALS -> cmp != 0;
                case GREATER_EQUALS -> cmp >= 0;
                case LESS_EQUALS -> cmp <= 0;
                case GREATER -> cmp > 0;
                case LESS -> cmp < 0;
            };
        }

        // sem numero em algum dos dois lados, so EQUALS/NOT_EQUALS fazem sentido (comparacao textual).
        boolean equal = resolved.equalsIgnoreCase(requirement.comparisonValue());
        return switch (requirement.operator()) {
            case EQUALS -> equal;
            case NOT_EQUALS -> !equal;
            default -> false;
        };
    }

    private Double tryParse(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim().toLowerCase(Locale.ROOT).replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
