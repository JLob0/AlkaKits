package com.alkacode.kits.model;

/** Progresso de um jogador num kit especifico. {@code unlockedLevel} 0 = nunca comprou o nivel 1 ainda. */
public record KitProgress(int unlockedLevel, long lastClaimEpochSeconds, int usesAtCurrentLevel, long lastBuyEpochSeconds) {

    public static final KitProgress EMPTY = new KitProgress(0, 0, 0, 0);
}
