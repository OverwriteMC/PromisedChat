package ru.overwrite.chat;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.plugin.messaging.Messenger;
import ru.overwrite.chat.configuration.Config;
import ru.overwrite.chat.configuration.data.ChatChannel;
import ru.overwrite.chat.configuration.data.NewbieChatSettings;
import ru.overwrite.chat.utils.Utils;

public class ChatManager {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private final PromisedChat plugin;
    private final Config pluginConfig;
    private final String[] searchList = {"%player%", "%prefix%", "%suffix%", "%dph%"};
    private PluginMessage pluginMessage;

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

    public PreparedChatMessage prepareChat(Player p, String rawMessage) {
        ChatChannel channel = pluginConfig.findChannel(rawMessage);

        if (!channel.equals(pluginConfig.getDefaultChannel()) && !p.hasPermission(channel.permission())) {
            channel = pluginConfig.getDefaultChannel();
        }

        String message = stripPrefix(channel, rawMessage);

        if (message.isEmpty()) {
            return null;
        }

        if (channel.cooldownSettings().process(p)) {
            return null;
        }

        String donatePlaceholder = plugin.getPerms() != null ? getDonatePlaceholder(p, channel) : "";
        String prefix = plugin.getChat() != null ? plugin.getChat().getPlayerPrefix(p) : "";
        String suffix = plugin.getChat() != null ? plugin.getChat().getPlayerSuffix(p) : "";

        String[] replacementList = {p.getName(), prefix, suffix, donatePlaceholder};

        String colorizedMessage = Utils.formatByPerm(p, message);

        String chatFormat = Utils.colorize(Utils.replacePlaceholders(p, Utils.replaceEach(channel.format(), searchList, replacementList)));

        String renderedMessage = chatFormat.replace("%message%", colorizedMessage);

        String hoverText = null;
        String clickAction = null;
        String clickActionValue = null;

        ChatChannel.HoverSettings hoverSettings = channel.hover();
        if (hoverSettings != null && hoverSettings.hoverEnabled()) {
            hoverText = Utils.colorize(Utils.replacePlaceholders(p, Utils.replaceEach(hoverSettings.hoverMessage(), searchList, replacementList)));
            if (hoverSettings.clickEventEnabled()) {
                clickAction = hoverSettings.clickAction();
                clickActionValue = Utils.replacePlaceholders(p, Utils.replaceEach(hoverSettings.clickActionValue(), searchList, replacementList));
            }
        }

        return new PreparedChatMessage(
                p,
                channel,
                renderedMessage,
                getRadius(p, channel),
                hoverText,
                clickAction,
                clickActionValue
        );
    }

    public Component createPaperComponent(PreparedChatMessage prepared) {
        Component component = LEGACY_SERIALIZER.deserialize(prepared.renderedMessage());

        if (!prepared.hoverEnabled()) {
            return component;
        }

        Component hoverComponent = LEGACY_SERIALIZER.deserialize(prepared.hoverText());
        Component wrapped = Component.empty().append(component).hoverEvent(HoverEvent.showText(hoverComponent));

        ClickEvent clickEvent = createPaperClickEvent(prepared);
        return clickEvent != null ? wrapped.clickEvent(clickEvent) : wrapped;
    }

    public void forwardProxy(PreparedChatMessage prepared) {
        if (pluginMessage == null || prepared.channel().radius() >= 0) {
            return;
        }

        if (prepared.hoverEnabled()) {
            pluginMessage.sendCrossProxy(
                    prepared.player(),
                    ComponentSerializer.toString(createProxyComponents(prepared)),
                    prepared.channel().permission(),
                    true
            );
            return;
        }

        pluginMessage.sendCrossProxy(prepared.player(), prepared.renderedMessage(), prepared.channel().permission(), false);
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

    private String stripPrefix(ChatChannel channel, String rawMessage) {
        return channel.prefix() != '\0' && !rawMessage.isEmpty() && rawMessage.charAt(0) == channel.prefix()
                ? rawMessage.substring(1).trim()
                : rawMessage;
    }

    private BaseComponent[] createProxyComponents(PreparedChatMessage prepared) {
        BaseComponent[] components = TextComponent.fromLegacyText(prepared.renderedMessage());
        net.md_5.bungee.api.chat.HoverEvent hoverEvent = new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new Text(TextComponent.fromLegacyText(prepared.hoverText()))
        );

        for (BaseComponent component : components) {
            component.setHoverEvent(hoverEvent);
        }

        net.md_5.bungee.api.chat.ClickEvent clickEvent = createProxyClickEvent(prepared);
        if (clickEvent != null) {
            for (BaseComponent component : components) {
                component.setClickEvent(clickEvent);
            }
        }

        return components;
    }

    private ClickEvent createPaperClickEvent(PreparedChatMessage prepared) {
        if (prepared.clickAction() == null || prepared.clickActionValue() == null) {
            return null;
        }

        try {
            return ClickEvent.clickEvent(ClickEvent.Action.valueOf(prepared.clickAction()), prepared.clickActionValue());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown click action for Paper chat: " + prepared.clickAction());
            return null;
        }
    }

    private net.md_5.bungee.api.chat.ClickEvent createProxyClickEvent(PreparedChatMessage prepared) {
        if (prepared.clickAction() == null || prepared.clickActionValue() == null) {
            return null;
        }

        try {
            return new net.md_5.bungee.api.chat.ClickEvent(
                    net.md_5.bungee.api.chat.ClickEvent.Action.valueOf(prepared.clickAction()),
                    prepared.clickActionValue()
            );
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown click action for proxy chat: " + prepared.clickAction());
            return null;
        }
    }

    private String getDonatePlaceholder(Player p, ChatChannel chatChannel) {
        String primaryGroup = plugin.getPerms().getPrimaryGroup(p);
        return chatChannel.donatePlaceholders().getOrDefault(primaryGroup, "");
    }

    public record PreparedChatMessage(
            Player player,
            ChatChannel channel,
            String renderedMessage,
            ObjectList<Player> recipients,
            String hoverText,
            String clickAction,
            String clickActionValue
    ) {
        public boolean hoverEnabled() {
            return hoverText != null && !hoverText.isEmpty();
        }
    }
}
