package com.alkacode.kits.command;

import com.alkacode.kits.AlkaKitsPlugin;
import com.alkacode.kits.gui.CategoriesMenu;
import com.alkacode.kits.gui.KitsMenu;
import com.alkacode.kits.gui.PreviewMenu;
import com.alkacode.kits.manager.KitManager;
import com.alkacode.kits.manager.KitProgressManager;
import com.alkacode.kits.model.Kit;
import com.alkacode.kits.model.KitProgress;
import com.alkacode.kits.service.KitClaimService;
import com.alkacode.kits.service.KitsEconomyService;
import com.alkacode.kits.service.VoucherManager;
import com.alkacode.kits.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class CommandKits implements CommandExecutor, TabCompleter {

    private final AlkaKitsPlugin plugin;
    private final KitManager kitManager;
    private final KitProgressManager progressManager;
    private final KitClaimService claimService;
    private final KitsEconomyService economyService;
    private final VoucherManager voucherManager;
    private final Messages messages;

    public CommandKits(AlkaKitsPlugin plugin, KitManager kitManager, KitProgressManager progressManager,
                        KitClaimService claimService, KitsEconomyService economyService,
                        VoucherManager voucherManager, Messages messages) {
        this.plugin = plugin;
        this.kitManager = kitManager;
        this.progressManager = progressManager;
        this.claimService = claimService;
        this.economyService = economyService;
        this.voucherManager = voucherManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("erro.apenas-jogador"));
            return true;
        }
        if (!progressManager.isLoaded(player.getUniqueId())) {
            player.sendMessage(messages.get("erro.carregando"));
            return true;
        }

        if (args.length == 0) {
            openCategoriesMenu(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("resgatar")) {
            redeemHeldVoucher(player);
            return true;
        }

        String kitId = args[0];
        Kit kit = kitManager.getKit(kitId);
        if (kit == null) {
            player.sendMessage(messages.get("erro.kit-inexistente"));
            return true;
        }
        openPreviewMenu(player, kit);
        return true;
    }

    private void redeemHeldVoucher(Player player) {
        var item = player.getInventory().getItemInMainHand();
        switch (voucherManager.redeem(player, item)) {
            case SUCCESS -> player.sendMessage(messages.get("voucher.resgatado"));
            case ALREADY_REDEEMED -> player.sendMessage(messages.get("voucher.ja-usado"));
            case KIT_NOT_FOUND -> player.sendMessage(messages.get("voucher.kit-inexistente"));
            case NOT_A_VOUCHER -> player.sendMessage(messages.get("voucher.invalido"));
        }
    }

    public void openCategoriesMenu(Player player) {
        new CategoriesMenu(plugin, player, kitManager, this::openKitsMenu).open();
    }

    private void openKitsMenu(Player player, String category) {
        new KitsMenu(plugin, player, category, kitManager, progressManager, claimService,
                this::openCategoriesMenu, this::openPreviewMenu).open();
    }

    private void openPreviewMenu(Player player, Kit kit) {
        KitProgress progress = progressManager.getProgress(player.getUniqueId(), kit.getId());
        int initialLevel = Math.max(1, Math.min(kit.getMaxLevel(), progress.unlockedLevel() + 1));
        new PreviewMenu(plugin, player, kit, initialLevel, claimService, progressManager,
                economyService, p -> openKitsMenu(p, kit.getCategory())).open();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        List<String> options = new java.util.ArrayList<>(kitManager.getKits().keySet());
        options.add("resgatar");
        String lower = args[0].toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(lower)).collect(Collectors.toList());
    }
}
