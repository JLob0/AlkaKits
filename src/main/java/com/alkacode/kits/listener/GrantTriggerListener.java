package com.alkacode.kits.listener;

import com.alkacode.kits.manager.GrantTriggerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class GrantTriggerListener implements Listener {

    private final GrantTriggerManager grantTriggerManager;

    public GrantTriggerListener(GrantTriggerManager grantTriggerManager) {
        this.grantTriggerManager = grantTriggerManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!event.getPlayer().hasPlayedBefore()) {
            grantTriggerManager.onFirstJoin(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        grantTriggerManager.onRespawn(event.getPlayer());
    }
}
