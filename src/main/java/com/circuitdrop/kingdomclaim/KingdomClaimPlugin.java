package com.circuitdrop.kingdomclaim;

import com.circuitdrop.kingdomclaim.command.KingdomCommand;
import com.circuitdrop.kingdomclaim.manager.DataManager;
import com.circuitdrop.kingdomclaim.manager.KingdomManager;
import com.circuitdrop.kingdomclaim.manager.PlayerDataManager;
import com.circuitdrop.kingdomclaim.manager.WarManager;
import com.circuitdrop.kingdomclaim.listener.GuiListener;
import com.circuitdrop.kingdomclaim.listener.KingdomChatListener;
import com.circuitdrop.kingdomclaim.listener.PlayerLifecycleListener;
import com.circuitdrop.kingdomclaim.listener.ProtectionListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class KingdomClaimPlugin extends JavaPlugin {

    private KingdomManager kingdomManager;
    private PlayerDataManager playerDataManager;
    private DataManager dataManager;
    private WarManager warManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        kingdomManager = new KingdomManager();
        playerDataManager = new PlayerDataManager();
        dataManager = new DataManager(this);
        dataManager.load(kingdomManager);
        warManager = new WarManager(kingdomManager, this);

        getServer().getPluginManager().registerEvents(new ProtectionListener(kingdomManager, playerDataManager, warManager), this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(new KingdomChatListener(kingdomManager, playerDataManager), this);
        getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(kingdomManager, playerDataManager), this);

        KingdomCommand kingdomCommand = new KingdomCommand(this, kingdomManager, playerDataManager, warManager);
        var command = getCommand("kingdom");
        if (command != null) {
            command.setExecutor(kingdomCommand);
            command.setTabCompleter(kingdomCommand);
        }

        long autosaveTicks = 20L * 60L * getConfig().getInt("autosave-minutes", 5);
        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> dataManager.save(kingdomManager), autosaveTicks, autosaveTicks);

        getLogger().info("KingdomClaim enabled: " + kingdomManager.all().size() + " kingdoms loaded.");
    }

    @Override
    public void onDisable() {
        if (dataManager != null && kingdomManager != null) {
            dataManager.save(kingdomManager);
        }
    }

    public KingdomManager getKingdomManager() {
        return kingdomManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public WarManager getWarManager() {
        return warManager;
    }
}
