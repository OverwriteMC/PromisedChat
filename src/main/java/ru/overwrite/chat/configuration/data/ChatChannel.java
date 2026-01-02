package ru.overwrite.chat.configuration.data;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import ru.overwrite.chat.configuration.Config;
import ru.overwrite.chat.utils.TimedExpiringMap;
import ru.overwrite.chat.utils.Utils;

import java.util.concurrent.TimeUnit;

public record ChatChannel(
        String id,
        String format,
        int radius,
        char prefix,
        CooldownSettings cooldownSettings,
        HoverSettings hover,
        Object2ObjectMap<String, String> donatePlaceholders,
        String permission
) {

    public record HoverSettings(
            boolean hoverEnabled,
            String hoverMessage,
            boolean clickEventEnabled,
            String clickAction,
            String clickActionValue
    ) {
        public static HoverSettings create(ConfigurationSection hoverText) {
            if (hoverText == null) {
                return null;
            }

            ConfigurationSection clickEvent = hoverText.getConfigurationSection("clickEvent");

            boolean clickEnabled = false;
            String action = null;
            String actionValue = null;

            if (clickEvent != null) {
                clickEnabled = clickEvent.getBoolean("enable");
                action = clickEvent.getString("actionType");
                actionValue = clickEvent.getString("actionValue");
            }

            return new HoverSettings(
                    hoverText.getBoolean("enable"),
                    hoverText.getString("format"),
                    clickEnabled,
                    action,
                    actionValue
            );

        }
    }

    public record CooldownSettings(
            long cooldownTime,
            String cooldownMessage,
            TimedExpiringMap<String, Long> playerCooldowns
    ) {
        public static CooldownSettings create(ConfigurationSection section, String defaultMessage) {

            String message = section.getString("cooldownMessage");
            if (message == null) {
                message = defaultMessage;
            } else {
                message = Utils.colorize(message);
            }

            long time = section.getLong("cooldown", 0);

            return new CooldownSettings(
                    time,
                    message,
                    time > 0 ? new TimedExpiringMap<>(TimeUnit.MILLISECONDS) : null
            );
        }

        public boolean process(Player player) {
            String name = player.getName();

            if (cooldownTime <= 0) {
                return false;
            }
            if (playerCooldowns == null) {
                return false;
            }
            if (player.hasPermission("pchat.bypass.cooldown")) {
                return false;
            }

            Long insertionTime = playerCooldowns.get(name);

            if (insertionTime != null) {
                long currentTime = System.currentTimeMillis();
                long timeRemaining = (insertionTime + cooldownTime) - currentTime;
                String timeStr = Utils.getTime((int) (timeRemaining / 1000), Config.timeHours, Config.timeMinutes, Config.timeSeconds);
                player.sendMessage(cooldownMessage.replace("%time%", timeStr));
                return true;
            }

            setCooldown(name);
            return false;
        }

        public void setCooldown(String name) {
            if (playerCooldowns != null) {
                playerCooldowns.put(name, System.currentTimeMillis(), cooldownTime);
            }
        }
    }
}
