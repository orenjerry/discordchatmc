package io.github.orenjerry.discordchatmc;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import me.clip.placeholderapi.PlaceholderAPI;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;

public class DiscordBotService {

    private final JavaPlugin plugin;
    private JDA jda;
    private long startTime;

    // Config values
    private String token;
    private TextChannel primaryChannel;
    private final java.util.Map<Long, String> webhookUrls = new java.util.concurrent.ConcurrentHashMap<>();
    private String mcToDiscordFormat;
    private String mcToDiscordNameFormat;
    private String avatarUrl;

    public DiscordBotService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.startTime = System.currentTimeMillis();
        loadConfig();
        login();
    }

    private TextChannel resolveChannel(String messageTypeKey) {
        String target = plugin.getConfig().getString("message-types." + messageTypeKey, "primary");
        if ("none".equalsIgnoreCase(target)) return null;

        org.bukkit.configuration.ConfigurationSection cs = plugin.getConfig().getConfigurationSection("channels");
        if (cs != null && cs.isLong(target)) {
            long id = cs.getLong(target);
            return jda.getTextChannelById(id);
        }

        try {
            long id = Long.parseLong(target);
            return jda.getTextChannelById(id);
        } catch (NumberFormatException ignored) {
            long primaryId = plugin.getConfig().getLong("channels.primary", 0L);
            return primaryId != 0 ? jda.getTextChannelById(primaryId) : null;
        }
    }

    private String getOrCreateWebhookUrl(TextChannel channel) {
        return webhookUrls.computeIfAbsent(channel.getIdLong(), id -> {
            java.util.List<net.dv8tion.jda.api.entities.Webhook> existing = channel.retrieveWebhooks().complete();
            net.dv8tion.jda.api.entities.Webhook hook = existing.isEmpty()
                    ? channel.createWebhook("DCMC").complete()
                    : existing.get(0);
            return hook.getUrl();
        });
    }

    public void loadConfig() {
        plugin.reloadConfig();

        token = plugin.getConfig().getString("bot-token", "");
        mcToDiscordFormat = plugin.getConfig().getString("messages.mc-to-discord", "{message}");
        mcToDiscordNameFormat = plugin.getConfig().getString("messages.mc-to-discord-name-format", "{displayname}");
        avatarUrl = plugin.getConfig().getString("avatar-url", "https://crafatar.com/avatars/{uuid}?overlay");

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
                    .build().awaitReady();

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

        String url = getOrCreateWebhookUrl(target);
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
        String template = plugin.getConfig()
                .getString("avatar-url", "https://crafatar.com/avatars/{uuid}");
        String url = template
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{name}", player.getName());
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") && url.contains("%")) {
            url = PlaceholderAPI.setPlaceholders(player, url);
        }
        return url;
    }

    private String applyPlaceholders(Player player, String message) {
        String result = message
                .replace("{username}", player.getName())
                .replace("{displayname}", player.getDisplayName())
                .replace("{world}", player.getWorld().getName())
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{unique}", String.valueOf(Bukkit.getOfflinePlayers().length));

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            result = PlaceholderAPI.setPlaceholders(player, result);
        }
        return result;
    }

    public void sendChatMessage(Player player, String message) {
        String username = mcToDiscordNameFormat.replace("{displayname}", player.getDisplayName())
                .replace("{username}", player.getName());
        String content = mcToDiscordFormat.replace("{message}", message)
                .replace("{displayname}", player.getDisplayName())
                .replace("{username}", player.getName());
        content = applyPlaceholders(player, content);
        String avatar = resolveAvatarUrl(player);
        sendViaWebhook("chat", username, avatar, content);
    }

    public void sendJoin(Player player, boolean firstJoin) {
        String key = firstJoin ? "first-join" : "join";
        String formatKey = "messages." + key;
        String username = player.getDisplayName();
        String content = plugin.getConfig().getString(formatKey,
                firstJoin ? ":arrow_right: :first_place: {displayname} has joined the server for the first time!"
                        : ":arrow_right: {displayname} has joined!");
        content = applyPlaceholders(player, content);
        String avatar = resolveAvatarUrl(player);
        sendViaWebhook(key, username, avatar, content);
    }

    public void sendLeave(Player player) {
        String username = player.getDisplayName();
        String content = plugin.getConfig().getString("messages.quit", ":arrow_left: {displayname} has left!");
        content = applyPlaceholders(player, content);
        String avatar = resolveAvatarUrl(player);
        sendViaWebhook("leave", username, avatar, content);
    }

    public void sendDeath(Player player, String deathMessage) {
        String username = player.getDisplayName();
        String content = plugin.getConfig().getString("messages.death", ":skull: {deathmessage}")
                .replace("{deathmessage}", deathMessage);
        content = applyPlaceholders(player, content);
        String avatar = resolveAvatarUrl(player);
        sendViaWebhook("death", username, avatar, content);
    }

    public void sendAdvancement(Player player, String advancementName) {
        String username = player.getDisplayName();
        String content = plugin.getConfig().getString("messages.advancement",
                        ":medal: {displayname} has completed the advancement **{advancement}**!")
                .replace("{advancement}", advancementName);
        content = applyPlaceholders(player, content);
        String avatar = resolveAvatarUrl(player);
        sendViaWebhook("advancement", username, avatar, content);
    }

    public void sendAction(Player player, String actionText) {
        String username = player.getDisplayName();
        String content = plugin.getConfig().getString("messages.action",
                        ":person_biking: {displayname} *{action}*")
                .replace("{action}", actionText);
        content = applyPlaceholders(player, content);
        String avatar = resolveAvatarUrl(player);
        sendViaWebhook("action", username, avatar, content);
    }

    public void sendKick(Player player, String reason) {
        String username = player.getDisplayName();
        String content = plugin.getConfig().getString("messages.kick",
                        "{displayname} was kicked with reason: {reason}")
                .replace("{reason}", reason == null ? "No reason" : reason);
        content = applyPlaceholders(player, content);
        String avatar = resolveAvatarUrl(player);
        sendViaWebhook("kick", username, avatar, content);
    }

    public void sendServerStart() {
        long seconds = (System.currentTimeMillis() - startTime) / 1000;
        String content = plugin.getConfig().getString("messages.server-start",
                        ":white_check_mark: The server has started in {starttimeseconds} seconds!")
                .replace("{starttimeseconds}", String.valueOf(seconds));
        sendViaWebhook("server-start", "Server", null, content);
    }

    public void sendServerStop() {
        String content = plugin.getConfig().getString("messages.server-stop",
                ":octagonal_sign: The server has stopped!");
        sendViaWebhook("server-stop", "Server", null, content);
    }

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

    private void updatePresence() {
        String activityMessage = plugin.getConfig().getString("presence.message", "Minecraft");
        jda.getPresence().setActivity(Activity.playing(activityMessage));
    }

    public boolean isBotReady() {
        return jda != null && primaryChannel != null;
    }
}
