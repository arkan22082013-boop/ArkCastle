package ru.kforbro.arkcastle.config;

import java.util.HashMap;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class CustomItem {
    private HashMap<String, ItemStack> items = new HashMap<>();

    public CustomItem() {
        items.put("TESTITEM", new ItemStack(Material.STRING));
    }

    public HashMap<String, ItemStack> getItems() {
        return this.items;
    }

    public void setItems(HashMap<String, ItemStack> items) {
        this.items = items;
    }
}