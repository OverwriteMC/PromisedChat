package ru.overwrite.chat.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import ru.overwrite.chat.ChatManager;
import ru.overwrite.chat.PromisedChat;
import ru.overwrite.chat.configuration.Config;

public class ChatListener implements Listener {

    private final ChatManager chatManager;
    private final Config pluginConfig;

    public ChatListener(PromisedChat plugin) {
        this.chatManager = plugin.getChatManager();
        this.pluginConfig = plugin.getPluginConfig();
    }

    // А в пизду захуячим 6 листенеров нахуй
    // Не ну а хуле делать

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChatLowest(AsyncPlayerChatEvent e) {
        if (pluginConfig.getChatPriority() != EventPriority.LOWEST) {
            return;
        }
        process(e);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChatLow(AsyncPlayerChatEvent e) {
        if (pluginConfig.getChatPriority() != EventPriority.LOW) {
            return;
        }
        process(e);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChatNormal(AsyncPlayerChatEvent e) {
        if (pluginConfig.getChatPriority() != EventPriority.NORMAL) {
            return;
        }
        process(e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChatHigh(AsyncPlayerChatEvent e) {
        if (pluginConfig.getChatPriority() != EventPriority.HIGH) {
            return;
        }
        process(e);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChatHighest(AsyncPlayerChatEvent e) {
        if (pluginConfig.getChatPriority() != EventPriority.HIGHEST) {
            return;
        }
        process(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChatMonitor(AsyncPlayerChatEvent e) {
        if (pluginConfig.getChatPriority() != EventPriority.MONITOR) {
            return;
        }
        process(e);
    }

    private void process(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();

        if (chatManager.checkNewbie(p, e)) {
            return;
        }

        String rawMessage = e.getMessage();

        chatManager.processChat(p, rawMessage, e);
    }

}
