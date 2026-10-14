package com.example.fivelives;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ReviveItem {

    private final FiveLives plugin;
    private final NamespacedKey key;

    public ReviveItem(FiveLives plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "revive_item");
    }

    public ItemStack create(int amount) {
        Material mat = Material.matchMaterial(plugin.getConfig().getString("revive.item-material", "NETHER_STAR"));
        if (mat == null || mat.isAir()) mat = Material.NETHER_STAR;

        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(FiveLives.color(plugin.getConfig().getString("revive.item-name", "&b&lRevive Token")));
        meta.setLore(List.of(
                FiveLives.color("&7Right-click to bring back"),
                FiveLives.color("&7an eliminated player.")));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isRevive(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
