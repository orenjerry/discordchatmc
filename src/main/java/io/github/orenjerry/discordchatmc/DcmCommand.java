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

// This one record will hold our plugin and service
public record DcmCommand(JavaPlugin plugin, DiscordWebhookService webhookService) implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // Check if the player just typed "/dcm" with no arguments
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        // Check the first argument (e.g., "reload", "list")
        switch (args[0].toLowerCase()) {
            case "reload":
                executeReload(sender);
                break;
            case "list":
                executeList(sender);
                break;
            default:
                sender.sendMessage("§cUnknown subcommand. Use /dcm for help.");
                break;
        }
        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6--- DiscordChatMC Help ---");
        if (sender.hasPermission("discordchatmc.list")) {
            sender.sendMessage("§e/dcm list §7- Sends the player list to Discord.");
        }
        if (sender.hasPermission("discordchatmc.reload")) {
            sender.sendMessage("§e/dcm reload §7- Reloads the plugin's config.");
        }
    }

    // This is the logic from your old ReloadCommand
    private void executeReload(CommandSender sender) {
        if (!sender.hasPermission("discordchatmc.reload")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return;
        }

        plugin.reloadConfig();
        webhookService.loadConfig();
        sender.sendMessage("§aDiscordChatMC configuration reloaded!");
    }

    // This is the logic from your old PlayerListCommand
    private void executeList(CommandSender sender) {
        if (!sender.hasPermission("discordchatmc.list")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return;
        }

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
    }
}