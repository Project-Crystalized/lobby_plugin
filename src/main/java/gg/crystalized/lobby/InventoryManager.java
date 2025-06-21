package gg.crystalized.lobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;

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
            p.openInventory(App.prepareInv("\uA000\uA002", 54, App.useCases.Menu));
            return;
        }
        if(App.identifyApp(event.getItem()) == null) return;
        App.identifyApp(event.getItem()).action(p);
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if(item == null){
            App app = null;
            for(App a : App.values()){
                if(a.slot != null && event.getSlot() == a.slot && a.self == null){
                    app = a;
                    break;
                }
            }
            return;
        }
        Player p = (Player) event.getWhoClicked();
        App app = App.identifyApp(item);
        if(event.getCurrentItem().equals(buildShardcore(p))){
            event.getWhoClicked().openInventory(App.prepareInv("\uA000\uA002", 54, App.useCases.Menu));
            return;
        }
        if(app != null) {
            app.action((Player) event.getWhoClicked());
        }else if(Cosmetic.identifyCosmetic(item) != null){
            event.getInventory().setItem(event.getSlot(), Cosmetic.identifyCosmetic(item).build(!Cosmetic.identifyCosmetic(item).isWearing(p)));
            Cosmetic.identifyCosmetic(item).clicked(event.getClick(), p);
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
    //TODO open shardcore when opening inv

    @EventHandler
    public void onInvOpen(InventoryOpenEvent event){
        App.useCases use = identifyInv(event.getView());
        if(use == null){
            return;
        }
        for(App a : App.values()){
            if(a.use == use && a.slots != null){
                fillButtons(a.slots, event.getInventory(), a.name);
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

    //TODO this needs testing v
    public static void doScrolling(App a, Player p, InventoryView view){
        App.useCases use = identifyInv(view);
        if(use == App.useCases.ShopPage){
            if(view.getTopInventory().getItem(InventoryManager.placeOnRightSlot(0, 51, 3, 1, 0)) == null || view.getTopInventory().getItem(InventoryManager.placeOnRightSlot(15, 51, 3, 1, 0)) == null){
                return;
            }
            EquipmentSlot slot = Cosmetic.identifyCosmetic(view.getTopInventory().getItem(InventoryManager.placeOnRightSlot(0, 51, 3, 1, 0))).slot;
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
                Cosmetic.placeCosmetics(p, a, page++);
            }else if(page != 1){
                Cosmetic.placeCosmetics(p, a, page--);
            }
        }
    }
}
