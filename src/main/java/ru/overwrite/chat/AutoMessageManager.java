package ru.overwrite.chat;

import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import lombok.Getter;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.overwrite.chat.configuration.Config;
import ru.overwrite.chat.configuration.data.AutoMessageSettings;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class AutoMessageManager {

    private final PromisedChat plugin;
    private final Config pluginConfig;
    private final Random random = new Random();

    private int randomIndex;
    private int sequentialIndex;

    private ObjectList<ObjectList<BaseComponent[]>> shuffledMessages;

    @Getter
    @Nullable
    private BukkitTask autoMessageTask;

    public AutoMessageManager(PromisedChat plugin) {
        this.plugin = plugin;
        this.pluginConfig = plugin.getPluginConfig();
    }

    public void clearData() {
        randomIndex = 0;
        sequentialIndex = 0;
        shuffledMessages = null;
        if (autoMessageTask != null) {
            autoMessageTask.cancel();
            autoMessageTask = null;
        }
    }

    public void startMSG() {
        AutoMessageSettings autoMessageSettings = pluginConfig.getAutoMessageSettings();
        if (!autoMessageSettings.enabled()) {
            return;
        }
        if (autoMessageTask != null) {
            autoMessageTask.cancel();
        }
        autoMessageTask = new BukkitRunnable() {
            @Override
            public void run() {
                List<BaseComponent[]> autoMessage = getAutoMessage();
                if (autoMessage == null || autoMessage.isEmpty()) {
                    return;
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.hasPermission("pchat.automessage")) {
                        continue;
                    }
                    for (BaseComponent[] msg : autoMessage) {
                        p.spigot().sendMessage(msg);
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L, autoMessageSettings.messageInterval() * 20L);
    }

    private ObjectList<BaseComponent[]> getAutoMessage() {
        AutoMessageSettings autoMessageSettings = pluginConfig.getAutoMessageSettings();
        ObjectList<ObjectList<BaseComponent[]>> messages = autoMessageSettings.messages();
        if (messages == null || messages.isEmpty()) {
            return ObjectList.of();
        }

        if (autoMessageSettings.random()) {
            handleRandomRotation(messages);
            return shuffledMessages.get(randomIndex - 1);
        } else {
            handleSequentialRotation(messages);
            return messages.get(sequentialIndex - 1);
        }
    }

    private void handleRandomRotation(ObjectList<ObjectList<BaseComponent[]>> messages) {
        if (shuffledMessages == null || randomIndex >= shuffledMessages.size()) {
            shuffledMessages = messages;
            ObjectLists.shuffle(shuffledMessages, random);
            randomIndex = 0;
        }
        randomIndex++;
    }

    private void handleSequentialRotation(ObjectList<ObjectList<BaseComponent[]>> messages) {
        if (sequentialIndex >= messages.size()) {
            sequentialIndex = 0;
        }
        sequentialIndex++;
    }
}