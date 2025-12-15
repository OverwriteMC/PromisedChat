package ru.overwrite.chat;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PromisedChatCommand implements TabExecutor {

    private final PromisedChat plugin;

    public PromisedChatCommand(PromisedChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String commandlabel, @NotNull String[] args) {
        if (!sender.hasPermission("pchat.admin")) {
            sender.sendMessage("§6❖ §7Running §5§lPromisedChat §c§l" + plugin.getDescription().getVersion() + "§7 by §5OverwriteMC");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§6/" + commandlabel + " reload - перезагрузить конфиг");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            long startTime = System.currentTimeMillis();
            plugin.reloadConfig();
            plugin.getPluginConfig().setupConfigs(plugin.getConfig());
            Bukkit.getScheduler().cancelTasks(plugin);
            plugin.getAutoMessageManager().clearData();
            plugin.getAutoMessageManager().startMSG();
            long endTime = System.currentTimeMillis();
            sender.sendMessage("§5§lPromisedChat §7> §aКонфигурация перезагружена за §e" + (endTime - startTime) + " ms");
            return true;
        } else {
            sender.sendMessage("§6❖ §7Running §5§lPromisedChat §c§l" + plugin.getDescription().getVersion() + "§7 by §5OverwriteMC");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String commandlabel, @NotNull String[] args) {
        return List.of("reload");
    }
}
