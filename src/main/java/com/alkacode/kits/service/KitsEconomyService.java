package com.alkacode.kits.service;

import com.alkacode.economy.CurrencyDefinition;
import com.alkacode.economy.EconomyManager;

import java.util.Optional;
import java.util.UUID;

/** Fina camada sobre o EconomyManager da AlkaEconomy (R4) - custo de upgrade de nivel de kit e config-driven por currencyId, nunca uma moeda fixa (feedback-currency-id-pattern). */
public final class KitsEconomyService {

    private final EconomyManager economyManager;

    public KitsEconomyService(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    public boolean isValidCurrency(String currencyId) {
        return currencyId != null && economyManager.isValidCurrency(currencyId);
    }

    public boolean has(UUID uuid, String currencyId, double amount) {
        return economyManager.has(uuid, currencyId, amount);
    }

    public void withdraw(UUID uuid, String currencyId, double amount) {
        economyManager.removeBalance(uuid, currencyId, amount);
    }

    public String getCurrencyDisplayName(String currencyId) {
        Optional<CurrencyDefinition> def = economyManager.getCurrencies().stream()
                .filter(c -> c.id().equalsIgnoreCase(currencyId))
                .findFirst();
        return def.map(CurrencyDefinition::name).orElse(currencyId);
    }

    public String formatAmount(double amount) {
        return EconomyManager.formatValue(amount);
    }
}
