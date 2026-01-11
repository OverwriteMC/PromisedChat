package ru.overwrite.chat;

import lombok.Getter;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.overwrite.chat.configuration.Config;
import ru.overwrite.chat.utils.Metrics;
import ru.overwrite.chat.utils.Utils;

@Getter
public final class PromisedChat extends JavaPlugin {

    private Chat chat;

    private Permission perms;

    private final Config pluginConfig = new Config();
    private final ChatManager chatManager = new ChatManager(this);
    private final AutoMessageManager autoMessageManager = new AutoMessageManager(this);

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        saveDefaultConfig();
        pluginConfig.setupConfigs(getConfig());
        ServicesManager servicesManager = getServer().getServicesManager();
        setupChat(servicesManager);
        PluginManager pluginManager = getServer().getPluginManager();
        setupPerms(servicesManager, pluginManager);
        setupPlaceholders(pluginManager);
        pluginManager.registerEvents(new ChatListener(this), this);
        pluginManager.registerEvents(new CommandListener(this), this);
        autoMessageManager.startMSG();
        getCommand("promisedchat").setExecutor(new PromisedChatCommand(this));
        new Metrics(this, 20699);
        long endTime = System.currentTimeMillis();
        getLogger().info("Plugin started in " + (endTime - startTime) + " ms");
    }

    private void setupChat(ServicesManager servicesManager) {
        chat = getProvider(servicesManager, Chat.class);
        if (chat == null) {
            getLogger().info("Менеджер чата не подключён!");
            getLogger().info("Функционал плагина не будет полноценно использован.");
            return;
        }
        getLogger().info("Менеджер чата подключён!");
    }

    private void setupPerms(ServicesManager servicesManager, PluginManager pluginManager) {
        perms = pluginManager.isPluginEnabled("LuckPerms") ? getProvider(servicesManager, Permission.class) : null;
        if (perms == null) {
            getLogger().info("Менеджер прав не подключён!");
            getLogger().info("Функционал плагина не будет полноценно использован.");
            return;
        }
        getLogger().info("Менеджер прав подключён!");
    }

    private void setupPlaceholders(PluginManager pluginManager) {
        if (!pluginManager.isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        Utils.USE_PAPI = true;
        getLogger().info("Плейсхолдеры подключены!");
    }

    private <T> T getProvider(ServicesManager servicesManager, Class<T> clazz) {
        final RegisteredServiceProvider<T> provider = servicesManager.getRegistration(clazz);
        return provider != null ? provider.getProvider() : null;
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }
}
