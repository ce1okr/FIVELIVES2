package com.example.fivelives;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class CombatManager {

    private final FiveLives plugin;
    private final Map<UUID, Long> tags = new HashMap<>();

    public CombatManager(FiveLives plugin) {
        this.plugin = plugin;
    }

    /** Starts or refreshes the combat timer for a player. */
    public void tag(Player p) {
        long duration = plugin.getConfig().getLong("combat.duration-seconds", 45) * 1000L;
        boolean already = isTagged(p);
        tags.put(p.getUniqueId(), System.currentTimeMillis() + duration);
        if (!already) {
            p.sendMessage(plugin.msg("combat-start", "seconds", secondsLeft(p)));
        }
    }

    public boolean isTagged(Player p) {
        Long expiry = tags.get(p.getUniqueId());
        return expiry != null && expiry > System.currentTimeMillis();
    }

    public int secondsLeft(Player p) {
        Long expiry = tags.get(p.getUniqueId());
        if (expiry == null) return 0;
        return (int) Math.max(0, Math.ceil((expiry - System.currentTimeMillis()) / 1000.0));
    }

    public void clear(Player p) {
        tags.remove(p.getUniqueId());
    }

    /** Ticks once per second: shows the action bar and ends expired timers. */
    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Iterator<Map.Entry<UUID, Long>> it = tags.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Long> entry = it.next();
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p == null) {
                    it.remove();
                    continue;
                }
                if (entry.getValue() <= System.currentTimeMillis()) {
                    it.remove();
                    p.sendMessage(plugin.msg("combat-end"));
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText(plugin.plain("combat-end")));
                } else {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText(
                                    plugin.plain("combat-actionbar", "seconds", secondsLeft(p))));
                }
            }
        }, 20L, 20L);
    }
}
