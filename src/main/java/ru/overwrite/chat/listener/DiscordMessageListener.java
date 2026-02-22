package ru.overwrite.chat.listener;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;
import ru.overwrite.chat.configuration.Config;
import ru.overwrite.chat.configuration.data.ChatChannel;

public class DiscordMessageListener {

    private final Config config;

    public DiscordMessageListener(Config config) {
        this.config = config;
    }

    @Subscribe
    public void onDiscordMessage(GameChatMessagePreProcessEvent e) {
        String message = e.getMessage();
        ChatChannel channel = config.findChannel(message);
        // Повторение - мать учения.
        // Да ужж, но я ничего лучше не придумал.
        if (channel.radius() != -1 || !e.getPlayer().hasPermission(channel.permission())) {
            e.setCancelled(true);
            return;
        }
        if (channel.prefix() != '\0') {
            e.setMessage(message.substring(1));
        }
    }

    public void subscribe() {
        DiscordSRV.api.subscribe(this);
    }

    public void unsubscribe() {
        DiscordSRV.api.unsubscribe(this);
    }
}
