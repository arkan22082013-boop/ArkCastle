package ru.kforbro.arkcastle.listeners;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.ItemStack;
import ru.kforbro.arkcastle.ArkCastle;
import ru.kforbro.arkcastle.Utils.Colorize;
import ru.kforbro.arkcastle.config.Config;
import ru.kforbro.arkcastle.shulkers.ShulkerBox;

public class PlayerListener implements Listener {
    private final ArkCastle plugin;

    public PlayerListener(ArkCastle plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteractSetup(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!this.plugin.getPlayersInSetup().contains(event.getPlayer().getUniqueId())) return;
        
        this.plugin.getConfigManager().getStorage().getShulkerBoxes().add(new ShulkerBox(block.getLocation(), Material.RED_SHULKER_BOX, -1));
        block.setType(Material.AIR);
        event.getPlayer().sendMessage("Shulker box location added!");
        this.plugin.getConfigManager().saveAll();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Проверяем, является ли блок шалкером
        if (!Tag.SHULKER_BOXES.isTagged(block.getType())) {
            return;
        }

        // Проверяем, есть ли такой шалкер в storage
        ShulkerBox shulkerBox = this.plugin.getShulkerBox(block.getLocation());
        if (shulkerBox == null) {
            return;
        }

        // Если игрок не может ломать - запрещаем
        if (!canDestroy(player)) {
            event.setCancelled(true);
            return;
        }

        // Отменяем стандартное разрушение
        event.setCancelled(true);

        if (shulkerBox.getDurability() == -1) {
            return;
        }

        if (shulkerBox.getDurability() > 0) {
            shulkerBox.setDurability(shulkerBox.getDurability() - 1);
            if (shulkerBox.getDurability() > 0) {
                Colorize.sendActionBar(player, "&x&f&a&5&d&5&dОсталось сломать еще &x&f&e&c&2&2&3" + shulkerBox.getDurability() + " &x&f&a&5&d&5&dраз");
            } else {
                // Шалкер сломан
                if (plugin.isGoldRushActive()) {
                    int chance = ThreadLocalRandom.current().nextInt(100);
                    if (chance < 30) {
                        float explosionPower = 4.0f;
                        shulkerBox.getLocation().getWorld().createExplosion(
                            shulkerBox.getLocation().clone().add(0.5, 0.5, 0.5),
                            explosionPower,
                            false,
                            false
                        );
                        Colorize.sendActionBar(player, "&cШалкер был взорван!");
                        block.setType(Material.AIR);
                        this.plugin.getConfigManager().saveAll();
                        return;
                    }
                }

                Colorize.sendActionBar(player, "&aШалкер был добыт!");
                dropLoot(shulkerBox);
                block.setType(Material.AIR);
                this.plugin.getConfigManager().saveAll();
            }
        }
    }

    private boolean canDestroy(Player player) {
        // Здесь можно добавить проверку на пермишн или регион
        // Например: player.hasPermission("arkcastle.break")
        // Или проверка на регион через WorldGuard
        return true;
    }

    private void dropLoot(ShulkerBox shulkerBox) {
        Config.ShulkerRarity rarity = null;
        for (Config.ShulkerRarity r : this.plugin.getConfigManager().getConfig().getRarities()) {
            if (r.material != shulkerBox.getMaterial()) continue;
            rarity = r;
            break;
        }
        if (rarity == null) return;

        List<ItemStack> loot = rarity.lootContent.getLoot();
        Location dropLocation = shulkerBox.getLocation().clone().add(0.5, 0.5, 0.5);

        for (ItemStack item : loot) {
            if (item == null) continue;
            if (item.getType() == Material.AIR) continue;
            Item droppedItem = shulkerBox.getLocation().getWorld().dropItemNaturally(dropLocation, item);
            droppedItem.setPickupDelay(0);
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null || !Tag.SHULKER_BOXES.isTagged(block.getType())) {
            return;
        }

        ShulkerBox shulkerBox = this.plugin.getShulkerBox(block.getLocation());
        if (shulkerBox == null) return;

        if (shulkerBox.getDurability() == -1) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            event.setCancelled(true);
            if (shulkerBox.getDurability() > 0) {
                Colorize.sendActionBar(player, "&x&f&a&5&d&5&dЧтобы получить лут, сломайте шалкер еще &x&f&e&c&2&2&3" + shulkerBox.getDurability() + " &x&f&a&5&d&5&dраз");
            }
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        Location location = event.getVehicle().getLocation();
        if (location.getX() <= 100.0 && location.getX() >= -100.0 && location.getZ() <= 100.0 && location.getZ() >= -100.0) {
            event.getVehicle().setPersistent(false);
        }
    }
}