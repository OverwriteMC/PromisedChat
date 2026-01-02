package ru.overwrite.chat.configuration;

import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import ru.overwrite.chat.configuration.data.AutoMessageSettings;
import ru.overwrite.chat.configuration.data.ChatChannel;
import ru.overwrite.chat.configuration.data.NewbieChatSettings;
import ru.overwrite.chat.utils.Utils;

@Getter
public class Config {

    private final Char2ObjectMap<ChatChannel> prefixMap = new Char2ObjectOpenHashMap<>();
    private ChatChannel defaultChannel;

    private NewbieChatSettings newbieChatSettings;
    private AutoMessageSettings autoMessageSettings;

    public static String timeHours, timeMinutes, timeSeconds;

    public void setupConfigs(FileConfiguration config) {
        setupChatFormats(config.getConfigurationSection("chatFormats"));

        this.newbieChatSettings = NewbieChatSettings.create(config.getConfigurationSection("newbieChat"));
        this.autoMessageSettings = AutoMessageSettings.create(config.getConfigurationSection("autoMessage"));

        final ConfigurationSection time = config.getConfigurationSection("placeholders.time"); // Потом когда-нить может быть заменено будет
        timeHours = Utils.colorize(time.getString("hours", " ч."));
        timeMinutes = Utils.colorize(time.getString("minutes", " мин."));
        timeSeconds = Utils.colorize(time.getString("seconds", " сек."));
    }

    private void setupChatFormats(ConfigurationSection chatFormats) {

        prefixMap.clear();

        ChatChannel.HoverSettings globalHover = ChatChannel.HoverSettings.create(chatFormats.getConfigurationSection("hoverText"));

        Object2ObjectMap<String, String> globalDonates = parseDonates(chatFormats.getConfigurationSection("donatePlaceholders"));

        String globalCooldownMessage = Utils.colorize(chatFormats.getString("cooldownMessage", ""));

        for (String key : chatFormats.getKeys(false)) {
            if (key.equals("hoverText") || key.equals("donatePlaceholders") || key.equals("cooldownMessage")) {
                continue;
            }

            ConfigurationSection section = chatFormats.getConfigurationSection(key);
            String prefixStr = section.getString("prefix", "");
            char prefixChar = prefixStr.isEmpty() ? '\0' : prefixStr.charAt(0);

            ChatChannel.HoverSettings channelHover = ChatChannel.HoverSettings.create(section.getConfigurationSection("hoverText"));
            if (channelHover == null) channelHover = globalHover;

            Object2ObjectMap<String, String> channelDonates = parseDonates(section.getConfigurationSection("donatePlaceholders"));
            if (channelDonates.isEmpty()) {
                channelDonates = globalDonates;
            }

            ChatChannel.CooldownSettings channelCooldownSettings = ChatChannel.CooldownSettings.create(section, globalCooldownMessage);

            String permission = section.getString("permission", "pchat.channel." + key);

            ChatChannel channel = new ChatChannel(
                    key,
                    section.getString("format"),
                    section.getInt("radius", -1),
                    prefixChar,
                    channelCooldownSettings,
                    channelHover,
                    channelDonates,
                    permission
            );

            if (prefixChar == '\0') {
                this.defaultChannel = channel;
            } else {
                prefixMap.put(prefixChar, channel);
            }
        }
    }

    private Object2ObjectMap<String, String> parseDonates(ConfigurationSection section) {
        Object2ObjectMap<String, String> map = new Object2ObjectOpenHashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String color = section.getString(key);
                if (color != null) {
                    map.put(key, color);
                }
            }
        }
        return map;
    }

    public ChatChannel findChannel(String message) {
        if (message.isEmpty()) return defaultChannel;
        return prefixMap.getOrDefault(message.charAt(0), defaultChannel);
    }
}