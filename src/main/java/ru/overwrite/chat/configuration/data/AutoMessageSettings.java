package ru.overwrite.chat.configuration.data;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.configuration.ConfigurationSection;
import ru.overwrite.chat.utils.Utils;

import java.util.List;
import java.util.Set;

public record AutoMessageSettings(
        boolean enabled,
        boolean random,
        int messageInterval,
        ObjectList<ObjectList<BaseComponent[]>> messages
) {
    public static AutoMessageSettings create(ConfigurationSection autoMessage) {

        if (!autoMessage.getBoolean("enable")) {
            return new AutoMessageSettings(false, false, 0, ObjectList.of());
        }

        // Если вы заебались читать этот кусок кода - я тоже заебался во время работы над ним вспоминая какой лист к чему относится
        ConfigurationSection messages = autoMessage.getConfigurationSection("messages");
        Set<String> keys = messages.getKeys(false);
        ObjectList<ObjectList<BaseComponent[]>> autoMessages = new ObjectArrayList<>(keys.size());
        for (String messageName : keys) {
            List<String> messagesList = messages.getStringList(messageName);
            ObjectList<BaseComponent[]> baseComponents = new ObjectArrayList<>(messagesList.size());
            for (String message : messagesList) {
                baseComponents.add(Utils.parseMessage(Utils.colorize(message), Utils.HOVER_MARKERS));
            }
            autoMessages.add(baseComponents);
        }

        return new AutoMessageSettings(
                true,
                autoMessage.getBoolean("random"),
                autoMessage.getInt("messageInterval"),
                autoMessages
        );
    }
}
