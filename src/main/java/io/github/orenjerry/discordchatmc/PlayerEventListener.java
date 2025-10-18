package io.github.orenjerry.discordchatmc;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public record PlayerEventListener(DiscordBotService bot) implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        boolean firstJoin = !event.getPlayer().hasPlayedBefore();
        bot.sendJoin(event.getPlayer(), firstJoin);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        bot.sendLeave(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        String deathMsg = PlainTextComponentSerializer.plainText().serialize(event.deathMessage());
        bot.sendDeath(event.getPlayer(), deathMsg);
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        String reason = PlainTextComponentSerializer.plainText().serialize(event.reason());
        bot.sendKick(event.getPlayer(), reason);
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        var advancement = event.getAdvancement();
        var display = advancement.getDisplay();
        if (display == null) return;

        String advName = PlainTextComponentSerializer.plainText()
                .serialize(display.title());
        bot.sendAdvancement(event.getPlayer(), advName);
    }
}
