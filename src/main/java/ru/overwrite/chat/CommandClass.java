package ru.overwrite.chat;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class CommandClass implements CommandExecutor {

    private final PromisedChat plugin;

    public CommandClass(PromisedChat plugin) {
        this.plugin = plugin;
    }

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
            plugin.setupConfig();
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
}
