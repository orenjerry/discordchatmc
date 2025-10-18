package io.github.orenjerry.discordchatmc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

// This record holds the plugin and the service it needs to reload
public record ReloadCommand(JavaPlugin plugin, DiscordWebhookService webhookService) implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Always add a permission check for admin commands!
        if (!sender.hasPermission("discordchatmc.reload")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        // 1. Reload the config file from disk
        plugin.reloadConfig();

        // 2. Tell the webhook service to re-load the new values
        webhookService.loadConfig();

        sender.sendMessage("§aDiscordChatMC configuration reloaded!");
        return true;
    }
}