package gg.crystalized.lobby;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class InventoryManager implements Listener {
    private static final App[] LOBBY_ITEMS = {App.Navigator_Hotbar};
    private static final String[] shardcores = {
            "shardcorenexus3/blue",
            "shardcorenexus3/gray",
            "shardcorenexus3/purple",
            "shardcorenexus3/augustify",
            "shardcorenexus3/abby1",
            "shardcorenexus3/shadow1"};

    @EventHandler
    public void onRightClick(PlayerInteractEvent event){
        if (!event.getAction().isRightClick()) {
            return;
        }
        if(event.getItem() == null) return;
        event.getPlayer().openInventory(getView(event.getMaterial()));
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        event.setCancelled(true);
        if(event.getCurrentItem() == null) return;
        ItemStack item = event.getCurrentItem();
        App app = null;
        for(App a : App.values()){
            if(item.equals(a.build())){
                app = a;
                break;
            }
        }
        if(app == null) return;
        //TODO add file reader
    }

    public Inventory getView(Material m){
        Inventory inv = null;
        App.useCases use = null;
        if(m == Material.EMERALD){
            inv = Bukkit.getServer().createInventory(null, 54, "\uA000\uA002");
            use = App.useCases.Menu;
        }else if(m == Material.COMPASS){
            inv = Bukkit.getServer().createInventory(null, 54, "\uA000\uA006");
            use = App.useCases.Games;
        }
        if(inv == null){
            return null;
        }
        App.buildApps(inv, use);
        return inv;
    }

    public static void giveLobbyItems(Player p){
        Inventory i = p.getInventory();
        for(App a : LOBBY_ITEMS){
            i.setItem(a.slot, a.build());
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
