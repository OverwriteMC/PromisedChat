package ru.overwrite.chat.listener;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import ru.overwrite.chat.ChatManager;
import ru.overwrite.chat.ChatManager.PreparedChatMessage;
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
    public void onChatLowest(AsyncChatEvent e) {
        if (pluginConfig.getChatPriority() != EventPriority.LOWEST) {
            return;
        }
        process(e);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChatLow(AsyncChatEvent e) {
        if (pluginConfig.getChatPriority() != EventPriority.LOW) {
            return;
        }
        process(e);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChatNormal(AsyncChatEvent e) {
        if (pluginConfig.getChatPriority() != EventPriority.NORMAL) {
            return;
        }
        process(e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChatHigh(AsyncChatEvent e) {
        if (pluginConfig.getChatPriority() != EventPriority.HIGH) {
            return;
        }
        process(e);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChatHighest(AsyncChatEvent e) {
        if (pluginConfig.getChatPriority() != EventPriority.HIGHEST) {
            return;
        }
        process(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChatMonitor(AsyncChatEvent e) {
        if (pluginConfig.getChatPriority() != EventPriority.MONITOR) {
            return;
        }
        process(e);
    }

    private void process(AsyncChatEvent e) {
        Player player = e.getPlayer();

        if (chatManager.checkNewbie(player, e)) {
            return;
        }

        PreparedChatMessage prepared = chatManager.prepareChat(player, PlainTextComponentSerializer.plainText().serialize(e.message()));
        if (prepared == null) {
            e.setCancelled(true);
            return;
        }

        e.viewers().removeIf(viewer -> shouldRemoveViewer(viewer, prepared));

        Component renderedMessage = chatManager.createPaperComponent(prepared);
        e.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, message) -> renderedMessage));

        chatManager.forwardProxy(prepared);
    }

    private boolean shouldRemoveViewer(Audience viewer, PreparedChatMessage prepared) {
        return viewer instanceof Player player && !prepared.recipients().contains(player);
    }
}
