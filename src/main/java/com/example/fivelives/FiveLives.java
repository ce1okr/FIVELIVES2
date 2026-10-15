package com.example.fivelives;

import com.example.fivelives.listeners.CombatListener;
import com.example.fivelives.listeners.LifeListener;
import com.example.fivelives.listeners.ReviveListener;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class FiveLives extends JavaPlugin {

    private LivesManager livesManager;
    private CombatManager combatManager;
    private ReviveItem reviveItem;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        livesManager = new LivesManager(this);
        combatManager = new CombatManager(this);
        reviveItem = new ReviveItem(this);

        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new LifeListener(this), this);
        getServer().getPluginManager().registerEvents(new ReviveListener(this), this);

        PluginCommand cmd = getCommand("fivelive");
        if (cmd != null) {
            FiveLiveCommand handler = new FiveLiveCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        combatManager.start();
        getLogger().info("FiveLives enabled.");
    }

    @Override
    public void onDisable() {
        if (livesManager != null) livesManager.save();
    }

    public LivesManager lives() { return livesManager; }
    public CombatManager combat() { return combatManager; }
    public ReviveItem reviveItem() { return reviveItem; }

    /** Message with prefix. Placeholders: pass key/value pairs, e.g. msg("x", "player", name). */
    public String msg(String key, Object... kv) {
        return color(getConfig().getString("messages.prefix", "")) + plain(key, kv);
    }

    /** Message without prefix. */
    public String plain(String key, Object... kv) {
        String raw = getConfig().getString("messages." + key, "&cMissing message: " + key);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            raw = raw.replace("{" + kv[i] + "}", String.valueOf(kv[i + 1]));
        }
        return color(raw);
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
