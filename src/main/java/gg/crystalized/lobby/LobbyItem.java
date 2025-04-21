package gg.crystalized.lobby;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class LobbyItem {
    private static final ArrayList<LobbyItem> LOBBY_ITEMS = buildLobbyItems();
    private static final String[] shardcores = {
            "shardcorenexus3/blue",
            "shardcorenexus3/gray",
            "shardcorenexus3/purple",
            "shardcorenexus3/augustify",
            "shardcorenexus3/abby1",
            "shardcorenexus3/shadow1"};

    ItemStack stack;
    int slot;

    public LobbyItem(ItemStack stack, int slot){
        this.stack = stack;
        this.slot = slot;
    }
    public static ArrayList<LobbyItem> buildLobbyItems(){
        ArrayList<LobbyItem> items = new ArrayList<>();

        ItemStack navigator = new ItemStack(Material.COMPASS);
        ItemMeta naviMeta = navigator.getItemMeta();
        naviMeta.displayName(Component.text("Play").color(GREEN).decoration(BOLD, true));
        navigator.setItemMeta(naviMeta);
        items.add(new LobbyItem(navigator, 0));

        //TODO add more here
        return items;
    }

    public static void giveLobbyItems(Player p){
        Inventory i = p.getInventory();
        for(LobbyItem l : LOBBY_ITEMS){
            i.setItem(l.slot, l.stack);
        }
        buildShardcore(p);
    }

    public static void buildShardcore(Player p){
        int id = (Integer) LobbyDatabase.fetchPlayerData(p).get("shardcore_id");
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Menu").color(LIGHT_PURPLE).decoration(BOLD, true));
        meta.setItemModel(new NamespacedKey("crystalized",shardcores[id]));
        item.setItemMeta(meta);
        p.getInventory().setItem(4, item);
    }
}
