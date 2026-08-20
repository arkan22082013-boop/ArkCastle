package ru.kforbro.arkcastle.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.kforbro.arkcastle.ArkCastle;
import ru.kforbro.arkcastle.shulkers.ShulkerBox;

public class ArkCastleCommand implements CommandExecutor {
    private final ArkCastle plugin;

    public ArkCastleCommand(ArkCastle plugin) {
        this.plugin = plugin;
        this.plugin.getCommand("arkcastle").setExecutor(this);
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("arkcastle.admin")) {
            sender.sendMessage("§cNo permission!");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§6=== ArkCastle Commands ===");
            sender.sendMessage("§e/arkcastle customitem add <name> §7- Add custom item");
            sender.sendMessage("§e/arkcastle customitem remove <name> §7- Remove custom item");
            sender.sendMessage("§e/arkcastle customitem list §7- List custom items");
            sender.sendMessage("§e/arkcastle setup §7- Enter setup mode");
            sender.sendMessage("§e/arkcastle spawn §7- Force spawn");
            sender.sendMessage("§e/arkcastle scan §7- Scan world");
            sender.sendMessage("§e/arkcastle goldrush start §7- Start GoldRush");
            sender.sendMessage("§e/arkcastle goldrush stop §7- Stop GoldRush");
            sender.sendMessage("§e/arkcastle reload §7- Reload plugin");
        } else if (args[0].equalsIgnoreCase("customitem")) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /arkcastle customitem add <name>");
                sender.sendMessage("§cUsage: /arkcastle customitem remove <name>");
                sender.sendMessage("§cUsage: /arkcastle customitem list");
                return true;
            }
            
            if (args[1].equalsIgnoreCase("add") && args.length > 2) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this command!");
                    return true;
                }
                Player player = (Player)sender;
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType() == Material.AIR) {
                    player.sendMessage("§cYou must hold an item in your hand!");
                    return true;
                }
                
                // Добавляем предмет в CustomItem
                this.plugin.getConfigManager().getCustomItem().getItems().put(args[2], item.clone());
                
                // Сохраняем в файл
                this.plugin.getConfigManager().saveCustomItems();
                
                player.sendMessage("§aCustom item added: " + args[2]);
                plugin.getLogger().info("Custom item added: " + args[2] + " by " + player.getName());
                return true;
            }
            
            if (args[1].equalsIgnoreCase("remove") && args.length > 2) {
                String key = args[2];
                if (this.plugin.getConfigManager().getCustomItem().getItems().containsKey(key)) {
                    this.plugin.getConfigManager().getCustomItem().getItems().remove(key);
                    this.plugin.getConfigManager().saveCustomItems();
                    sender.sendMessage("§aCustom item removed: " + key);
                    plugin.getLogger().info("Custom item removed: " + key + " by " + sender.getName());
                } else {
                    sender.sendMessage("§cCustom item not found: " + key);
                }
                return true;
            }
            
            if (args[1].equalsIgnoreCase("list")) {
                sender.sendMessage("§6=== Custom Items (" + this.plugin.getConfigManager().getCustomItem().getItems().size() + ") ===");
                for (String key : this.plugin.getConfigManager().getCustomItem().getItems().keySet()) {
                    sender.sendMessage("§e- " + key);
                }
                return true;
            }
        } else if (args[0].equalsIgnoreCase("setup")) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player)sender;
            if (this.plugin.getPlayersInSetup().contains(player.getUniqueId())) {
                this.plugin.getPlayersInSetup().remove(player.getUniqueId());
                player.sendMessage("§aExited setup mode");
            } else {
                this.plugin.getPlayersInSetup().add(player.getUniqueId());
                player.sendMessage("§aEntered setup mode - left click blocks to add shulker boxes");
            }
        } else if (args[0].equalsIgnoreCase("spawn")) {
            this.plugin.spawnShulkerBoxes();
            sender.sendMessage("§aShulker boxes spawned!");
        } else if (args[0].equalsIgnoreCase("scan")) {
            this.plugin.getConfigManager().getStorage().getShulkerBoxes().clear();
            int count = 0;
            for (int x = -50; x < 50; x++) {
                for (int y = 0; y < 150; y++) {
                    for (int z = -50; z < 50; z++) {
                        Location location = new Location(Bukkit.getWorld("world"), x, y, z);
                        Block block = location.getBlock();
                        if (!block.getType().equals(Material.CRYING_OBSIDIAN)) continue;
                        this.plugin.getConfigManager().getStorage().getShulkerBoxes().add(new ShulkerBox(location.clone().add(0.0, 1.0, 0.0), Material.RED_SHULKER_BOX, -1));
                        count++;
                    }
                }
            }
            this.plugin.getConfigManager().saveAll();
            sender.sendMessage("§aScanned " + count + " shulker boxes");
        } else if (args[0].equalsIgnoreCase("goldrush")) {
            if (args.length < 2) return true;
            if (args[1].equalsIgnoreCase("start")) {
                this.plugin.startGoldRush();
                sender.sendMessage("§aGoldRush started!");
            } else if (args[1].equalsIgnoreCase("stop")) {
                this.plugin.stopGoldRush();
                sender.sendMessage("§cGoldRush stopped!");
            }
        } else if (args[0].equalsIgnoreCase("reload")) {
            this.plugin.reload();
            sender.sendMessage("§aArkCastle reloaded!");
        }
        return false;
    }
}