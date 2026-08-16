package com.alkacode.kits.model;

import java.util.UUID;

/** Estado persistido de um voucher fisico - a validade real mora no banco, o item fisico so carrega o {@code voucherId} (PDC). */
public record Voucher(UUID voucherId, String kitId, int level, boolean redeemed, UUID redeemedBy, long redeemedAt) {
}
