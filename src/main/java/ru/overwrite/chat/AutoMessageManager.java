package ru.overwrite.chat;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.overwrite.chat.configuration.Config;
import ru.overwrite.chat.utils.Utils;

import java.util.Collections;
import java.util.List;

public class AutoMessageManager {

    private final PromisedChat plugin;
    private final Config pluginConfig;

    private int randomIndex;
    private int sequentialIndex;

    private ObjectList<List<String>> shuffledMessages;

    public AutoMessageManager(PromisedChat plugin) {
        this.plugin = plugin;
        this.pluginConfig = plugin.getPluginConfig();
    }

    public void clearData() {
        randomIndex = 0;
        sequentialIndex = 0;
        shuffledMessages = null;
    }

    public void startMSG() {
        if (!pluginConfig.autoMessage) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                List<String> autoMessage = getAutoMessage();
                if (autoMessage == null || autoMessage.isEmpty()) {
                    return;
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.hasPermission("pchat.automessage")) {
                        continue;
                    }
                    for (String msg : autoMessage) {
                        p.sendMessage(Utils.colorize(msg));
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L, pluginConfig.autoMessageInterval * 20L);
    }

    private List<String> getAutoMessage() {
        ObjectList<List<String>> messages = pluginConfig.autoMessages;
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        if (pluginConfig.isRandom) {
            handleRandomRotation(messages);
            return shuffledMessages.get(randomIndex - 1);
        } else {
            handleSequentialRotation(messages);
            return messages.get(sequentialIndex - 1);
        }
    }

    private void handleRandomRotation(ObjectList<List<String>> messages) {
        if (shuffledMessages == null || randomIndex >= shuffledMessages.size()) {
            shuffledMessages = new ObjectArrayList<>(messages);
            Collections.shuffle(shuffledMessages);
            randomIndex = 0;
        }
        randomIndex++;
    }

    private void handleSequentialRotation(ObjectList<List<String>> messages) {
        if (sequentialIndex >= messages.size()) {
            sequentialIndex = 0;
        }
        sequentialIndex++;
    }
}