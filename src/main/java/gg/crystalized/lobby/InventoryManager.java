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

import static net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class InventoryManager implements Listener {
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

    public static Integer placeOnRightSlot(int iterator, int end){
        int[] border = {7,8, 16, 17, 26, 26, 34, 35, 43, 44, 52, 53};
        int[] nextLine = {2, 11, 20, 29, 38, 47};
        int fin = iterator + 2;
        boolean isInBorder = false;

        for(int i : border){
            if(iterator + 2 == i){
                isInBorder = true;
                break;
            }
        }

        boolean isOnNextLine = false;
        if(!isInBorder) return fin;
        while(!isOnNextLine){
            fin++;
            for(int i : nextLine){
                if(iterator + 2 == i){
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
