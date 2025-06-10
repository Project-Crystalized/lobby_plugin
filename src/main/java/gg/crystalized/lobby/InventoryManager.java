package gg.crystalized.lobby;

import net.kyori.adventure.text.Component;
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

import java.util.ArrayList;

import static net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public class InventoryManager implements Listener {
    static final String[] shardcores = {
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
        Player p = event.getPlayer();
        if(event.getItem().equals(buildShardcore(p))){
            p.openInventory(App.prepareInv("\uA000\uA002", 54, App.useCases.Menu, p));
            return;
        }
        if(App.identifyApp(event.getItem()) == null) return;
        App.identifyApp(event.getItem()).op.action(p);
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        event.setCancelled(true);
        if(event.getCurrentItem() == null) return;
        ItemStack item = event.getCurrentItem();
        App app = App.identifyApp(item);
        Player p = (Player) event.getWhoClicked();
        if(event.getCurrentItem().equals(buildShardcore(p))){
            event.getWhoClicked().openInventory(App.prepareInv("\uA000\uA002", 54, App.useCases.Menu, p));
            return;
        }
        if(app != null) {
            app.op.action((Player) event.getWhoClicked());
        }else if(Cosmetic.identifyCosmetic(item) != null){
            event.getInventory().setItem(event.getSlot(), Cosmetic.identifyCosmetic(item).build(!Cosmetic.identifyCosmetic(item).isWearing(p)));
            Cosmetic.identifyCosmetic(item).clicked(event.getClick(), p);
        }
    }

    public static void giveLobbyItems(Player p){
        Inventory i = p.getInventory();
        for(App a : App.values()){
            if(a.uses == App.useCases.Hotbar) {
                i.setItem(a.slot, a.build());
            }
        }
        p.getInventory().setItem(4, buildShardcore(p));
    }

    public static ItemStack buildShardcore(Player p){
        int id = (Integer) LobbyDatabase.fetchPlayerData(p).get("shardcore_id");
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Menu").color(LIGHT_PURPLE).decoration(BOLD, true));
        meta.setItemModel(new NamespacedKey("crystalized",shardcores[id]));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack buildShardcore(int id, Boolean wearing){
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Shardcore: " +id).color(WHITE).decoration(ITALIC, false));
        ArrayList<Component> lore = new ArrayList<>();
        if(wearing == null){
            lore.add(Component.text("[Right-click] price: 200").color(WHITE).decoration(ITALIC, false));
        }else if(wearing){
            lore.add(Component.text(""));
        }else{
            lore.add(Component.text("[Right-click] equip").color(WHITE).decoration(ITALIC, false));
        }
        meta.lore(lore);
        meta.setItemModel(new NamespacedKey("crystalized",shardcores[id]));
        item.setItemMeta(meta);
        return item;
    }

    public static boolean ownsShardcore(int id, Player p){
        ArrayList<Object[]> list = LobbyDatabase.fetchShardcores(p);
        boolean own = false;
        for(Object[] o : list){
            if((Integer)o[1] == id){
                own = true;
                break;
            }
        }
        return own;
    }

    public static Integer placeOnRightSlot(int iterator, int end, int line, int borderrw, int borderlw){
        int[] border = {8, 17, 26, 35, 44, 53};
        int[] nextLine = {2, 11, 20, 29, 38, 47};
        int fin = iterator + nextLine[line] + borderlw;
        boolean isInBorder = false;

        for(int i : border){
            if(fin <= i && fin >= i - borderrw){
                isInBorder = true;
                break;
            }
        }

        boolean isOnNextLine = false;
        if(!isInBorder) return fin;

        if(fin == end){
            return fin;
        }

        while(!isOnNextLine){
            fin++;
            for(int i : nextLine){
                if(fin == i + borderlw){
                    isOnNextLine = true;
                    break;
                }
            }
        }

        if(fin < end){
            return null;
        }

        return fin;
    }
}
