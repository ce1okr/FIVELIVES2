package com.example.fivelives.listeners;

import com.example.fivelives.FiveLives;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;

public class CombatListener implements Listener {

    private final FiveLives plugin;

    public CombatListener(FiveLives plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;

        Player attacker = resolvePlayer(e.getDamager());

        if (attacker != null) {
            if (attacker.equals(victim)) return; // self damage (own pearl/arrow etc.)
            plugin.combat().tag(victim);
            plugin.combat().tag(attacker);
        } else if (plugin.getConfig().getBoolean("combat.tag-on-mob-damage", false)) {
            plugin.combat().tag(victim);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (!plugin.combat().isTagged(p)) return;
        if (p.hasPermission("fivelive.bypass")) return;

        String message = e.getMessage();
        if (message.length() < 2) return;

        String label = message.substring(1).trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':'); // handle essentials:home etc.
        if (colon >= 0) label = label.substring(colon + 1);

        for (String blocked : plugin.getConfig().getStringList("combat.blocked-commands")) {
            if (blocked.equalsIgnoreCase(label)) {
                e.setCancelled(true);
                p.sendMessage(plugin.msg("command-blocked", "seconds", plugin.combat().secondsLeft(p)));
                return;
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (plugin.combat().isTagged(p)
                && plugin.getConfig().getBoolean("combat.kill-on-logout", true)
                && p.getHealth() > 0) {
            org.bukkit.Bukkit.broadcastMessage(plugin.msg("combat-logged-broadcast", "player", p.getName()));
            p.setHealth(0); // fires PlayerDeathEvent while still tagged -> costs a life
        }
        plugin.combat().clear(p);
    }

    private Player resolvePlayer(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }
}
