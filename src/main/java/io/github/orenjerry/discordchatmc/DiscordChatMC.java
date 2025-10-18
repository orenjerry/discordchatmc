package io.github.orenjerry.discordchatmc;

import org.bukkit.plugin.java.JavaPlugin;

public final class DiscordChatMC extends JavaPlugin {

    private DiscordBotService botService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // 1. Initialize our bot service. This will log in.
        this.botService = new DiscordBotService(this);

        // 2. Register our chat listener (using the fixed version)
        getServer().getPluginManager().registerEvents(new ChatListener(this.botService), this);

        // 3. Register our master command
        DcmCommand dcmCommand = new DcmCommand(this, this.botService);
        this.getCommand("dcm").setExecutor(dcmCommand);
        this.getCommand("dcm").setTabCompleter(dcmCommand);

        getLogger().info("DiscordChatMC has been enabled!");

        // 4. Update the startup warning
        if (!this.botService.isBotReady()) {
            getLogger().warning("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            getLogger().warning("Discord Bot is not ready. Check config.yml");
            getLogger().warning("for a valid 'bot-token' and 'channels.primary'");
            getLogger().warning("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
    }

    @Override
    public void onDisable() {
        // 5. Shut down the bot gracefully when the server stops
        if (botService != null) {
            botService.shutdown();
        }
        getLogger().info("DiscordChatMC has been disabled.");
    }
}