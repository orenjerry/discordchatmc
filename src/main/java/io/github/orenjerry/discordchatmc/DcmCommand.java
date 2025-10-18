package io.github.orenjerry.discordchatmc;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter; // <-- IMPORT THIS
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil; // <-- IMPORT THIS
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable; // <-- IMPORT THIS

import java.util.ArrayList; // <-- IMPORT THIS
import java.util.Collection;
import java.util.List; // <-- IMPORT THIS
import java.util.stream.Collectors;

// Add TabCompleter to the implements list
public record DcmCommand(JavaPlugin plugin, DiscordWebhookService webhookService) implements CommandExecutor, TabCompleter {

    // --- onCommand method is unchanged ---
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
            case "list":
                executeList(sender);
                break;
            default:
                sender.sendMessage("§cUnknown subcommand. Use /dcm for help.");
                break;
        }
        return true;
    }

    // --- THIS IS THE NEW METHOD TO ADD ---
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // We only want to provide completions for the first argument
        if (args.length == 1) {
            // This will hold our suggestions
            List<String> completions = new ArrayList<>();

            // Check permissions before offering suggestions
            if (sender.hasPermission("discordchatmc.list")) {
                completions.add("list");
            }
            if (sender.hasPermission("discordchatmc.reload")) {
                completions.add("reload");
            }

            // This helper method filters the list based on what the user is typing
            return StringUtil.copyPartialMatches(args[0], completions, new ArrayList<>());
        }

        // Return an empty list for any other arguments (e.g., /dcm list <arg2>)
        return List.of();
    }
    // --- END OF NEW METHOD ---


    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6--- DiscordChatMC Help ---");
        if (sender.hasPermission("discordchatmc.list")) {
            sender.sendMessage("§e/dcm list §7- Sends the player list to Discord.");
        }
        if (sender.hasPermission("discordchatmc.reload")) {
            sender.sendMessage("§e/dcm reload §7- Reloads the plugin's config.");
        }
    }

    private void executeReload(CommandSender sender) {
        if (!sender.hasPermission("discordchatmc.reload")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return;
        }
        plugin.reloadConfig();
        webhookService.loadConfig();
        sender.sendMessage("§aDiscordChatMC configuration reloaded!");
    }

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