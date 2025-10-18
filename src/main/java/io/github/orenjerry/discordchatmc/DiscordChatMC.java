package io.github.orenjerry.discordchatmc;

import net.dv8tion.jda.api.JDA;
import org.bukkit.plugin.java.JavaPlugin;

public final class DiscordChatMC extends JavaPlugin {

    private DiscordBotService botService;
    private JDA jda;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.botService = new DiscordBotService(this);
        getServer().getPluginManager().registerEvents(new ChatListener(this.botService), this);
        DcmCommand dcmCommand = new DcmCommand(this, this.botService);
        this.getCommand("dcm").setExecutor(dcmCommand);
        this.getCommand("dcm").setTabCompleter(dcmCommand);

        getLogger().info("DiscordChatMC has been enabled!");

        if (!this.botService.isBotReady()) {
            getLogger().warning("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            getLogger().warning("Discord Bot is not ready. Check config.yml");
            getLogger().warning("for a valid 'bot-token' and 'channels.primary'");
            getLogger().warning("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
    }

    @Override
    public void onDisable() {
        if (jda != null) {
            try {
                jda.shutdownNow();
                jda.awaitShutdown();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            getLogger().info("DiscordChatMC has been disabled!");
        }
    }
}