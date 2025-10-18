package io.github.orenjerry.discordchatmc;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.Color;

public class DiscordBotService {

    private final JavaPlugin plugin;
    private JDA jda;

    // Config values
    private String token;
    private TextChannel primaryChannel;
    private String mcToDiscordFormat;
    private String mcToDiscordNameFormat;
    private String avatarUrl;

    public DiscordBotService(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig(); // Load initial config
        login();      // Attempt to log in
    }

    public void loadConfig() {
        // Reload config from disk
        plugin.reloadConfig();

        // Load values from config.yml
        token = plugin.getConfig().getString("bot-token", "");
        mcToDiscordFormat = plugin.getConfig().getString("messages.mc-to-discord", "{message}");
        mcToDiscordNameFormat = plugin.getConfig().getString("messages.mc-to-discord-name-format", "{displayname}");
        avatarUrl = plugin.getConfig().getString("avatar-url", "https://crafatar.com/avatars/{uuid}?overlay");

        // If JDA is already running, update channels and presence
        if (jda != null) {
            updateChannels();
            updatePresence();
        }
    }

    private void login() {
        if (token.isEmpty()) {
            plugin.getLogger().warning("Bot token is empty in config.yml. Discord bot will not start.");
            return;
        }

        try {
            jda = JDABuilder.createDefault(token)
                    .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .build().awaitReady(); // Wait for the bot to log in

            updateChannels();
            updatePresence();

            plugin.getLogger().info("Successfully logged in to Discord!");

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to log in to Discord: " + e.getMessage());
            jda = null; // Ensure JDA is null if login failed
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
            plugin.getLogger().info("Discord bot has been shut down.");
        }
    }

    // This method will be for sending chat messages
    public void sendChatMessage(String playerName, String playerUUID, String message) {
        if (primaryChannel == null) return; // Don't send if channel is invalid

        // Use the formats from config.yml
        String discordName = mcToDiscordNameFormat.replace("{displayname}", playerName);
        String discordMessage = mcToDiscordFormat.replace("{message}", message);
        String playerAvatar = avatarUrl
                .replace("{uuid}", playerUUID)
                .replace("{name}", playerName)
                .replace("{textures}.png", playerUUID); // Fix for crafatar URL

        // Use an EmbedBuilder for a nice message
        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor(discordName, null, playerAvatar);
        embed.setDescription(discordMessage);

        primaryChannel.sendMessageEmbeds(embed.build()).queue();
    }

    // This method will be for sending the /list command
    public void sendPlayerList(String title, String description) {
        if (primaryChannel == null) return; // Don't send if channel is invalid

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(title);
        embed.setDescription(description);
        embed.setColor(new Color(65280)); // Green

        primaryChannel.sendMessageEmbeds(embed.build()).queue();
    }

    // Helper method to update the primary channel
    private void updateChannels() {
        long primaryId = plugin.getConfig().getLong("channels.primary");
        if (primaryId == 0) {
            plugin.getLogger().warning("'channels.primary' is not set in config.yml. Bot will not send messages.");
            primaryChannel = null;
            return;
        }

        TextChannel channel = jda.getTextChannelById(primaryId);
        if (channel == null) {
            plugin.getLogger().warning("Could not find channel with ID: " + primaryId + ". (Is the bot in that server?)");
            primaryChannel = null;
        } else {
            primaryChannel = channel;
            plugin.getLogger().info("Set primary channel to: " + channel.getName());
        }
    }

    // Helper method to set the bot's "Playing" status
    private void updatePresence() {
        String activityMessage = plugin.getConfig().getString("presence.message", "Minecraft");
        // You can make this more advanced later to support "watching", "listening", etc.
        jda.getPresence().setActivity(Activity.playing(activityMessage));
    }

    public boolean isBotReady() {
        return jda != null && primaryChannel != null;
    }
}