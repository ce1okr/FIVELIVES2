package com.example.fivelives.listeners;

import com.example.fivelives.FiveLives;
import com.example.fivelives.ReviveGUI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class ReviveListener implements Listener {

    private final FiveLives plugin;

    public ReviveListener(FiveLives plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        if (!plugin.reviveItem().isRevive(p.getInventory().getItemInMainHand())) return;

        e.setCancelled(true);

        if (plugin.getConfig().getBoolean("revive.op-only-use", false) && !p.isOp()) {
            p.sendMessage(plugin.msg("revive-op-only"));
            return;
        }

        Map<UUID, String> eliminated = plugin.lives().getEliminated();
        if (eliminated.isEmpty()) {
            p.sendMessage(plugin.msg("revive-none"));
            return;
        }

        p.openInventory(new ReviveGUI(plugin, eliminated).getInventory());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof ReviveGUI gui)) return;

        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != top) return;

        UUID targetId = gui.getTarget(e.getSlot());
        if (targetId == null) return;

        if (plugin.getConfig().getBoolean("revive.op-only-use", false) && !p.isOp()) {
            p.closeInventory();
            p.sendMessage(plugin.msg("revive-op-only"));
            return;
        }

        if (!plugin.lives().isEliminated(targetId)) {
            p.closeInventory();
            return;
        }

        if (!consumeOne(p)) {
            p.closeInventory();
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        plugin.lives().revive(target, plugin.getConfig().getInt("revive.lives-restored", 3));
        p.closeInventory();
        Bukkit.broadcastMessage(plugin.msg("revive-success",
                "player", target.getName(), "reviver", p.getName()));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof ReviveGUI) {
            e.setCancelled(true);
        }
    }

    private boolean consumeOne(Player p) {
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (plugin.reviveItem().isRevive(stack)) {
                if (stack.getAmount() > 1) {
                    stack.setAmount(stack.getAmount() - 1);
                } else {
                    p.getInventory().setItem(i, null);
                }
                return true;
            }
        }
        return false;
    }
}
