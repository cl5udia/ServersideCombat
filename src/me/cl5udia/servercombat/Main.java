package me.cl5udia.servercombat;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = this.getConfig();
        float maxReach = (float) config.getDouble("maximum_reach");
        boolean throughBlocks = config.getBoolean("hit_through_blocks");
        getServer().getPluginManager().registerEvents(new CombatManager(maxReach, throughBlocks, this), this);
    }
}