package io.github.orenjerry.discordchatmc;

import org.bukkit.plugin.java.JavaPlugin;

public final class DiscordChatMC extends JavaPlugin {

    private DiscordWebhookService webhookService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.webhookService = new DiscordWebhookService(this);

        getServer().getPluginManager().registerEvents(new ChatListener(this.webhookService), this);
        DcmCommand dcmCommand = new DcmCommand(this, this.webhookService);

        this.getCommand("dcm").setExecutor(dcmCommand);
        this.getCommand("dcm").setTabCompleter(dcmCommand);
        getLogger().info("DiscordChatMC has been enabled!");

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