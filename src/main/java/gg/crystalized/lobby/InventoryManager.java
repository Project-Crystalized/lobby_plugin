package gg.crystalized.lobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;


public class InventoryManager implements Listener {

    @EventHandler
    public void onRightClick(PlayerInteractEvent event){
        if (!event.getAction().isRightClick()) {
            return;
        }
        if(event.getItem() == null) return;
        Player p = event.getPlayer();
        if(event.getItem().equals(Cosmetic.getShardcore(p).build(true, false))){
            p.openInventory(App.prepareInv("\uA000\uA002", 54, App.useCases.Menu));
            for(int i = 0; i <= 54; i++){
                if(p.getInventory().getItem(i) == null){
                    continue;
                }
                if(Objects.equals(p.getInventory().getItem(i), Cosmetic.getShardcore(p).build(true, false))){
                    p.getInventory().setItem(i, Cosmetic.getShardcore(p).build(true, true));
                }
            }
            return;
        }
        if(App.identifyApp(event.getItem()) == null) return;
        App.identifyApp(event.getItem()).action(p);
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        Player p = (Player) event.getWhoClicked();
        if(item == null){
            App app = null;
            for(App a : App.values()){
                if(a.slot != null && event.getSlot() == a.slot && a.self == null){
                    app = a;
                    break;
                }
            }
            if(app == null) return;
            if(app.slot != 20) {
                doScrolling(app, p, event.getView());
                return;
            }
            goBack(identifyInv(event.getView()), p);
            return;
        }
        App app = App.identifyApp(item);
        if(event.getCurrentItem().equals(Cosmetic.getShardcore(p).build(true, false)) || event.getCurrentItem().equals(Cosmetic.getShardcore(p).build(true, true))){
            event.getWhoClicked().openInventory(App.prepareInv("\uA000\uA002", 54, App.useCases.Menu));
            for(int i = 0; i <= 54; i++){
                if(p.getInventory().getItem(i) == null){
                    continue;
                }
                if(Objects.equals(p.getInventory().getItem(i), Cosmetic.getShardcore(p).build(true, false))){
                    p.getInventory().setItem(i, Cosmetic.getShardcore(p).build(true, true));
                }
            }
            return;
        }

        if(app != null) {
            app.action((Player) event.getWhoClicked());
        }else if(Cosmetic.identifyCosmetic(item) != null){
            event.getInventory().setItem(event.getSlot(), Cosmetic.identifyCosmetic(item).build(!Cosmetic.identifyCosmetic(item).isWearing(p), false));
            Cosmetic.identifyCosmetic(item).clicked(event.getClick(), p, event.getView()); //TODO
        }else{
            App.useCases use = identifyInv(event.getView());
            if(use == null){
                return;
            }
            for(App a : App.values()){
                if(a.use == use && a.slots != null && isSlotInButton(event.getSlot(), a.slots)){
                    Cosmetic.placeCosmetics(p, a, 1);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onInvOpen(InventoryOpenEvent event){
        App.useCases use = identifyInv(event.getView());
        Player p = (Player) event.getPlayer();
        if(use == null){
            return;
        }
        for(App a : App.values()){
            if(a.use == use && a.slots != null){
                fillButtons(a.slots, event.getInventory(), a.name);
            }
        }
        for(int i = 0; i <= 54; i++){
            if(p.getInventory().getItem(i) == null){
                continue;
            }
            if(Objects.equals(p.getInventory().getItem(i), Cosmetic.getShardcore(p).build(true, false))){
                p.getInventory().setItem(i, Cosmetic.getShardcore(p).build(true, true));
            }
        }
    }

    @EventHandler
    public void onInvClose(InventoryCloseEvent event){
        Player p = (Player)event.getPlayer();
        for(int i = 0; i <= 54; i++){
            if(p.getInventory().getItem(i) == null){
                continue;
            }
            if(Objects.equals(p.getInventory().getItem(i), Cosmetic.getShardcore(p).build(true, true))){
                p.getInventory().setItem(i, Cosmetic.getShardcore(p).build(true, false));
            }
        }
    }
    public static boolean isSlotInButton(int slot, int[] button){
        for(int i = button[0]; i <= button[0] + button[1] - 1; i++){
            for(int j = i; j <= i + 9*button[2] -1; j = j+9){
                if(slot == j){
                    return true;
                }
            }
        }
        return false;
    }

    public static void fillButtons(int[] button, Inventory inv, Component name){
        ItemStack item = new ItemStack(Material.COAL);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(new NamespacedKey("crystalized", "ui/invisible"));
        meta.itemName(name);
        item.setItemMeta(meta);
        for(int i = button[0]; i <= button[0] + button[1] - 1; i++){
            for(int j = i; j <= i + 9*(button[2]-1); j = j+9){
                inv.setItem(j, item);
            }
        }
    }

    public static App.useCases identifyInv(InventoryView inv){
        for(App a : App.values()){
            if(((TextComponent) inv.title()).content().equals("\uA000\uA00A")){
                return App.useCases.ShopPage;
            }
            if(!(a.extra instanceof String)){
                continue;
            }
            if(((TextComponent)inv.title()).content().equals(a.extra)){
                return a.self;
            }
        }
        return null;
    }
    public static void giveLobbyItems(Player p){
        Inventory i = p.getInventory();
        int in = 0;
        for(App a : App.values()){
            if(a.uses == null){
                continue;
            }
            if(Arrays.asList(a.uses).contains(App.useCases.Hotbar)) {
                i.setItem(in, a.build());
                in++;
            }
        }
        p.getInventory().setItem(4, Cosmetic.getShardcore(p).build(true, false));
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

    //TODO this needs testing v
    public static void doScrolling(App a, Player p, InventoryView view){
        App.useCases use = identifyInv(view);
        if(use == App.useCases.ShopPage){
            if(view.getTopInventory().getItem(InventoryManager.placeOnRightSlot(0, 51, 4, 1, 0)) == null || view.getTopInventory().getItem(InventoryManager.placeOnRightSlot(15, 51, 3, 1, 0)) == null){
                return;
            }
            EquipmentSlot slot = Cosmetic.identifyCosmetic(view.getTopInventory().getItem(InventoryManager.placeOnRightSlot(0, 51, 4, 1, 0))).slot;
            if(slot == null){
                return;
            }
            int i = 1;
            for(Cosmetic c : Cosmetic.values()){
                if(Cosmetic.identifyCosmetic(view.getTopInventory().getItem(InventoryManager.placeOnRightSlot(15, 51, 3, 1, 0))).equals(c)){
                    break;
                }
                if(c.slot == slot){
                    i++;
                }
            }

            int page = i/15;
            if(a == App.ScrollRight) {
                Cosmetic.placeCosmetics(p, a, page+1);
            }else if(page != 1){
                Cosmetic.placeCosmetics(p, a, page-1);
            }
        }
    }

    public static void goBack(App.useCases use, Player p){
        if(use == App.useCases.ShopPage){
            App.Shop.action(p);
        }
    }
}
