package com.example.fivelives;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class LivesManager {

    private final FiveLives plugin;
    private final File file;
    private final YamlConfiguration data;

    public LivesManager(FiveLives plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public int getLives(UUID id) {
        return data.getInt("players." + id + ".lives", plugin.getConfig().getInt("starting-lives", 5));
    }

    public boolean isEliminated(UUID id) {
        return getLives(id) <= 0;
    }

    public void setLives(UUID id, int lives, String name) {
        data.set("players." + id + ".lives", Math.max(0, lives));
        if (name != null) data.set("players." + id + ".name", name);
        save();
    }

    /** Removes one life and returns the new total. */
    public int removeLife(Player p) {
        int lives = Math.max(0, getLives(p.getUniqueId()) - 1);
        setLives(p.getUniqueId(), lives, p.getName());
        return lives;
    }

    public void updateName(Player p) {
        String path = "players." + p.getUniqueId() + ".name";
        if (!p.getName().equals(data.getString(path))) {
            data.set(path, p.getName());
            save();
        }
    }

    /** All eliminated players (uuid -> name). */
    public Map<UUID, String> getEliminated() {
        Map<UUID, String> result = new LinkedHashMap<>();
        ConfigurationSection section = data.getConfigurationSection("players");
        if (section == null) return result;
        for (String key : section.getKeys(false)) {
            if (section.getInt(key + ".lives", 1) <= 0) {
                UUID id = UUID.fromString(key);
                String name = section.getString(key + ".name");
                if (name == null) name = Bukkit.getOfflinePlayer(id).getName();
                result.put(id, name == null ? key : name);
            }
        }
        return result;
    }

    /** Gives the player lives back and brings them back into the game. */
    public void revive(OfflinePlayer target, int lives) {
        setLives(target.getUniqueId(), lives, target.getName());

        if ("BAN".equalsIgnoreCase(plugin.getConfig().getString("elimination.mode"))
                && target.getName() != null) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pardon " + target.getName());
        }

        Player online = target.getPlayer();
        if (online != null) {
            restore(online);
        } else {
            // Offline: finish restoring when they next join
            data.set("players." + target.getUniqueId() + ".restore", true);
            save();
        }
    }

    public boolean consumeRestoreFlag(UUID id) {
        String path = "players." + id + ".restore";
        if (!data.getBoolean(path, false)) return false;
        data.set(path, null);
        save();
        return true;
    }

    /** Puts a revived player back into survival at their bed / world spawn. */
    public void restore(Player p) {
        p.setGameMode(GameMode.SURVIVAL);
        Location loc = p.getBedSpawnLocation();
        if (loc == null) loc = Bukkit.getWorlds().get(0).getSpawnLocation();
        p.teleport(loc);
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.sendMessage(plugin.msg("revive-restored", "lives", getLives(p.getUniqueId())));
    }

    public void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }
}
