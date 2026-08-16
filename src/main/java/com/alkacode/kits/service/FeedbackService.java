package com.alkacode.kits.service;

import com.alkacode.kits.model.Kit;
import com.alkacode.kits.model.KitLevel;
import com.alkacode.kits.model.KitStatus;
import com.alkacode.kits.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.logging.Level;

/**
 * Feedback por resultado de clique (som/particula/titulo/fireworks/comandos) - so
 * defaults GLOBAIS em config.yml, um por {@link KitStatus}, nunca por kit/nivel
 * individual. O AdvancedKits (referencia de terceiros) permite configurar isso por
 * nivel - avaliado como over-engineering pro nosso caso (analise 2026-08-13, ver
 * memoria project-alkakits): a mesma config de "sucesso"/"sem permissao"/etc serve
 * pra qualquer kit sem repetir 10 blocos identicos por nivel.
 */
public final class FeedbackService {

    private final JavaPlugin plugin;
    private final Messages messages;

    public FeedbackService(JavaPlugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void fire(Player player, KitStatus status, Kit kit, KitLevel level) {
        String kitName = kit.getId();
        String levelStr = String.valueOf(level != null ? level.level() : 0);

        player.sendMessage(messages.get("status." + status.name(), "<kit>", kitName, "<nivel>", levelStr));

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("feedback." + status.name());
        if (section == null) {
            return;
        }

        playSound(player, section.getConfigurationSection("som"));
        playParticle(player, section.getConfigurationSection("particula"));
        showTitle(player, section.getConfigurationSection("titulo"), kitName, levelStr);
        launchFirework(player, section.getConfigurationSection("fireworks"));
        runCommands(player, section.getStringList("comandos"));
    }

    private void launchFirework(Player player, ConfigurationSection section) {
        if (section == null || !section.getBoolean("ativo", false)) {
            return;
        }
        var firework = player.getWorld().spawn(player.getLocation(), org.bukkit.entity.Firework.class);
        var meta = firework.getFireworkMeta();

        org.bukkit.FireworkEffect.Type type;
        try {
            type = org.bukkit.FireworkEffect.Type.valueOf(section.getString("tipo", "BALL").toUpperCase());
        } catch (IllegalArgumentException e) {
            type = org.bukkit.FireworkEffect.Type.BALL;
        }

        var colors = section.getStringList("cores").stream().map(this::parseColor).filter(java.util.Objects::nonNull).toList();
        if (colors.isEmpty()) {
            colors = java.util.List.of(org.bukkit.Color.WHITE);
        }

        meta.addEffect(org.bukkit.FireworkEffect.builder()
                .with(type)
                .withColor(colors)
                .trail(section.getBoolean("trail", false))
                .flicker(section.getBoolean("flicker", false))
                .build());
        meta.setPower(Math.max(0, section.getInt("potencia", 1)));
        firework.setFireworkMeta(meta);

        // detona na hora em vez de subir voando - e feedback instantaneo de recompensa, nao um foguete de verdade.
        firework.detonate();
    }

    private org.bukkit.Color parseColor(String name) {
        return switch (name.toUpperCase()) {
            case "WHITE" -> org.bukkit.Color.WHITE;
            case "SILVER" -> org.bukkit.Color.SILVER;
            case "GRAY" -> org.bukkit.Color.GRAY;
            case "BLACK" -> org.bukkit.Color.BLACK;
            case "RED" -> org.bukkit.Color.RED;
            case "MAROON" -> org.bukkit.Color.MAROON;
            case "YELLOW" -> org.bukkit.Color.YELLOW;
            case "OLIVE" -> org.bukkit.Color.OLIVE;
            case "LIME" -> org.bukkit.Color.LIME;
            case "GREEN" -> org.bukkit.Color.GREEN;
            case "AQUA" -> org.bukkit.Color.AQUA;
            case "TEAL" -> org.bukkit.Color.TEAL;
            case "BLUE" -> org.bukkit.Color.BLUE;
            case "NAVY" -> org.bukkit.Color.NAVY;
            case "FUCHSIA" -> org.bukkit.Color.FUCHSIA;
            case "PURPLE" -> org.bukkit.Color.PURPLE;
            case "ORANGE" -> org.bukkit.Color.ORANGE;
            default -> null;
        };
    }

    private void playSound(Player player, ConfigurationSection section) {
        if (section == null) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(section.getString("nome", "").toUpperCase());
            player.playSound(player.getLocation(), sound,
                    (float) section.getDouble("volume", 1.0), (float) section.getDouble("pitch", 1.0));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.FINE, "Som de feedback invalido: " + section.getString("nome"), e);
        }
    }

    private void playParticle(Player player, ConfigurationSection section) {
        if (section == null) {
            return;
        }
        try {
            Particle particle = Particle.valueOf(section.getString("nome", "").toUpperCase());
            player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), section.getInt("quantidade", 10));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.FINE, "Particula de feedback invalida: " + section.getString("nome"), e);
        }
    }

    private void showTitle(Player player, ConfigurationSection section, String kitName, String levelStr) {
        if (section == null) {
            return;
        }
        String titleText = section.getString("texto", "");
        String subtitleText = section.getString("subtitulo", "");
        if (titleText.isBlank() && subtitleText.isBlank()) {
            return;
        }
        var mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
        Title.Times times = Title.Times.times(
                Duration.ofMillis(section.getInt("fade-in", 10) * 50L),
                Duration.ofMillis(section.getInt("stay", 40) * 50L),
                Duration.ofMillis(section.getInt("fade-out", 10) * 50L));
        Title title = Title.title(
                mm.deserialize(titleText.replace("<kit>", kitName).replace("<nivel>", levelStr)),
                mm.deserialize(subtitleText.replace("<kit>", kitName).replace("<nivel>", levelStr)),
                times);
        player.showTitle(title);
    }

    private void runCommands(Player player, java.util.List<String> commands) {
        for (String command : commands) {
            String parsed = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }
}
