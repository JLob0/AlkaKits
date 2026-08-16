package com.alkacode.kits.service;

import com.alkacode.kits.database.KitsRepository;
import com.alkacode.kits.manager.KitManager;
import com.alkacode.kits.model.Kit;
import com.alkacode.kits.model.Voucher;
import com.alkacode.kits.util.Messages;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Voucher fisico - item com um {@code voucherId} (UUID) gravado no PDC; a validade
 * real mora no banco ({@link KitsRepository#tryRedeemVoucher}, update atomico
 * condicional evita duplicar por dois cliques quase simultaneos no mesmo item, sem
 * precisar de NBT proprio como o AdvancedKits fazia).
 */
public final class VoucherManager {

    private final JavaPlugin plugin;
    private final KitsRepository repository;
    private final KitManager kitManager;
    private final KitClaimService claimService;
    private final Messages messages;
    private final NamespacedKey voucherKey;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public VoucherManager(JavaPlugin plugin, KitsRepository repository, KitManager kitManager,
                           KitClaimService claimService, Messages messages) {
        this.plugin = plugin;
        this.repository = repository;
        this.kitManager = kitManager;
        this.claimService = claimService;
        this.messages = messages;
        this.voucherKey = new NamespacedKey(plugin, "alkakits_voucher_id");
    }

    public ItemStack createVoucherItem(String kitId, int level) {
        UUID voucherId = UUID.randomUUID();
        repository.createVoucher(voucherId, kitId, level);

        String materialName = plugin.getConfig().getString("voucher.material", "PAPER");
        Material material = Material.matchMaterial(materialName);
        ItemStack item = new ItemStack(material != null ? material : Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(messages.raw("voucher.item-nome")
                        .replace("<kit>", kitId).replace("<nivel>", String.valueOf(level)))
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(messages.getList("voucher.item-lore", "<kit>", kitId, "<nivel>", String.valueOf(level)));
        meta.getPersistentDataContainer().set(voucherKey, PersistentDataType.STRING, voucherId.toString());
        item.setItemMeta(meta);
        return item;
    }

    public UUID readVoucherId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String raw = item.getItemMeta().getPersistentDataContainer().get(voucherKey, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public enum RedeemResult {
        SUCCESS, NOT_A_VOUCHER, ALREADY_REDEEMED, KIT_NOT_FOUND
    }

    public RedeemResult redeem(Player player, ItemStack item) {
        UUID voucherId = readVoucherId(item);
        if (voucherId == null) {
            return RedeemResult.NOT_A_VOUCHER;
        }
        Voucher voucher = repository.getVoucher(voucherId);
        if (voucher == null) {
            return RedeemResult.NOT_A_VOUCHER;
        }
        Kit kit = kitManager.getKit(voucher.kitId());
        if (kit == null || !kit.hasLevel(voucher.level())) {
            return RedeemResult.KIT_NOT_FOUND;
        }
        if (!repository.tryRedeemVoucher(voucherId, player.getUniqueId())) {
            return RedeemResult.ALREADY_REDEEMED;
        }

        item.setAmount(item.getAmount() - 1);
        claimService.grantDirect(player, kit, voucher.level());
        return RedeemResult.SUCCESS;
    }
}
