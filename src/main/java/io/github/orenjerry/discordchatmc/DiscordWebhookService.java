package io.github.orenjerry.discordchatmc;

import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class DiscordWebhookService {

    private final JavaPlugin plugin;
    private final HttpClient httpClient;
    private String webhookUrl;
    private String botName;

    // Constructor: We pass in our main plugin class
    public DiscordWebhookService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Load settings from config
        loadConfig();
    }

    public void loadConfig() {
        this.webhookUrl = plugin.getConfig().getString("webhook-url");
        this.botName = plugin.getConfig().getString("bot-name", "Minecraft Server");
    }

    // This method will be for sending chat messages
    public void sendChatMessage(String playerName, String playerUUID, String message) {
        if (isUrlInvalid()) return;

        // We use an "embed" for a nice-looking message
        JSONObject embed = new JSONObject();
        embed.put("description", message); // The chat message
        embed.put("author", new JSONObject()
                .put("name", playerName)
                .put("icon_url", "https://crafatar.com/avatars/" + playerUUID + "?overlay"));

        JSONObject payload = new JSONObject();
        payload.put("username", this.botName);
        payload.put("embeds", new JSONArray().put(embed));

        sendAsync(payload);
    }

    // This method will be for sending the /list command
    public void sendPlayerList(String title, String description) {
        if (isUrlInvalid()) return;

        JSONObject embed = new JSONObject();
        embed.put("title", title);
        embed.put("description", description);
        embed.put("color", 65280); // A nice green color

        JSONObject payload = new JSONObject();
        payload.put("username", this.botName);
        payload.put("embeds", new JSONArray().put(embed));

        sendAsync(payload);
    }

    public boolean isUrlInvalid() {
        return webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("YOUR_WEBHOOK_URL_HERE");
    }

    // Private helper method to send the request asynchronously
    private void sendAsync(JSONObject payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            // Send the request async! This does NOT lag the server.
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .exceptionally(e -> {
                        plugin.getLogger().warning("Failed to send webhook to Discord: " + e.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            plugin.getLogger().warning("Error creating Discord webhook request: " + e.getMessage());
        }
    }
}