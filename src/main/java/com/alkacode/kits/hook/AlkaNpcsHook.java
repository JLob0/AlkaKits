package com.alkacode.kits.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Ponte opcional com o AlkaNpcs - registra o que acontece quando o jogador clica no
 * NPC "suprimentos" (abre o menu de kits, mesmo comportamento de /kits sem argumentos).
 * Reflection pura via ServicesManager - nunca importa com.alkacode.npcs.api.AlkaNpcsAPI
 * diretamente (softdepend, o AlkaNpcs pode nao estar instalado).
 */
public final class AlkaNpcsHook {

    private AlkaNpcsHook() {
    }

    public static boolean tryRegister(JavaPlugin plugin, String npcId, Consumer<Player> handler) {
        if (Bukkit.getPluginManager().getPlugin("AlkaNpcs") == null) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("com.alkacode.npcs.api.AlkaNpcsAPI");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null) {
                return false;
            }
            Method registerMethod = apiClass.getMethod("registerClickHandler", String.class, Consumer.class);
            registerMethod.invoke(registration.getProvider(), npcId, handler);
            plugin.getLogger().info("NPC '" + npcId + "' conectado via AlkaNpcs.");
            return true;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "AlkaNpcs encontrado mas o hook do NPC '" + npcId + "' falhou.", t);
            return false;
        }
    }
}
