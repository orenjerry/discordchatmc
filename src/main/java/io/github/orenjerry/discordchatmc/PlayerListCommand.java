package io.github.orenjerry.discordchatmc;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

// This class can also be a record!
public record PlayerListCommand(JavaPlugin plugin, DiscordWebhookService webhookService) implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // We MUST run this async, because it makes a web request
        // The 'plugin' and 'webhookService' fields are automatically available
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            int playerCount = players.size();
            int maxPlayers = Bukkit.getMaxPlayers();

            String title = "Player List (" + playerCount + "/" + maxPlayers + ")";
            String description;

            if (playerCount == 0) {
                description = "There is nobody online.";
            } else {
                description = players.stream()
                        .map(Player::getName)
                        .collect(Collectors.joining("\n"));
            }

            webhookService.sendPlayerList(title, description);

            sender.sendMessage("§aSent player list to Discord!");
        });

        return true;
    }
}