package com.alkacode.kits.manager;

import com.alkacode.kits.model.Kit;
import com.alkacode.kits.service.KitClaimService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Concessoes automaticas config-driven - so os dois gatilhos que o AdvancedKits
 * (referencia de terceiros) realmente tem: primeiro join e respawn (ver analise
 * 2026-08-13, memoria project-alkakits - o nome "GrantTrigger" sugeria mais
 * variedade, mas na pratica e so isso mesmo).
 */
public final class GrantTriggerManager {

    private record Grant(String kitId, int level) {
    }

    private final JavaPlugin plugin;
    private final KitManager kitManager;
    private final KitClaimService claimService;

    private final List<Grant> firstJoinGrants = new ArrayList<>();
    private final List<Grant> respawnGrants = new ArrayList<>();

    public GrantTriggerManager(JavaPlugin plugin, KitManager kitManager, KitClaimService claimService) {
        this.plugin = plugin;
        this.kitManager = kitManager;
        this.claimService = claimService;
        load();
    }

    public void load() {
        firstJoinGrants.clear();
        respawnGrants.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("gatilhos");
        if (section == null) {
            return;
        }
        parseGrants(section, "primeiro-join", firstJoinGrants);
        parseGrants(section, "respawn", respawnGrants);
    }

    private void parseGrants(ConfigurationSection section, String key, List<Grant> target) {
        for (Map<?, ?> raw : section.getMapList(key)) {
            String kitId = String.valueOf(raw.get("kit"));
            Object levelRaw = raw.get("nivel");
            int level = levelRaw instanceof Number number ? number.intValue() : 1;
            target.add(new Grant(kitId, level));
        }
    }

    public void onFirstJoin(Player player) {
        grantAll(player, firstJoinGrants);
    }

    public void onRespawn(Player player) {
        grantAll(player, respawnGrants);
    }

    private void grantAll(Player player, List<Grant> grants) {
        for (Grant grant : grants) {
            Kit kit = kitManager.getKit(grant.kitId());
            if (kit == null) {
                plugin.getLogger().warning("Gatilho aponta pra kit inexistente '" + grant.kitId() + "' - ignorado.");
                continue;
            }
            claimService.grantDirect(player, kit, grant.level());
        }
    }
}
