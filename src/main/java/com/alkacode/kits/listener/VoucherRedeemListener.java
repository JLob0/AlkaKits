package com.alkacode.kits.listener;

import com.alkacode.kits.service.VoucherManager;
import com.alkacode.kits.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class VoucherRedeemListener implements Listener {

    private final VoucherManager voucherManager;
    private final Messages messages;

    public VoucherRedeemListener(VoucherManager voucherManager, Messages messages) {
        this.voucherManager = voucherManager;
        this.messages = messages;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (voucherManager.readVoucherId(item) == null) {
            return;
        }

        event.setCancelled(true);
        redeem(player, item);
    }

    public void redeem(Player player, ItemStack item) {
        switch (voucherManager.redeem(player, item)) {
            case SUCCESS -> player.sendMessage(messages.get("voucher.resgatado"));
            case ALREADY_REDEEMED -> player.sendMessage(messages.get("voucher.ja-usado"));
            case KIT_NOT_FOUND -> player.sendMessage(messages.get("voucher.kit-inexistente"));
            case NOT_A_VOUCHER -> player.sendMessage(messages.get("voucher.invalido"));
        }
    }
}
