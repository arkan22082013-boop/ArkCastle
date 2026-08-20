package ru.kforbro.arkcastle.config;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import ru.kforbro.arkcastle.shulkers.ShulkerBox;

public class Storage {
    private List<ShulkerBox> shulkerBoxes = new ArrayList<>();

    public Storage() {
        World world = Bukkit.getWorld("world");
        if (world != null) {
            shulkerBoxes.add(new ShulkerBox(new Location(world, 0, 0, 0), Material.RED_SHULKER_BOX, -1));
        }
    }

    public List<ShulkerBox> getShulkerBoxes() {
        return this.shulkerBoxes;
    }

    public void setShulkerBoxes(List<ShulkerBox> shulkerBoxes) {
        this.shulkerBoxes = shulkerBoxes;
    }
}