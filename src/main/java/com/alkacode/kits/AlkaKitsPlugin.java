package com.alkacode.kits;

import com.alkacode.core.api.AlkaAPI;
import com.alkacode.core.plugin.AlkaPlugin;
import com.alkacode.economy.AlkaEconomyPlugin;
import com.alkacode.kits.command.CommandAlkaKits;
import com.alkacode.kits.command.CommandKits;
import com.alkacode.kits.config.MenuConfig;
import com.alkacode.kits.database.KitsRepository;
import com.alkacode.kits.gui.layout.GuiLayoutLoader;
import com.alkacode.kits.listener.GrantTriggerListener;
import com.alkacode.kits.listener.KitProgressListener;
import com.alkacode.kits.listener.VoucherRedeemListener;
import com.alkacode.kits.manager.GrantTriggerManager;
import com.alkacode.kits.manager.KitManager;
import com.alkacode.kits.manager.KitProgressManager;
import com.alkacode.kits.placeholder.AlkaKitsExpansion;
import com.alkacode.kits.service.FeedbackService;
import com.alkacode.kits.service.KitClaimService;
import com.alkacode.kits.service.KitsEconomyService;
import com.alkacode.kits.service.RequirementService;
import com.alkacode.kits.service.VoucherManager;
import com.alkacode.kits.util.Messages;
import org.bukkit.entity.Player;

/**
 * Kits com niveis/upgrade pago, requisitos, feedback rico e vouchers (ver kits.yml).
 * Sobre o AlkaCore (banco/HikariCP, GUI) e a AlkaEconomy (custo de upgrade de
 * nivel). Reconstruido a partir do KTempo... digo, do KITS_MESTRE (Drakkar, antigo
 * plugin do proprio estudio) cruzado com uma analise do AdvancedKits (referencia de
 * terceiros) - ver memoria project-alkakits pra decisoes de escopo (2026-08-13).
 */
public final class AlkaKitsPlugin extends AlkaPlugin {

    private MenuConfig menuConfig;
    private GuiLayoutLoader guiLayoutLoader;

    public MenuConfig getMenuConfig() {
        return menuConfig;
    }

    public GuiLayoutLoader getGuiLayoutLoader() {
        return guiLayoutLoader;
    }

    @Override
    protected void onPluginEnable() {
        AlkaAPI api = getAlkaAPI();
        Messages messages = new Messages(this);
        menuConfig = new MenuConfig(this);
        guiLayoutLoader = new GuiLayoutLoader(this);

        if (!(getServer().getPluginManager().getPlugin("AlkaEconomy") instanceof AlkaEconomyPlugin alkaEconomy)) {
            getLogger().severe("AlkaEconomy e obrigatorio e nao foi encontrado. Desativando.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        KitsRepository repository = new KitsRepository(api.getDatabase(), getLogger());
        KitProgressManager progressManager = new KitProgressManager(repository, api.getScheduler());
        KitManager kitManager = new KitManager(this);

        RequirementService requirementService = new RequirementService();
        KitsEconomyService economyService = new KitsEconomyService(alkaEconomy.getEconomyManager());
        FeedbackService feedbackService = new FeedbackService(this, messages);
        KitClaimService claimService = new KitClaimService(progressManager, requirementService, economyService, feedbackService);

        VoucherManager voucherManager = new VoucherManager(this, repository, kitManager, claimService, messages);
        GrantTriggerManager grantTriggerManager = new GrantTriggerManager(this, kitManager, claimService);

        getServer().getPluginManager().registerEvents(new KitProgressListener(progressManager), this);
        getServer().getPluginManager().registerEvents(new GrantTriggerListener(grantTriggerManager), this);
        getServer().getPluginManager().registerEvents(new VoucherRedeemListener(voucherManager, messages), this);

        CommandKits commandKits = new CommandKits(this, kitManager, progressManager, claimService, economyService, voucherManager, messages);
        getCommand("kits").setExecutor(commandKits);
        getCommand("kits").setTabCompleter(commandKits);

        CommandAlkaKits commandAlkaKits = new CommandAlkaKits(this, kitManager, progressManager, claimService, voucherManager, grantTriggerManager, messages);
        getCommand("alkakits").setExecutor(commandAlkaKits);
        getCommand("alkakits").setTabCompleter(commandAlkaKits);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new AlkaKitsExpansion(kitManager, progressManager).register();
        }

        // cobre /reload do servidor - so popula o cache de progresso, NUNCA repete
        // grant-triggers de primeiro-join pra quem ja estava online.
        for (Player player : getServer().getOnlinePlayers()) {
            progressManager.onJoin(player.getUniqueId());
        }

        getLogger().info("AlkaKits habilitado (" + kitManager.getKits().size() + " kits carregados).");
    }

    @Override
    protected void onPluginDisable() {
        // Progresso e write-through (cada claim/compra ja persiste na hora) - nao ha
        // estado de sessao acumulado pra descarregar aqui, diferente do AlkaTime.
        // Conexao/pool e do AlkaCore - fechado pelo proprio Core no seu onDisable.
    }
}
