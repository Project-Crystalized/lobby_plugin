package gg.crystalized.lobby;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

public class InvListener implements Listener {

    @EventHandler
    public void onRightClick(PlayerInteractEvent event){
        if (!event.getAction().isRightClick()) {
            return;
        }

        if(event.getItem() == null) return;
        Inventory inv = null;
        switch(event.getMaterial()){
            case Material.COMPASS: inv = Bukkit.getServer().createInventory(null, 54, "\uA000" + "\uA006");
            case Material.EMERALD: inv = Bukkit.getServer().createInventory(null, 54, "\uA000" + "\uA004");
        }
        if(inv == null) return;
        event.getPlayer().openInventory(inv);
    }
}
