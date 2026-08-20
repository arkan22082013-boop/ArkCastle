package ru.kforbro.arkcastle.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import ru.kforbro.arkcastle.ArkCastle;
import ru.kforbro.arkcastle.shulkers.ShulkerBox;

public class ConfigManager {
    private final ArkCastle plugin;
    private Storage storage;
    private CustomItem customItem;
    private Config config;
    
    private File storageFile;
    private File customItemFile;
    private File configFile;

    public ConfigManager(ArkCastle plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "storage.yml");
        this.customItemFile = new File(plugin.getDataFolder(), "customItems.yml");
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        
        plugin.getDataFolder().mkdirs();
        this.loadAll();
    }

    public void loadAll() {
        this.loadConfig();
        this.loadStorage();
        this.loadCustomItems();
    }

    public void loadStorage() {
        if (!storageFile.exists()) {
            try {
                storageFile.createNewFile();
                this.storage = new Storage();
                this.saveStorage();
                return;
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create storage.yml");
            }
        }
        
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(storageFile);
        this.storage = new Storage();
        
        List<ShulkerBox> boxes = new ArrayList<>();
        List<?> list = yaml.getList("shulkerBoxes");
        if (list != null) {
            for (Object obj : list) {
                if (obj instanceof String) {
                    String[] parts = ((String) obj).split(";");
                    if (parts.length == 5) {
                        try {
                            String worldName = parts[0];
                            int x = Integer.parseInt(parts[1]);
                            int y = Integer.parseInt(parts[2]);
                            int z = Integer.parseInt(parts[3]);
                            Material material = Material.valueOf(parts[4]);
                            
                            Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);
                            ShulkerBox box = new ShulkerBox(loc, material, -1);
                            boxes.add(box);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to load shulker box: " + obj);
                        }
                    }
                }
            }
        }
        this.storage.setShulkerBoxes(boxes);
        plugin.getLogger().info("Loaded " + boxes.size() + " shulker boxes");
    }

    public void saveStorage() {
        FileConfiguration yaml = new YamlConfiguration();
        List<String> serialized = new ArrayList<>();
        
        for (ShulkerBox box : storage.getShulkerBoxes()) {
            Location loc = box.getLocation();
            String data = loc.getWorld().getName() + ";" + 
                          loc.getBlockX() + ";" + 
                          loc.getBlockY() + ";" + 
                          loc.getBlockZ() + ";" + 
                          box.getMaterial().name();
            serialized.add(data);
        }
        
        yaml.set("shulkerBoxes", serialized);
        
        try {
            yaml.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save storage.yml");
        }
    }

    public void loadCustomItems() {
        this.customItem = new CustomItem();
        
        if (!customItemFile.exists()) {
            try {
                customItemFile.createNewFile();
                plugin.getLogger().info("Created customItems.yml");
                return;
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create customItems.yml");
                return;
            }
        }
        
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(customItemFile);
        
        // Загружаем кастомные предметы
        if (yaml.contains("items")) {
            for (String key : yaml.getConfigurationSection("items").getKeys(false)) {
                try {
                    // Получаем сериализованный предмет
                    String serialized = yaml.getString("items." + key);
                    if (serialized == null || serialized.isEmpty()) continue;
                    
                    // Создаем временный файл для десериализации
                    File tempFile = File.createTempFile("temp", ".yml");
                    tempFile.deleteOnExit();
                    
                    // Записываем сериализованный предмет во временный файл
                    java.io.FileWriter writer = new java.io.FileWriter(tempFile);
                    writer.write("item:\n");
                    // Добавляем отступы для правильного формата
                    String[] lines = serialized.split("\n");
                    for (String line : lines) {
                        writer.write("  " + line + "\n");
                    }
                    writer.close();
                    
                    // Загружаем предмет
                    FileConfiguration tempYaml = YamlConfiguration.loadConfiguration(tempFile);
                    ItemStack item = tempYaml.getItemStack("item");
                    if (item != null && item.getType() != Material.AIR) {
                        this.customItem.getItems().put(key, item);
                        plugin.getLogger().info("Loaded custom item: " + key);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load custom item: " + key + " - " + e.getMessage());
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + this.customItem.getItems().size() + " custom items");
    }

    public void saveCustomItems() {
        FileConfiguration yaml = new YamlConfiguration();
        
        // Сохраняем каждый предмет
        for (Map.Entry<String, ItemStack> entry : customItem.getItems().entrySet()) {
            try {
                // Создаем временный YAML для сериализации
                File tempFile = File.createTempFile("temp", ".yml");
                tempFile.deleteOnExit();
                YamlConfiguration tempYaml = new YamlConfiguration();
                tempYaml.set("item", entry.getValue());
                tempYaml.save(tempFile);
                
                // Читаем сериализованную строку
                FileConfiguration loadedYaml = YamlConfiguration.loadConfiguration(tempFile);
                String serialized = loadedYaml.saveToString();
                
                // Убираем "item: " из строки
                String[] lines = serialized.split("\n");
                StringBuilder itemData = new StringBuilder();
                for (int i = 1; i < lines.length; i++) {
                    if (i > 1) itemData.append("\n");
                    itemData.append(lines[i]);
                }
                
                yaml.set("items." + entry.getKey(), itemData.toString());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save custom item: " + entry.getKey() + " - " + e.getMessage());
            }
        }
        
        try {
            yaml.save(customItemFile);
            plugin.getLogger().info("Saved " + customItem.getItems().size() + " custom items");
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save customItems.yml: " + e.getMessage());
        }
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        this.config = new Config(plugin);
        plugin.getLogger().info("Config loaded!");
    }

    public void saveAll() {
        this.saveStorage();
        this.saveCustomItems();
        plugin.getLogger().info("All configs saved!");
    }

    public Storage getStorage() {
        return this.storage;
    }

    public CustomItem getCustomItem() {
        return this.customItem;
    }

    public Config getConfig() {
        return this.config;
    }
}