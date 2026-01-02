package ru.overwrite.chat;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import ru.overwrite.chat.configuration.Config;
import ru.overwrite.chat.configuration.data.ChatChannel;
import ru.overwrite.chat.configuration.data.NewbieChatSettings;
import ru.overwrite.chat.utils.Utils;

import java.util.Collection;

public class ChatListener implements Listener {

    private final PromisedChat plugin;
    private final Config pluginConfig;

    public ChatListener(PromisedChat plugin) {
        this.plugin = plugin;
        this.pluginConfig = plugin.getPluginConfig();
    }

    private final String[] searchList = {"%player%", "%prefix%", "%suffix%", "%dph%"};

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();

        if (checkNewbie(p, e)) {
            return;
        }

        String rawMessage = e.getMessage();

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

        Collection<? extends Player> playersInRadius = getRadius(p, channel);

        String formatWithMessage = getFormatWithMessage(chatFormat, colorizedMessage);

        ChatChannel.HoverSettings hoverSettings = channel.hover();
        if (hoverSettings.hoverEnabled()) {
            e.setCancelled(true);
            sendHover(p, replacementList, playersInRadius, formatWithMessage, hoverSettings);
            return;
        }
        e.getRecipients().addAll(playersInRadius);
        e.setFormat(formatWithMessage);
    }

    private boolean checkNewbie(Player p, Cancellable e) {
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

    private void sendHover(Player p, String[] replacementList, Collection<? extends Player> recipients, String formatWithMessage, ChatChannel.HoverSettings hoverSettings) {
        String hoverText = Utils.colorize(Utils.replacePlaceholders(p, Utils.replaceEach(hoverSettings.hoverMessage(), searchList, replacementList)));
        HoverEvent hoverEvent = new HoverEvent(Action.SHOW_TEXT, new Text(TextComponent.fromLegacyText(hoverText)));
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
        // Костыли... костыли вечны.
        Bukkit.getConsoleSender().sendMessage(formatWithMessage);
    }

    private Collection<? extends Player> getRadius(Player p, ChatChannel chatChannel) {
        if (chatChannel.radius() < 0) {
            return Bukkit.getOnlinePlayers();
        }
        ObjectList<Player> plist = new ObjectArrayList<>();
        double maxDist = Math.pow(chatChannel.radius(), 2.0D);
        Location loc = p.getLocation();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() != p.getWorld()) {
                continue;
            }
            if (loc.distanceSquared(player.getLocation()) <= maxDist) {
                plist.add(player);
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
