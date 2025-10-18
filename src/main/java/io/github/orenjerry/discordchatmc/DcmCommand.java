package io.github.orenjerry.discordchatmc;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

// Change the service to DiscordBotService
public record DcmCommand(JavaPlugin plugin, DiscordBotService botService) implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                executeReload(sender);
                break;
            default:
                sender.sendMessage("§cUnknown subcommand. Use /dcm for help.");
                break;
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("discordchatmc.reload")) {
                completions.add("reload");
            }
            return StringUtil.copyPartialMatches(args[0], completions, new ArrayList<>());
        }
        return List.of();
    }


    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6--- DiscordChatMC Help ---");
        if (sender.hasPermission("discordchatmc.reload")) {
            sender.sendMessage("§e/dcm reload §7- Reloads the plugin's config.");
        }
    }

    // This logic is now simpler
    private void executeReload(CommandSender sender) {
        if (!sender.hasPermission("discordchatmc.reload")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return;
        }

        // The service's loadConfig() method already reloads the plugin config
        botService.loadConfig();

        sender.sendMessage("§aDiscordChatMC configuration reloaded!");
        if (!botService.isBotReady()) {
            sender.sendMessage("§eWarning: Bot is not connected. Check console for errors.");
        }
    }
}