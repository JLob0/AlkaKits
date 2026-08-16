package com.alkacode.kits.model;

/**
 * Resultado de avaliar um nivel de kit pra um jogador - guia tanto o item exibido no
 * preview (cadeado/relogio/moeda/etc) quanto o feedback disparado no clique (ver
 * FeedbackService). Um status por "aba" de nivel: niveis ja desbloqueados avaliam
 * pra claim (ON_CLAIM_COOLDOWN/FULL_INVENTORY/MAX_USES_REACHED/CLAIMABLE), o proximo
 * nivel ainda nao comprado avalia pra compra (LOCKED/REQUIREMENT_NOT_MET/
 * ON_BUY_COOLDOWN/INSUFFICIENT_FUNDS/PURCHASABLE).
 */
public enum KitStatus {
    LOCKED,
    REQUIREMENT_NOT_MET,
    ON_BUY_COOLDOWN,
    ON_CLAIM_COOLDOWN,
    INSUFFICIENT_FUNDS,
    FULL_INVENTORY,
    MAX_USES_REACHED,
    MAX_LEVEL_REACHED,
    PURCHASABLE,
    CLAIMABLE
}
