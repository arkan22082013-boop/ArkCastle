package ru.kforbro.arkcastle.shulkers;

import org.bukkit.Location;
import org.bukkit.Material;

public class ShulkerBox {
    private Location location;
    private Material material;
    private int durability;
    private boolean spawned;

    public ShulkerBox() {
    }

    public ShulkerBox(Location location, Material material, int durability) {
        this.location = location;
        this.material = material;
        this.durability = durability;
        this.spawned = false;
    }

    public Location getLocation() {
        return this.location;
    }

    public Material getMaterial() {
        return this.material;
    }

    public int getDurability() {
        return this.durability;
    }

    public boolean isSpawned() {
        return this.spawned;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public void setDurability(int durability) {
        this.durability = durability;
    }

    public void setSpawned(boolean spawned) {
        this.spawned = spawned;
    }
}