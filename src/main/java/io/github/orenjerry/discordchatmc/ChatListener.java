package io.github.orenjerry.discordchatmc;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

// Change 'class' to 'record' and move the fields to be parameters
public record ChatListener(DiscordWebhookService webhookService) implements Listener {

    // The @EventHandler method stays exactly the same
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = event.signedMessage().message(); // Get the message content

        // The 'webhookService' is automatically available as a field
        webhookService.sendChatMessage(
                player.getName(),
                player.getUniqueId().toString(),
                message
        );
    }
}