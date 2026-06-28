package ru.overwrite.chat;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.messaging.Messenger;
import ru.overwrite.chat.configuration.Config;
import ru.overwrite.chat.configuration.data.ChatChannel;
import ru.overwrite.chat.configuration.data.NewbieChatSettings;
import ru.overwrite.chat.utils.Utils;

public class ChatManager {

    private final PromisedChat plugin;
    private final Config pluginConfig;
    private final String[] searchList = {"%player%", "%prefix%", "%suffix%", "%dph%"};
    private PluginMessage pluginMessage;
    private Listener registeredListener;

    public ChatManager(PromisedChat plugin) {
        this.plugin = plugin;
        this.pluginConfig = plugin.getPluginConfig();
        this.setupProxy();
    }

    private void setupProxy() {
        if (pluginConfig.isProxy()) {
            Messenger messenger = Bukkit.getMessenger();
            messenger.registerOutgoingPluginChannel(plugin, "BungeeCord");
            pluginMessage = new PluginMessage(plugin);
            messenger.registerIncomingPluginChannel(plugin, "BungeeCord", pluginMessage);
        }
    }

    public void register() {
        unregister();

        EventPriority priority = pluginConfig.getChatPriority();
        Listener listener = new Listener() {
        };
        EventExecutor executor = (ignored, event) -> {
            if (!(event instanceof AsyncPlayerChatEvent chatEvent)) {
                return;
            }
            process(chatEvent);
        };

        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvent(
                AsyncPlayerChatEvent.class,
                listener,
                priority,
                executor,
                plugin,
                true
        );

        this.registeredListener = listener;
    }

    public void unregister() {
        if (registeredListener != null) {
            HandlerList.unregisterAll(registeredListener);
            registeredListener = null;
        }
    }

    private void process(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();

        if (checkNewbie(p, e)) {
            return;
        }

        String rawMessage = e.getMessage();

        processChat(p, rawMessage, e);
    }

    public void processChat(Player p, String rawMessage, AsyncPlayerChatEvent e) {
        ChatChannel channel = pluginConfig.findChannel(rawMessage);

        if (!channel.equals(pluginConfig.getDefaultChannel()) && !p.hasPermission(channel.permission())) {
            channel = pluginConfig.getDefaultChannel();
        }

        String message = (channel.prefix() != '\0' && rawMessage.charAt(0) == channel.prefix())
                ? rawMessage.substring(1).trim() : rawMessage;

        if (message.isEmpty()) {
            e.setCancelled(true);
            return;
        }

        if (channel.cooldownSettings().process(p)) {
            e.setCancelled(true);
            return;
        }

        String donatePlaceholder = plugin.getPerms() != null ? getDonatePlaceholder(p, channel) : "";
        String prefix = plugin.getChat() != null ? plugin.getChat().getPlayerPrefix(p) : "";
        String suffix = plugin.getChat() != null ? plugin.getChat().getPlayerSuffix(p) : "";

        String[] replacementList = {p.getName(), prefix, suffix, donatePlaceholder};

        String colorizedMessage = Utils.formatByPerm(p, message);

        String chatFormat = Utils.colorize(Utils.replacePlaceholders(p, Utils.replaceEach(channel.format(), searchList, replacementList)));

        e.getRecipients().clear();

        ObjectList<Player> playersInRadius = getRadius(p, channel);

        String formatWithMessage = getFormatWithMessage(chatFormat, colorizedMessage);

        if (sendHover(p, replacementList, playersInRadius, formatWithMessage, channel)) {
            e.setCancelled(true);
            return;
        }
        e.getRecipients().addAll(playersInRadius);
        e.setFormat(formatWithMessage);
        if (pluginMessage != null && channel.radius() < 0) {
            pluginMessage.sendCrossProxy(p, formatWithMessage, channel.permission(), false);
        }
    }

    public boolean checkNewbie(Player p, Cancellable e) {
        NewbieChatSettings newbieChatSettings = pluginConfig.getNewbieChatSettings();
        if (newbieChatSettings.enabled()) {
            if (p.hasPermission("pchat.bypass.newbie")) {
                return false;
            }
            long time = (System.currentTimeMillis() - p.getFirstPlayed()) / 1000;
            if (time <= newbieChatSettings.cooldown()) {
                String cooldown = Utils.getTime((int) (newbieChatSettings.cooldown() - time), Config.timeHours, Config.timeMinutes, Config.timeSeconds);
                p.sendMessage(newbieChatSettings.message().replace("%time%", cooldown));
                e.setCancelled(true);
                return true;
            }
        }
        return false;
    }

    public boolean sendHover(Player p, String[] replacementList, ObjectList<Player> recipients, String formatWithMessage, ChatChannel channel) {
        ChatChannel.HoverSettings hoverSettings = channel.hover();
        if (hoverSettings == null || !hoverSettings.hoverEnabled()) {
            return false;
        }
        String hoverText = Utils.colorize(Utils.replacePlaceholders(p, Utils.replaceEach(hoverSettings.hoverMessage(), searchList, replacementList)));
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(TextComponent.fromLegacyText(hoverText)));
        BaseComponent[] comp = TextComponent.fromLegacyText(formatWithMessage);
        for (BaseComponent component : comp) {
            component.setHoverEvent(hoverEvent);
        }
        if (hoverSettings.clickEventEnabled()) {
            ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.valueOf(hoverSettings.clickAction()), Utils.replacePlaceholders(p, Utils.replaceEach(hoverSettings.clickActionValue(), searchList, replacementList)));
            for (BaseComponent component : comp) {
                component.setClickEvent(clickEvent);
            }
        }
        for (Player recipient : recipients) {
            recipient.spigot().sendMessage(comp);
        }
        if (pluginMessage != null && channel.radius() < 0) {
            pluginMessage.sendCrossProxy(p, ComponentSerializer.toString(comp), channel.permission(), true);
        }
        // Костыли... костыли вечны.
        Bukkit.getConsoleSender().sendMessage(formatWithMessage);
        return true;
    }

    private ObjectList<Player> getRadius(Player p, ChatChannel chatChannel) {
        ObjectList<Player> plist = new ObjectArrayList<>();
        double radius = chatChannel.radius();
        boolean useRadius = radius >= 0;
        double maxDist = Math.pow(chatChannel.radius(), 2.0D);
        Location loc = useRadius ? p.getLocation() : null;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!useRadius) {
                if (onlinePlayer.hasPermission(chatChannel.permission())) {
                    plist.add(onlinePlayer);
                }
                continue;
            }

            if (p.getWorld() == onlinePlayer.getWorld() && loc.distanceSquared(onlinePlayer.getLocation()) <= maxDist) {
                plist.add(onlinePlayer);
            }
        }

        return plist;
    }

    private String getFormatWithMessage(String format, String chatMessage) {
        return format
                .replace("%message%", chatMessage)
                .replace("%", "%%"); // Это надо чтобы PAPI не выёбывался
    }

    private String getDonatePlaceholder(Player p, ChatChannel chatChannel) {
        String primaryGroup = plugin.getPerms().getPrimaryGroup(p);
        return chatChannel.donatePlaceholders().getOrDefault(primaryGroup, "");
    }
}
