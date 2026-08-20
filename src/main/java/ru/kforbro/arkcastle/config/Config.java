package ru.kforbro.arkcastle.config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import ru.kforbro.arkcastle.Utils.WeighedProbability;
import ru.kforbro.arkcastle.ArkCastle;

public class Config {
    private List<ShulkerRarity> rarities = new ArrayList<>();
    private SpawnSettings spawnSettings = new SpawnSettings();
    
    private final ArkCastle plugin;

    public Config(ArkCastle plugin) {
        this.plugin = plugin;
        this.loadFromFile();
    }

    public void loadFromFile() {
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.getLogger().warning("config.yml not found, using defaults!");
            setDefaults();
            return;
        }
        
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        
        if (yaml.contains("spawn-settings")) {
            int base = yaml.getInt("spawn-settings.base-count", 4);
            int max = yaml.getInt("spawn-settings.max-count", 8);
            int interval = yaml.getInt("spawn-settings.spawn-interval-seconds", 600);
            this.spawnSettings = new SpawnSettings(base, max, interval);
        }
        
        if (yaml.contains("rarities")) {
            this.rarities.clear();
            List<?> rarityList = yaml.getList("rarities");
            if (rarityList != null) {
                for (Object obj : rarityList) {
                    if (obj instanceof java.util.Map) {
                        java.util.Map<?, ?> map = (java.util.Map<?, ?>) obj;
                        try {
                            double weight = ((Number) map.get("weight")).doubleValue();
                            String materialName = (String) map.get("material");
                            int durability = ((Number) map.get("durability")).intValue();
                            
                            Material material = Material.valueOf(materialName);
                            
                            java.util.Map<?, ?> lootMap = (java.util.Map<?, ?>) map.get("lootContent");
                            double mean = ((Number) lootMap.get("mean")).doubleValue();
                            double stdDev = ((Number) lootMap.get("standardDeviation")).doubleValue();
                            
                            List<LootEntry> items = new ArrayList<>();
                            List<?> itemsList = (List<?>) lootMap.get("items");
                            if (itemsList != null) {
                                for (Object itemObj : itemsList) {
                                    java.util.Map<?, ?> itemMap = (java.util.Map<?, ?>) itemObj;
                                    double itemWeight = ((Number) itemMap.get("weight")).doubleValue();
                                    int minAmount = ((Number) itemMap.get("minAmount")).intValue();
                                    int maxAmount = ((Number) itemMap.get("maxAmount")).intValue();
                                    
                                    Material itemMaterial = null;
                                    String customItem = null;
                                    
                                    if (itemMap.containsKey("material") && itemMap.get("material") != null) {
                                        itemMaterial = Material.valueOf((String) itemMap.get("material"));
                                    }
                                    if (itemMap.containsKey("customItem") && itemMap.get("customItem") != null) {
                                        customItem = (String) itemMap.get("customItem");
                                    }
                                    
                                    items.add(new LootEntry(itemWeight, itemMaterial, minAmount, maxAmount, customItem));
                                }
                            }
                            
                            LootContent lootContent = new LootContent(mean, stdDev, items);
                            this.rarities.add(new ShulkerRarity(weight, material, durability, lootContent));
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to load rarity: " + e.getMessage());
                        }
                    }
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + rarities.size() + " rarities");
        
        if (rarities.isEmpty()) {
            plugin.getLogger().warning("No rarities loaded, using defaults!");
            setDefaults();
        }
    }

    private void setDefaults() {
        List<LootEntry> items = new ArrayList<>();
        items.add(new LootEntry(50.0, Material.DIAMOND, 8, 32, null));
        items.add(new LootEntry(100.0, Material.IRON_INGOT, 24, 54, null));
        LootContent lootContent = new LootContent(4.0, 3.0, items);
        rarities.add(new ShulkerRarity(50.0, Material.GRAY_SHULKER_BOX, 5, lootContent));
    }

    public List<ShulkerRarity> getRarities() {
        return this.rarities;
    }

    public void setRarities(List<ShulkerRarity> rarities) {
        this.rarities = rarities;
    }

    public SpawnSettings getSpawnSettings() {
        return spawnSettings;
    }

    public void setSpawnSettings(SpawnSettings spawnSettings) {
        this.spawnSettings = spawnSettings;
    }

    public static class ShulkerRarity {
        public double weight;
        public Material material;
        public int durability;
        public LootContent lootContent;

        public ShulkerRarity() {}

        public ShulkerRarity(double weight, Material material, int durability, LootContent lootContent) {
            this.weight = weight;
            this.material = material;
            this.durability = durability;
            this.lootContent = lootContent;
        }
    }

    public static class LootContent {
        public double mean;
        public double standardDeviation;
        public List<LootEntry> items;

        public LootContent() {}

        public LootContent(double mean, double standardDeviation, List<LootEntry> items) {
            this.mean = mean;
            this.standardDeviation = standardDeviation;
            this.items = items;
        }

        public List<ItemStack> getLoot() {
            int amount = (int)Math.max(Math.ceil(ThreadLocalRandom.current().nextGaussian(this.mean, this.standardDeviation)), 0.0);
            amount++;
            List<ItemStack> loot = new ArrayList<>();
            for (int i = 0; i < amount; i++) {
                ItemStack itemStack = this.rollLoot();
                if (itemStack == null) continue;
                if (itemStack.getType() == Material.AIR) continue;
                loot.add(itemStack);
            }
            return loot;
        }

        public ItemStack rollLoot() {
            HashMap<Integer, Double> weightsMap = new HashMap<Integer, Double>();
            for (int i = this.items.size() - 1; i >= 0; i--) {
                weightsMap.put(i, this.items.get(i).weight);
            }
            Integer selected = WeighedProbability.pickWeighedProbability(weightsMap);
            if (selected == null) return null;
            return this.items.get(selected).getItemStack();
        }
    }

    public static class LootEntry {
        public double weight;
        public Material material;
        public int minAmount;
        public int maxAmount;
        public String customItem;

        private transient ArkCastle plugin;

        public LootEntry() {}

        public LootEntry(double weight, Material material, int minAmount, int maxAmount, String customItem) {
            this.weight = weight;
            this.material = material;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.customItem = customItem;
        }

        public ItemStack getItemStack() {
            ItemStack itemStack = null;
            if (this.material != null) {
                itemStack = new ItemStack(this.material);
            } else if (this.customItem != null && !this.customItem.isEmpty()) {
                ArkCastle plugin = ArkCastle.getInstance();
                if (plugin != null && plugin.getConfigManager() != null) {
                    ItemStack custom = plugin.getConfigManager().getCustomItem().getItems().get(this.customItem);
                    if (custom != null) {
                        itemStack = custom.clone();
                    } else if (plugin.getLogger() != null) {
                        plugin.getLogger().warning("Custom item not found: " + this.customItem);
                    }
                }
            }
            if (itemStack != null) {
                if (this.minAmount < this.maxAmount) {
                    itemStack.setAmount(ThreadLocalRandom.current().nextInt(this.minAmount, this.maxAmount + 1));
                } else {
                    itemStack.setAmount(this.minAmount);
                }
                return itemStack;
            }
            return null;
        }
    }

    public static class SpawnSettings {
        public int baseCount = 4;
        public int maxCount = 8;
        public int spawnIntervalSeconds = 600;

        public SpawnSettings() {}

        public SpawnSettings(int baseCount, int maxCount, int spawnIntervalSeconds) {
            this.baseCount = baseCount;
            this.maxCount = maxCount;
            this.spawnIntervalSeconds = spawnIntervalSeconds;
        }
    }
}