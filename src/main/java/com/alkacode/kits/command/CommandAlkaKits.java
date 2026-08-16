package com.alkacode.kits.command;

import com.alkacode.kits.manager.GrantTriggerManager;
import com.alkacode.kits.manager.KitManager;
import com.alkacode.kits.manager.KitProgressManager;
import com.alkacode.kits.model.Kit;
import com.alkacode.kits.model.KitProgress;
import com.alkacode.kits.service.KitClaimService;
import com.alkacode.kits.service.VoucherManager;
import com.alkacode.kits.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Comandos administrativos - todos exigem o alvo ONLINE (progresso de kit vive num
 * cache em memoria por jogador conectado, ver KitProgressManager; suportar alvo
 * offline exigiria contornar esse cache inteiramente - fora de escopo por ora, ver
 * README "Debitos conhecidos").
 */
public final class CommandAlkaKits implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final KitManager kitManager;
    private final KitProgressManager progressManager;
    private final KitClaimService claimService;
    private final VoucherManager voucherManager;
    private final GrantTriggerManager grantTriggerManager;
    private final Messages messages;

    public CommandAlkaKits(JavaPlugin plugin, KitManager kitManager, KitProgressManager progressManager,
                            KitClaimService claimService, VoucherManager voucherManager,
                            GrantTriggerManager grantTriggerManager, Messages messages) {
        this.plugin = plugin;
        this.kitManager = kitManager;
        this.progressManager = progressManager;
        this.claimService = claimService;
        this.voucherManager = voucherManager;
        this.grantTriggerManager = grantTriggerManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(messages.get("erro.uso-invalido", "<uso>", "/alkakits <reload|give|reset|setlevel|voucher>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "give" -> handleGive(sender, args);
            case "reset" -> handleReset(sender, args);
            case "setlevel" -> handleSetLevel(sender, args);
            case "voucher" -> handleVoucher(sender, args);
            default -> sender.sendMessage(messages.get("erro.uso-invalido", "<uso>", "/alkakits <reload|give|reset|setlevel|voucher>"));
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        messages.load();
        kitManager.load();
        grantTriggerManager.load();
        sender.sendMessage(messages.get("admin.reload-sucesso"));
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(messages.get("erro.uso-invalido", "<uso>", "/alkakits give <jogador> <kit> [nivel]"));
            return;
        }
        Player target = resolveOnlineTarget(sender, args[1]);
        Kit kit = resolveKit(sender, args[2]);
        if (target == null || kit == null) {
            return;
        }
        int level;
        if (args.length >= 4) {
            level = parseLevel(sender, args[3]);
            if (level < 0) {
                return;
            }
        } else {
            level = progressManager.getProgress(target.getUniqueId(), kit.getId()).unlockedLevel();
        }
        if (level < 1) {
            level = 1;
        }
        if (!kit.hasLevel(level)) {
            sender.sendMessage(messages.get("erro.nivel-invalido"));
            return;
        }
        claimService.grantDirect(target, kit, level);
        sender.sendMessage(messages.get("admin.give-sucesso", "<jogador>", target.getName(), "<kit>", kit.getId(), "<nivel>", String.valueOf(level)));
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(messages.get("erro.uso-invalido", "<uso>", "/alkakits reset <jogador> <kit>"));
            return;
        }
        Player target = resolveOnlineTarget(sender, args[1]);
        Kit kit = resolveKit(sender, args[2]);
        if (target == null || kit == null) {
            return;
        }
        progressManager.saveProgress(target.getUniqueId(), kit.getId(), KitProgress.EMPTY);
        sender.sendMessage(messages.get("admin.reset-sucesso", "<jogador>", target.getName(), "<kit>", kit.getId()));
    }

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(messages.get("erro.uso-invalido", "<uso>", "/alkakits setlevel <jogador> <kit> <nivel>"));
            return;
        }
        Player target = resolveOnlineTarget(sender, args[1]);
        Kit kit = resolveKit(sender, args[2]);
        if (target == null || kit == null) {
            return;
        }
        int level = parseLevel(sender, args[3]);
        if (level < 0 || (level > 0 && !kit.hasLevel(level))) {
            sender.sendMessage(messages.get("erro.nivel-invalido"));
            return;
        }
        progressManager.saveProgress(target.getUniqueId(), kit.getId(), new KitProgress(level, 0, 0, 0));
        sender.sendMessage(messages.get("admin.setlevel-sucesso", "<jogador>", target.getName(), "<kit>", kit.getId(), "<nivel>", String.valueOf(level)));
    }

    private void handleVoucher(CommandSender sender, String[] args) {
        if (args.length < 5 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage(messages.get("erro.uso-invalido", "<uso>", "/alkakits voucher give <jogador> <kit> <nivel>"));
            return;
        }
        Player target = resolveOnlineTarget(sender, args[2]);
        Kit kit = resolveKit(sender, args[3]);
        if (target == null || kit == null) {
            return;
        }
        int level = parseLevel(sender, args[4]);
        if (!kit.hasLevel(level)) {
            sender.sendMessage(messages.get("erro.nivel-invalido"));
            return;
        }
        var voucherItem = voucherManager.createVoucherItem(kit.getId(), level);
        var leftover = target.getInventory().addItem(voucherItem);
        leftover.values().forEach(item -> target.getWorld().dropItem(target.getLocation(), item));
        sender.sendMessage(messages.get("admin.voucher-sucesso", "<jogador>", target.getName(), "<kit>", kit.getId(), "<nivel>", String.valueOf(level)));
    }

    private Player resolveOnlineTarget(CommandSender sender, String name) {
        Player target = Bukkit.getPlayer(name);
        if (target == null) {
            sender.sendMessage(messages.get("erro.jogador-offline"));
        }
        return target;
    }

    private Kit resolveKit(CommandSender sender, String kitId) {
        Kit kit = kitManager.getKit(kitId);
        if (kit == null) {
            sender.sendMessage(messages.get("erro.kit-inexistente"));
        }
        return kit;
    }

    private int parseLevel(CommandSender sender, String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            sender.sendMessage(messages.get("erro.numero-invalido", "<valor>", raw));
            return -1;
        }
    }

    private static final List<String> SUBCOMMANDS = List.of("reload", "give", "reset", "setlevel", "voucher");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("voucher")) {
            if (args.length == 2) {
                return filter(List.of("give"), args[1]);
            }
            if (args.length == 3) {
                return filter(onlinePlayerNames(), args[2]);
            }
            if (args.length == 4) {
                return filter(List.copyOf(kitManager.getKits().keySet()), args[3]);
            }
            return List.of();
        }
        if ((sub.equals("give") || sub.equals("reset") || sub.equals("setlevel")) && args.length == 2) {
            return filter(onlinePlayerNames(), args[1]);
        }
        if ((sub.equals("give") || sub.equals("reset") || sub.equals("setlevel")) && args.length == 3) {
            return filter(List.copyOf(kitManager.getKits().keySet()), args[2]);
        }
        return List.of();
    }

    private List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(lower)).collect(Collectors.toList());
    }
}
