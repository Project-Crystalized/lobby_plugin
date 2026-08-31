package gg.crystalized.lobby;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.bukkit.Material.COAL;

public enum Navigation {
    Spawn(LobbyConfig.Locations.get("spawn"), 0, "Spawn"),
    Litestrike(LobbyConfig.Locations.get("litestrike_hub"), 1, "Litestrike"),
    Knockoff(LobbyConfig.Locations.get("knockoff_hub"), 2, "Knockoff"),
    CrystalWars(LobbyConfig.Locations.get("crystalblitz_hub"), 3, "Crystal Wars");

    final Location loc;
    final int inventorySlot;
    final String name;
    Navigation(Location loc, int inventorySlot, String name){
        this.loc = loc;
        this.inventorySlot = inventorySlot;
        this.name = name;
    }

    public ItemStack build(){
        ItemStack item = new ItemStack(COAL);
        //TODO add texture for this
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).color(WHITE).decoration(ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    public static void placeNavigation(Player p, Inventory inv){
        //TODO place this in action
        for(Navigation nav : Navigation.values()){
            inv.setItem(nav.inventorySlot, nav.build());
        }
        p.openInventory(inv);
    }

    public static Navigation getNavigationItem(ItemStack item){
        for(Navigation nav : Navigation.values()){
            if(nav.build().equals(item)) return nav;
        }
        return null;
    }
}
