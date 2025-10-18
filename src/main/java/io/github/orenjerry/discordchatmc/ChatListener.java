package io.github.orenjerry.discordchatmc;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

// We are changing the service this uses to DiscordBotService
public record ChatListener(DiscordBotService botService) implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) { // Use AsyncPlayerChatEvent
        // Use the modern .player() method
        Player player = event.getPlayer();

        // Use the modern .message() method and serialize it to a plain string
        String message = event.signedMessage().message();

        // Send to our new bot service
        botService.sendChatMessage(
                player.getName(),
                player.getUniqueId().toString(),
                message
        );
    }
}