package com.neverland.aiplayer;

import com.neverland.aiplayer.bot.AIBot;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class AIPlayerPlugin extends JavaPlugin {
    private AIBot bot;
    @Override public void onEnable() {
        saveDefaultConfig();
        getCommand("aiplayer").setExecutor((sender, command, label, args) -> {
            if (!sender.hasPermission("aiplayer.admin")) return true;
            if (bot != null && bot.isRunning()) { bot.stop(); sender.sendMessage("§cAIPlayer stopped."); }
            else { bot = new AIBot(this, getConfig()); bot.start(); sender.sendMessage("§aAIPlayer started."); }
            return true;
        });
        Bukkit.getScheduler().runTaskTimer(this, () -> { if (bot != null) bot.tick(); }, 20L, 20L);
        getLogger().info("AIPlayer 0.6.0 enabled.");
    }
    @Override public void onDisable() { if (bot != null) bot.stop(); }
}