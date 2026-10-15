package com.example.fivelives.listeners;

import com.example.fivelives.FiveLives;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class LifeListener implements Listener {

    private final FiveLives plugin;

    public LifeListener(FiveLives plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();

        boolean killedByPlayer = p.getKiller() != null && !p.getKiller().equals(p);
        boolean inCombat = plugin.combat().isTagged(p);
        plugin.combat().clear(p);

        // Lives are only lost to players or while in combat
        if (!killedByPlayer && !inCombat) return;

        int lives = plugin.lives().removeLife(p);

        if (lives > 0) {
            p.sendMessage(plugin.msg("lost-life", "lives", lives));
            return;
        }

        // Eliminated
        p.sendMessage(plugin.msg("eliminated-self"));
        Bukkit.broadcastMessage(plugin.msg("eliminated-broadcast", "player", p.getName()));

        if (isBanMode()) {
            String name = p.getName();
            String reason = plugin.getConfig().getString("messages.ban-reason", "You ran out of lives!");
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + name + " " + reason), 40L);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (!isBanMode() && plugin.lives().isEliminated(p.getUniqueId())) {
            Bukkit.getScheduler().runTask(plugin, () -> p.setGameMode(GameMode.SPECTATOR));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        plugin.lives().updateName(p);

        // Revived while offline
        if (plugin.lives().consumeRestoreFlag(id)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.lives().restore(p), 5L);
            return;
        }

        if (!isBanMode() && plugin.lives().isEliminated(id)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> p.setGameMode(GameMode.SPECTATOR), 5L);
        }
    }

    private boolean isBanMode() {
        return "BAN".equalsIgnoreCase(plugin.getConfig().getString("elimination.mode", "SPECTATOR"));
    }
}
