package com.alkacode.kits.database;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;
import com.alkacode.kits.model.KitProgress;
import com.alkacode.kits.model.Voucher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Unico ponto de acesso ao banco do AlkaKits, sobre o DatabaseProvider do AlkaCore (R2). Todas as chamadas bloqueantes de proposito - callers rodam fora da main thread (R7), ver KitProgressManager. */
public final class KitsRepository extends AbstractRepository {

    private final Logger logger;

    public KitsRepository(DatabaseProvider db, Logger logger) {
        super(db);
        this.logger = logger;
        createTables();
    }

    private void createTables() {
        try (Connection conn = db.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS alka_kits_progress (
                        player_uuid VARCHAR(36) NOT NULL,
                        kit_id VARCHAR(64) NOT NULL,
                        unlocked_level INT DEFAULT 0,
                        last_claim_epoch BIGINT DEFAULT 0,
                        uses_at_current_level INT DEFAULT 0,
                        last_buy_epoch BIGINT DEFAULT 0,
                        PRIMARY KEY (player_uuid, kit_id)
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS alka_kits_vouchers (
                        voucher_id VARCHAR(36) PRIMARY KEY,
                        kit_id VARCHAR(64) NOT NULL,
                        level INT NOT NULL,
                        redeemed INT DEFAULT 0,
                        redeemed_by VARCHAR(36),
                        redeemed_at BIGINT DEFAULT 0
                    )
                    """);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao criar tabelas do AlkaKits", e);
        }
    }

    // ------------------------------------------------------------------ progresso

    /** Le todo o progresso de um jogador numa unica query - usado no join pra popular o cache inteiro de uma vez. */
    public Map<String, KitProgress> loadAllProgress(UUID uuid) {
        Map<String, KitProgress> result = new HashMap<>();
        String sql = "SELECT kit_id, unlocked_level, last_claim_epoch, uses_at_current_level, last_buy_epoch FROM alka_kits_progress WHERE player_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("kit_id"), new KitProgress(
                            rs.getInt("unlocked_level"),
                            rs.getLong("last_claim_epoch"),
                            rs.getInt("uses_at_current_level"),
                            rs.getLong("last_buy_epoch")));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao carregar progresso de kits de " + uuid, e);
        }
        return result;
    }

    public KitProgress loadProgress(UUID uuid, String kitId) {
        String sql = "SELECT unlocked_level, last_claim_epoch, uses_at_current_level, last_buy_epoch FROM alka_kits_progress WHERE player_uuid = ? AND kit_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, kitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return KitProgress.EMPTY;
                }
                return new KitProgress(rs.getInt("unlocked_level"), rs.getLong("last_claim_epoch"),
                        rs.getInt("uses_at_current_level"), rs.getLong("last_buy_epoch"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao carregar progresso do kit " + kitId + " de " + uuid, e);
            return KitProgress.EMPTY;
        }
    }

    public void saveProgress(UUID uuid, String kitId, KitProgress progress) {
        String sql = upsert("alka_kits_progress",
                new String[]{"player_uuid", "kit_id", "unlocked_level", "last_claim_epoch", "uses_at_current_level", "last_buy_epoch"},
                new String[]{"player_uuid", "kit_id"});
        try {
            execute(sql, ps -> {
                ps.setString(1, uuid.toString());
                ps.setString(2, kitId);
                ps.setInt(3, progress.unlockedLevel());
                ps.setLong(4, progress.lastClaimEpochSeconds());
                ps.setInt(5, progress.usesAtCurrentLevel());
                ps.setLong(6, progress.lastBuyEpochSeconds());
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao salvar progresso do kit " + kitId + " de " + uuid, e);
        }
    }

    // ------------------------------------------------------------------- vouchers

    public void createVoucher(UUID voucherId, String kitId, int level) {
        String sql = "INSERT INTO alka_kits_vouchers (voucher_id, kit_id, level) VALUES (?, ?, ?)";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, voucherId.toString());
            ps.setString(2, kitId);
            ps.setInt(3, level);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao criar voucher " + voucherId, e);
        }
    }

    public Voucher getVoucher(UUID voucherId) {
        String sql = "SELECT kit_id, level, redeemed, redeemed_by, redeemed_at FROM alka_kits_vouchers WHERE voucher_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, voucherId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String redeemedByRaw = rs.getString("redeemed_by");
                return new Voucher(voucherId, rs.getString("kit_id"), rs.getInt("level"),
                        rs.getInt("redeemed") != 0, redeemedByRaw != null ? UUID.fromString(redeemedByRaw) : null,
                        rs.getLong("redeemed_at"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao ler voucher " + voucherId, e);
            return null;
        }
    }

    /** Marca como resgatado so se AINDA nao estava (evita corrida entre dois cliques quase simultaneos no mesmo voucher) - retorna false se ja tinha sido resgatado. */
    public boolean tryRedeemVoucher(UUID voucherId, UUID redeemer) {
        String sql = "UPDATE alka_kits_vouchers SET redeemed = 1, redeemed_by = ?, redeemed_at = ? WHERE voucher_id = ? AND redeemed = 0";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, redeemer.toString());
            ps.setLong(2, System.currentTimeMillis() / 1000L);
            ps.setString(3, voucherId.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao resgatar voucher " + voucherId, e);
            return false;
        }
    }
}
