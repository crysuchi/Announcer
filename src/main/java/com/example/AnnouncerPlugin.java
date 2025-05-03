package com.example;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class AnnouncerPlugin extends JavaPlugin {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final List<BukkitTask> scheduledTasks = new ArrayList<>();
    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        getCommand("announcer").setExecutor(this);
        scheduleMessages();

        
        getServer().getConsoleSender().sendMessage(miniMessage.deserialize("<green>[Announcer] Enabled</green> <blue>Announcer Version: 1.2.2"));
    }

    @Override
    public void onDisable() {
        cancelAllTasks();
        
        getServer().getConsoleSender().sendMessage(miniMessage.deserialize("<green>[Announcer] Plugin disabled.</green>"));
    }

    private void cancelAllTasks() {
        for (BukkitTask task : scheduledTasks) {
            task.cancel();
        }
        scheduledTasks.clear();
    }

    private void scheduleMessages() {
        cancelAllTasks();
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");

        if (messagesSection == null) {
            getLogger().warning("No messages found in config.yml.");
            return;
        }

        for (String key : messagesSection.getKeys(false)) {
            ConfigurationSection msg = messagesSection.getConfigurationSection(key);
            if (msg == null) continue;

            String text = msg.getString("text", "");
            int interval = msg.getInt("interval", 60);

            BukkitTask task = Bukkit.getScheduler().runTaskTimer(this, () -> {
                String processed = applyPlaceholders(text);
                if (!processed.isEmpty()) {
                    Component message = miniMessage.deserialize(config.getString("settings.prefix", "") + processed);
                    Bukkit.getServer().sendMessage(message);
                }
            }, interval * 20L, interval * 20L);

            scheduledTasks.add(task);
        }
    }

    private String applyPlaceholders(String message) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String parsed = PlaceholderAPI.setPlaceholders(player, message);
                Component msg = miniMessage.deserialize(config.getString("settings.prefix", "") + parsed);
                player.sendMessage(msg);
            }
            return "";
        }
        return message;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("announcer")) return false;

        if (args.length == 0) {
            sendInfo(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                reloadConfig();
                config = getConfig();
                scheduleMessages();
                sender.sendMessage(miniMessage.deserialize("<green>[Announcer]</green> Configuration reloaded and timers restarted."));
                // Log to console
                getServer().getConsoleSender().sendMessage(miniMessage.deserialize("<green>[Announcer] Configuration reloaded successfully.</green>"));
                break;
            case "credits":
                sender.sendMessage(miniMessage.deserialize(
                    "<green>[Announcer]</green>\n" +
                    "Plugin developed by <yellow>crysuchi, 4uchi</yellow>\n" +
                    "Version: <blue>1.2.0</blue>"
                ));
                break;
            default:
                sender.sendMessage(miniMessage.deserialize("<red>Unknown subcommand. Use /announcer for help.</red>"));
                break;
        }

        return true;
    }

    private void sendInfo(CommandSender sender) {
        sender.sendMessage(miniMessage.deserialize(
            "<green>[Announcer]</green>\n" +
            "Plugin version: <blue>1.2.2</blue>\n" +
            "<yellow>Developed by </yellow>crysuchi, 4uchi.\n" +
            "<gray>Available commands:</gray>\n" +
            "<green>/announcer reload</green> - Reload the configuration\n" +
            "<green>/announcer credits</green> - Show plugin credits"
        ));
    }
} 
