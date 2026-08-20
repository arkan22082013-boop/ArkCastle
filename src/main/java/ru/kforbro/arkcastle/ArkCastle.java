package ru.kforbro.arkcastle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ru.kforbro.arkcastle.Utils.Colorize;
import ru.kforbro.arkcastle.Utils.TimeUtils;
import ru.kforbro.arkcastle.Utils.WeighedProbability;
import ru.kforbro.arkcastle.commands.ArkCastleCommand;
import ru.kforbro.arkcastle.config.Config;
import ru.kforbro.arkcastle.config.ConfigManager;
import ru.kforbro.arkcastle.listeners.PlayerListener;
import ru.kforbro.arkcastle.shulkers.ShulkerBox;

public final class ArkCastle extends JavaPlugin {
    private static ArkCastle instance;
    private ConfigManager configManager;
    private final Set<UUID> playersInSetup = new HashSet<UUID>();
    private long nextSpawnTime = 0L;
    private boolean goldRushActive = false;
    private int spawnTaskId = -1;

    @Override
    public void onEnable() {
        instance = this;
        this.configManager = new ConfigManager(this);
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        new ArkCastleCommand(this);
        
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Colorize.sendMessage(player, "");
                Colorize.sendMessage(player, "&9 &n┃&f В центре обычного мира &7(X: 0, Z: 0) &fнаходится замок,");
                Colorize.sendMessage(player, "&9 ┃&f на котором раз в &x&f&e&c&2&2&3" + TimeUtils.prettyTime(this.getSpawnInterval()) + " &fпоявляются ящики.");
                Colorize.sendMessage(player, "");
            }
        }, 0L, 36000L);
        
        this.startSpawnTask();
        this.nextSpawnTime = System.currentTimeMillis() + 10000L;
        getLogger().info("ArkCastle enabled!");
    }

    @Override
    public void onDisable() {
        if (this.spawnTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.spawnTaskId);
        }
        if (this.configManager != null) {
            this.despawnShulkerBoxes();
            this.configManager.saveAll();
        }
        getLogger().info("ArkCastle disabled!");
    }

    public void startSpawnTask() {
        if (this.spawnTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.spawnTaskId);
        }
        
        this.spawnTaskId = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (this.nextSpawnTime < System.currentTimeMillis()) {
                if (goldRushActive) {
                    this.nextSpawnTime = System.currentTimeMillis() + 120000L;
                    this.spawnGoldShulkerBoxes();
                } else {
                    this.nextSpawnTime = System.currentTimeMillis() + (long)this.getSpawnInterval() * 1000L;
                    this.spawnShulkerBoxes();
                }
            }
        }, 20L, 20L).getTaskId();
    }

    public void reload() {
        getLogger().info("Reloading ArkCastle...");
        
        if (this.spawnTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.spawnTaskId);
            this.spawnTaskId = -1;
        }
        
        this.despawnShulkerBoxes();
        this.configManager.loadAll();
        this.nextSpawnTime = System.currentTimeMillis() + 5000L;
        this.startSpawnTask();
        
        getLogger().info("ArkCastle reloaded!");
        for (Player player : Bukkit.getOnlinePlayers()) {
            Colorize.sendMessage(player, "&a&lArkCastle &8┃ &fПлагин перезагружен!");
        }
    }

    public int getShulkerCount() {
        Config.SpawnSettings settings = this.configManager.getConfig().getSpawnSettings();
        int base = settings.baseCount;
        int max = settings.maxCount;
        int count = ThreadLocalRandom.current().nextInt(base, max + 1);
        return count;
    }

    public int getSpawnInterval() {
        return this.configManager.getConfig().getSpawnSettings().spawnIntervalSeconds;
    }

    public ShulkerBox getShulkerBox(Location location) {
        for (ShulkerBox shulkerBox : this.configManager.getStorage().getShulkerBoxes()) {
            if (!shulkerBox.getLocation().equals(location)) continue;
            return shulkerBox;
        }
        return null;
    }

    public void spawnShulkerBoxes() {
        this.despawnShulkerBoxes();
        int count = this.getShulkerCount();
        ArrayList<ShulkerBox> boxes = new ArrayList<ShulkerBox>(this.configManager.getStorage().getShulkerBoxes());
        Collections.shuffle(boxes);
        
        if (boxes.isEmpty()) {
            getLogger().warning("No shulker boxes found in storage!");
            return;
        }
        
        for (int i = 0; i < Math.min(count, boxes.size()); i++) {
            ShulkerBox box = boxes.get(i);
            Block block = box.getLocation().getBlock();
            Config.ShulkerRarity rarity = this.getRandomRarity();
            
            if (rarity == null) {
                getLogger().warning("No rarity found!");
                continue;
            }
            
            block.setType(rarity.material);
            box.setMaterial(rarity.material);
            box.setDurability(rarity.durability);
            box.setSpawned(true);
            
            BlockState blockState = block.getState();
            if (blockState instanceof Container) {
                Container container = (Container)blockState;
                List<ItemStack> loot = rarity.lootContent.getLoot();
                for (ItemStack item : loot) {
                    if (item == null) continue;
                    container.getInventory().addItem(item);
                }
                container.update();
            }
            box.getLocation().getWorld().createExplosion(box.getLocation().clone().add(0.5, 1.5, 0.5), 5.0f, false, false);
        }
        this.configManager.saveAll();
    }

    public void spawnGoldShulkerBoxes() {
        this.despawnShulkerBoxes();
        ArrayList<ShulkerBox> boxes = new ArrayList<ShulkerBox>(this.configManager.getStorage().getShulkerBoxes());
        Collections.shuffle(boxes);
        
        if (boxes.isEmpty()) {
            getLogger().warning("No shulker boxes found in storage!");
            return;
        }
        
        Config.ShulkerRarity rarity = null;
        for (Config.ShulkerRarity r : this.configManager.getConfig().getRarities()) {
            if (r.material == Material.YELLOW_SHULKER_BOX) {
                rarity = r;
                break;
            }
        }
        if (rarity == null) {
            List<Config.ShulkerRarity> rarities = this.configManager.getConfig().getRarities();
            if (!rarities.isEmpty()) {
                rarity = rarities.get(rarities.size() - 1);
            } else {
                getLogger().warning("No rarities found for gold rush!");
                return;
            }
        }
        
        for (ShulkerBox box : boxes) {
            Block block = box.getLocation().getBlock();
            block.setType(rarity.material);
            box.setMaterial(rarity.material);
            box.setDurability(rarity.durability);
            box.setSpawned(true);
            
            BlockState blockState = block.getState();
            if (blockState instanceof Container) {
                Container container = (Container)blockState;
                List<ItemStack> loot = rarity.lootContent.getLoot();
                for (ItemStack item : loot) {
                    if (item == null) continue;
                    container.getInventory().addItem(item);
                }
                container.update();
            }
            box.getLocation().getWorld().createExplosion(box.getLocation().clone().add(0.5, 1.5, 0.5), 6.0f, false, false);
        }
        this.configManager.saveAll();
    }

    public void despawnShulkerBoxes() {
        if (this.configManager == null || this.configManager.getStorage() == null) return;
        for (ShulkerBox shulkerBox : this.configManager.getStorage().getShulkerBoxes()) {
            this.despawnShulkerBox(shulkerBox);
        }
    }

    public void despawnShulkerBox(ShulkerBox shulkerBox) {
        shulkerBox.getLocation().getBlock().setType(Material.AIR);
        shulkerBox.setSpawned(false);
        shulkerBox.setDurability(-1);
    }

    public Config.ShulkerRarity getRandomRarity() {
        HashMap<Integer, Double> weightsMap = new HashMap<Integer, Double>();
        ArrayList<Config.ShulkerRarity> rarities = new ArrayList<Config.ShulkerRarity>(this.configManager.getConfig().getRarities());
        
        rarities.removeIf(r -> r.material == Material.YELLOW_SHULKER_BOX);
        
        if (rarities.isEmpty()) {
            getLogger().warning("No rarities available for spawning!");
            return null;
        }
        
        for (int i = rarities.size() - 1; i >= 0; i--) {
            weightsMap.put(i, rarities.get(i).weight);
        }
        Integer selected = WeighedProbability.pickWeighedProbability(weightsMap);
        if (selected == null) return rarities.get(0);
        return rarities.get(selected);
    }

    public void startGoldRush() {
        this.goldRushActive = true;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Colorize.sendMessage(player, "&9 &n┃&f Золотая лихорадка только что началась!");
            Colorize.sendMessage(player, "&9 ┃&f Координаты: &x&f&e&c&2&2&3X: 0, Z: 0");
        }
        this.nextSpawnTime = System.currentTimeMillis() - 1000;
    }

    public void stopGoldRush() {
        this.goldRushActive = false;
        this.despawnShulkerBoxes();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Colorize.sendMessage(player, "&9 &n┃&f Золотая лихорадка закончилась!");
        }
        this.nextSpawnTime = System.currentTimeMillis() + (long)this.getSpawnInterval() * 1000L;
    }

    public static ArkCastle getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public Set<UUID> getPlayersInSetup() {
        return this.playersInSetup;
    }

    public boolean isGoldRushActive() {
        return goldRushActive;
    }
}