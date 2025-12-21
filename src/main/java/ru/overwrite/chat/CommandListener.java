package ru.overwrite.chat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import ru.overwrite.chat.configuration.Config;
import ru.overwrite.chat.configuration.data.NewbieChatSettings;
import ru.overwrite.chat.utils.Utils;

public class CommandListener implements Listener {

    private final Config pluginConfig;

    public CommandListener(PromisedChat plugin) {
        this.pluginConfig = plugin.getPluginConfig();
    }

    @EventHandler(ignoreCancelled = true)
    public void playerCommand(PlayerCommandPreprocessEvent e) {
        NewbieChatSettings newbieCommands = pluginConfig.getNewbieChatSettings();
        if (!newbieCommands.enabled()) {
            return;
        }
        Player p = e.getPlayer();
        String command = cutCommand(e.getMessage());
        long time = (System.currentTimeMillis() - p.getFirstPlayed()) / 1000;
        if (time <= newbieCommands.cooldown() && !p.hasPermission("pchat.bypass.newbie")) {
            for (String cmd : newbieCommands.commands()) {
                if (!command.equalsIgnoreCase(cmd)) {
                    continue;
                }
                String cooldown = Utils.getTime((int) (newbieCommands.cooldown() - time), " ч. ", " мин. ", " сек. ");
                p.sendMessage(newbieCommands.message().replace("%time%", cooldown));
                e.setCancelled(true);
                return;
            }
        }
    }

    private String cutCommand(String str) {
        int index = str.indexOf(' ');
        return index == -1 ? str : str.substring(0, index);
    }
}
