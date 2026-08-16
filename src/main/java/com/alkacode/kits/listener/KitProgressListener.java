package com.alkacode.kits.listener;

import com.alkacode.kits.manager.KitProgressManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class KitProgressListener implements Listener {

    private final KitProgressManager progressManager;

    public KitProgressListener(KitProgressManager progressManager) {
        this.progressManager = progressManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        progressManager.onJoin(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        progressManager.onQuit(event.getPlayer().getUniqueId());
    }
}
