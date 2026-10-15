package com.example.fivelives;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FiveLiveCommand implements CommandExecutor, TabCompleter {

    private final FiveLives plugin;

    public FiveLiveCommand(FiveLives plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.msg("usage"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> give(sender, args);
            case "lives" -> lives(sender, args);
            case "set" -> set(sender, args);
            case "revive" -> revive(sender, args);
            case "reload" -> {
                if (!isAdmin(sender)) return noPerm(sender);
                plugin.reloadConfig();
                sender.sendMessage(plugin.msg("reloaded"));
            }
            default -> sender.sendMessage(plugin.msg("usage"));
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        if (!isAdmin(sender)) { noPerm(sender); return; }
        if (args.length < 2 || !args[1].equalsIgnoreCase("REVIVE")) {
            sender.sendMessage(plugin.msg("usage"));
            return;
        }

        Player target = null;
        if (args.length >= 3) target = Bukkit.getPlayerExact(args[2]);
        else if (sender instanceof Player p) target = p;

        if (target == null) {
            sender.sendMessage(plugin.msg("player-not-found"));
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
            } catch (NumberFormatException ignored) { }
        }

        ItemStack item = plugin.reviveItem().create(amount);
        for (ItemStack leftover : target.getInventory().addItem(item).values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), leftover);
        }

        sender.sendMessage(plugin.msg("revive-given", "amount", amount, "player", target.getName()));
        if (!target.equals(sender)) {
            target.sendMessage(plugin.msg("revive-received", "amount", amount));
        }
    }

    private void lives(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            if (!isAdmin(sender) && !(sender instanceof Player p && p.getName().equalsIgnoreCase(args[1]))) {
                noPerm(sender);
                return;
            }
            OfflinePlayer target = find(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.msg("player-not-found"));
                return;
            }
            sender.sendMessage(plugin.msg("lives-other",
                    "player", target.getName(), "lives", plugin.lives().getLives(target.getUniqueId())));
        } else if (sender instanceof Player p) {
            sender.sendMessage(plugin.msg("lives-self", "lives", plugin.lives().getLives(p.getUniqueId())));
        } else {
            sender.sendMessage(plugin.msg("usage"));
        }
    }

    private void set(CommandSender sender, String[] args) {
        if (!isAdmin(sender)) { noPerm(sender); return; }
        if (args.length < 3) { sender.sendMessage(plugin.msg("usage")); return; }

        OfflinePlayer target = find(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.msg("player-not-found"));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.msg("usage"));
            return;
        }
        if (amount < 1) {
            sender.sendMessage(plugin.msg("usage"));
            return;
        }

        if (plugin.lives().isEliminated(target.getUniqueId())) {
            plugin.lives().revive(target, amount);
        } else {
            plugin.lives().setLives(target.getUniqueId(), amount, target.getName());
        }
        sender.sendMessage(plugin.msg("set-done", "player", target.getName(), "lives", amount));
    }

    private void revive(CommandSender sender, String[] args) {
        if (!isAdmin(sender)) { noPerm(sender); return; }
        if (args.length < 2) { sender.sendMessage(plugin.msg("usage")); return; }

        OfflinePlayer target = find(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.msg("player-not-found"));
            return;
        }
        if (!plugin.lives().isEliminated(target.getUniqueId())) {
            sender.sendMessage(plugin.msg("not-eliminated", "player", target.getName()));
            return;
        }

        plugin.lives().revive(target, plugin.getConfig().getInt("revive.lives-restored", 3));
        Bukkit.broadcastMessage(plugin.msg("revive-success",
                "player", target.getName(), "reviver", sender.getName()));
    }

    private boolean isAdmin(CommandSender sender) {
        return sender.hasPermission("fivelive.admin");
    }

    private boolean noPerm(CommandSender sender) {
        sender.sendMessage(plugin.msg("no-permission"));
        return true;
    }

    private OfflinePlayer find(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        OfflinePlayer off = Bukkit.getOfflinePlayer(name);
        return off.hasPlayedBefore() ? off : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        boolean admin = isAdmin(sender);

        if (args.length == 1) {
            out.add("lives");
            if (admin) out.addAll(List.of("give", "set", "revive", "reload"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "give" -> { if (admin) out.add("REVIVE"); }
                case "lives", "set", "revive" -> { if (admin) addOnline(out); }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give") && admin) {
            addOnline(out);
        }

        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        return out.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(last)).collect(Collectors.toList());
    }

    private void addOnline(List<String> out) {
        Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
    }
}
