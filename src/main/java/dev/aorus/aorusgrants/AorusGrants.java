package dev.aorus.aorusgrants;

import dev.aorus.aorusgrants.commands.AgAdminCommand;
import dev.aorus.aorusgrants.commands.AgCommand;
import dev.aorus.aorusgrants.listeners.MenuListener;
import dev.aorus.aorusgrants.managers.*;
import dev.aorus.aorusgrants.managers.HistoryManager;
import net.luckperms.api.LuckPerms;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class AorusGrants extends JavaPlugin {

    private static AorusGrants instance;
    private LuckPerms luckPerms;
    private ConfigManager configManager;
    private GroupManager groupManager;
    private GroupItemStorage groupItemStorage;
    private SessionManager sessionManager;
    private LPExecutor lpExecutor;
    private HistoryManager historyManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("menus.yml", false);

        RegisteredServiceProvider<LuckPerms> provider =
                getServer().getServicesManager().getRegistration(LuckPerms.class);

        if (provider == null) {
            getLogger().severe("LuckPerms not found! Disabling AorusGrants.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        luckPerms = provider.getProvider();
        getLogger().info("LuckPerms hooked successfully.");

        configManager    = new ConfigManager(this);
        groupItemStorage = new GroupItemStorage(this);
        groupManager     = new GroupManager(this);
        sessionManager   = new SessionManager(this);
        lpExecutor       = new LPExecutor(this);
        historyManager   = new HistoryManager(this);

        // /ag command
        AgCommand agCommand = new AgCommand(this);
        getCommand("ag").setExecutor(agCommand);
        getCommand("ag").setTabCompleter(agCommand);

        // /agadmin command
        AgAdminCommand agAdminCommand = new AgAdminCommand(this);
        getCommand("agadmin").setExecutor(agAdminCommand);
        getCommand("agadmin").setTabCompleter(agAdminCommand);

        getServer().getPluginManager().registerEvents(new MenuListener(this), this);

        getLogger().info("AorusGrants v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (sessionManager != null) sessionManager.clearAll();
        getLogger().info("AorusGrants disabled.");
    }

    public static AorusGrants getInstance() { return instance; }
    public LuckPerms getLuckPerms()          { return luckPerms; }
    public ConfigManager getConfigManager()  { return configManager; }
    public GroupManager getGroupManager()    { return groupManager; }
    public GroupItemStorage getGroupItemStorage() { return groupItemStorage; }
    public SessionManager getSessionManager(){ return sessionManager; }
    public LPExecutor getLpExecutor()        { return lpExecutor; }
    public HistoryManager getHistoryManager(){ return historyManager; }
}
