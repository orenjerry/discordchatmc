package io.github.orenjerry.discordchatmc;

import org.bukkit.plugin.java.JavaPlugin;

public final class DiscordChatMC extends JavaPlugin {

    private DiscordWebhookService webhookService;

    @Override
    public void onEnable() {
        // 1. Save the default config.yml (if it doesn't exist)
        saveDefaultConfig();

        // 2. Initialize our webhook service
        this.webhookService = new DiscordWebhookService(this);

        // 3. Register our chat listener
        getServer().getPluginManager().registerEvents(new ChatListener(this.webhookService), this);

        // 4. Register our /discordlist command
        this.getCommand("discordlist").setExecutor(new PlayerListCommand(this, this.webhookService));

        // 5. THIS IS THE NEW LINE YOU NEED TO ADD:
        this.getCommand("dcmreload").setExecutor(new ReloadCommand(this, this.webhookService));


        getLogger().info("DiscordChatMC has been enabled!");

        // Check if the webhook URL is set
        if (this.webhookService.isUrlInvalid()) {
            getLogger().warning("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            getLogger().warning("Discord webhook URL is not set in config.yml!");
            getLogger().warning("The plugin will not work until you set it.");
            getLogger().warning("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("DiscordChatMC has been disabled.");
    }
}