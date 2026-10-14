package com.example.fivelives;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReviveGUI implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, UUID> slots = new HashMap<>();

    public ReviveGUI(FiveLives plugin, Map<UUID, String> eliminated) {
        this.inventory = Bukkit.createInventory(this, 54, plugin.plain("gui-title"));

        int slot = 0;
        for (Map.Entry<UUID, String> entry : eliminated.entrySet()) {
            if (slot >= 54) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.getKey()));
            meta.setDisplayName(ChatColor.RED + entry.getValue());
            meta.setLore(List.of(ChatColor.GRAY + "Click to revive"));
            head.setItemMeta(meta);
            inventory.setItem(slot, head);
            slots.put(slot, entry.getKey());
            slot++;
        }
    }

    public UUID getTarget(int slot) {
        return slots.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
