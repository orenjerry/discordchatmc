package io.github.orenjerry.discordchatmc;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import me.clip.placeholderapi.PlaceholderAPI;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;

import java.awt.Color;

public class DiscordBotService {

    private final JavaPlugin plugin;
    private JDA jda;

    // Config values
    private String token;
    private TextChannel primaryChannel;
    private final java.util.Map<Long, String> webhookUrls = new java.util.concurrent.ConcurrentHashMap<>();
    private String mcToDiscordFormat;
    private String mcToDiscordNameFormat;
    private String avatarUrl;

    public DiscordBotService(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig(); // Load initial config
        login();      // Attempt to log in
    }

    private TextChannel resolveChannel(String messageTypeKey) {
        String target = plugin.getConfig().getString("message-types." + messageTypeKey, "primary");
        if ("none".equalsIgnoreCase(target)) return null; // disabled

        // Try named channel from channels section
        org.bukkit.configuration.ConfigurationSection cs = plugin.getConfig().getConfigurationSection("channels");
        if (cs != null && cs.isLong(target)) {
            long id = cs.getLong(target);
            return jda.getTextChannelById(id);
        }

        // Try parsing as raw ID
        try {
            long id = Long.parseLong(target);
            return jda.getTextChannelById(id);
        } catch (NumberFormatException ignored) {
            // Fallback to primary
            long primaryId = plugin.getConfig().getLong("channels.primary", 0L);
            return primaryId != 0 ? jda.getTextChannelById(primaryId) : null;
        }
    }

    private String getOrCreateWebhookUrl(TextChannel channel) {
        return webhookUrls.computeIfAbsent(channel.getIdLong(), id -> {
            java.util.List<net.dv8tion.jda.api.entities.Webhook> existing = channel.retrieveWebhooks().complete();
            net.dv8tion.jda.api.entities.Webhook hook = existing.isEmpty()
                    ? channel.createWebhook("DiscordChatMC").complete()
                    : existing.get(0);
            return hook.getUrl(); // JDA exposes full execution URL
        });
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
            jda = null;
        }
    }

    private void sendViaWebhook(String messageTypeKey, String username, String avatarUrl, String content) {
        TextChannel target = resolveChannel(messageTypeKey);
        if (target == null || content == null || content.isEmpty()) return;

        String url = getOrCreateWebhookUrl(target); // caches per-channel webhook URL
        try (WebhookClient client = WebhookClient.withUrl(url)) {
            WebhookMessageBuilder builder = new WebhookMessageBuilder()
                    .setUsername(username)
                    .setContent(content);
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                builder.setAvatarUrl(avatarUrl);
            }
            client.send(builder.build());
        }
    }

    private String resolveAvatarUrl(Player player) {
        String template = plugin.getConfig().getString("avatar-url", "https://mc-heads.net/avatar/{textures}.png");
        String url = template;
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            if (template.contains("%")) {
                url = PlaceholderAPI.setPlaceholders(player, template);
            } else {
                String tex = PlaceholderAPI.setPlaceholders(player, "%skinsrestorer_texture_id_or_steve%");
                url = template.replace("{textures}", tex);
            }
        }
        return url;
    }


    public void sendChatMessage(Player player, String message) {
        String username = mcToDiscordNameFormat.replace("{displayname}", player.getName());
        String content  = mcToDiscordFormat.replace("{message}", message);
        String avatar   = resolveAvatarUrl(player);
        sendViaWebhook("chat", username, avatar, content);
    }

    public void sendJoin(Player player, boolean firstJoin) {
        String key = firstJoin ? "first-join" : "join";
        String username = mcToDiscordNameFormat.replace("{displayname}", player.getName());
        String formatKey = firstJoin ? "messages.first-join" : "messages.join";
        String content = plugin.getConfig().getString(formatKey, username + " joined");
        String avatar  = resolveAvatarUrl(player);
        sendViaWebhook(key, username, avatar, content);
    }

    public void sendLeave(Player player) {
        String username = mcToDiscordNameFormat.replace("{displayname}", player.getName());
        String content  = plugin.getConfig().getString("messages.leave", username + " left");
        String avatar   = resolveAvatarUrl(player);
        sendViaWebhook("leave", username, avatar, content);
    }

    public void sendDeath(Player player, String deathMessage) {
        String username = mcToDiscordNameFormat.replace("{displayname}", player.getName());
        String content  = plugin.getConfig().getString("messages.death", "{message}")
                .replace("{message}", deathMessage);
        String avatar   = resolveAvatarUrl(player);
        sendViaWebhook("death", username, avatar, content);
    }

    public void sendAdvancement(Player player, String advancementText) {
        String username = mcToDiscordNameFormat.replace("{displayname}", player.getName());
        String content  = plugin.getConfig().getString("messages.advancement", "{message}")
                .replace("{message}", advancementText);
        String avatar   = resolveAvatarUrl(player);
        sendViaWebhook("advancement", username, avatar, content);
    }

    public void sendAction(Player player, String actionText) {
        String username = mcToDiscordNameFormat.replace("{displayname}", player.getName());
        String content  = plugin.getConfig().getString("messages.action", "*{message}*")
                .replace("{message}", actionText);
        String avatar   = resolveAvatarUrl(player);
        sendViaWebhook("action", username, avatar, content);
    }

    public void sendKick(Player player, String reason) {
        String username = mcToDiscordNameFormat.replace("{displayname}", player.getName());
        String content  = plugin.getConfig().getString("messages.kick", "{name} was kicked: {reason}")
                .replace("{name}", username)
                .replace("{reason}", reason == null ? "" : reason);
        String avatar   = resolveAvatarUrl(player);
        sendViaWebhook("kick", username, avatar, content);
    }

    public void sendMute(Player player, boolean muted) {
        String username = mcToDiscordNameFormat.replace("{displayname}", player.getName());
        String baseKey  = muted ? "messages.mute-on" : "messages.mute-off";
        String content  = plugin.getConfig().getString(baseKey, muted ? "{name} was muted" : "{name} was unmuted")
                .replace("{name}", username);
        String avatar   = resolveAvatarUrl(player);
        sendViaWebhook("mute", username, avatar, content);
    }

    public void sendServerStart() {
        String content = plugin.getConfig().getString("messages.server-start", "Server started");
        sendViaWebhook("server-start", "Server", null, content);
    }

    public void sendServerStop() {
        String content = plugin.getConfig().getString("messages.server-stop", "Server stopped");
        sendViaWebhook("server-stop", "Server", null, content);
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