package gg.crystalized.lobby;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import gg.crystalized.lobby.statistics.StatView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
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
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;


public class InventoryManager implements Listener {

    @EventHandler
    public void onRightClick(PlayerInteractEvent event){
        if (!event.getAction().isRightClick()) {
            return;
        }
        if(event.getItem() == null) return;
        Player p = event.getPlayer();
        if(!Lobby_plugin.getInstance().passive_mode && event.getItem().equals(Cosmetic.getShardcore(p).build(true, false, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))))){
            p.openInventory(App.prepareInv("\uA000\uA002", 54, App.useCases.Menu, event.getPlayer()));
            for(int i = 0; i <= 54; i++){
                if(p.getInventory().getItem(i) == null){
                    continue;
                }
                if(Objects.equals(p.getInventory().getItem(i), Cosmetic.getShardcore(p).build(true, false, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))))){
                    p.getInventory().setItem(i, Cosmetic.getShardcore(p).build(true, true, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))));
                }
            }
            return;
        }
        if(App.identifyApp(event.getItem()) == null) return;
        App.identifyApp(event.getItem()).action(p);
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        if(Lobby_plugin.getInstance().passive_mode && !isLobbyInv(event.getView())){
            return;
        }
        Player p = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        event.setCancelled(true);

        if(item == null){
            p.playSound(p, "crystalized:effect.scn3.click_basic", 1, 1);
            /*
            App app = null;
            for(App a : App.values()){
                if(a.slot != null && event.getSlot() == a.slot && a.self == null){
                    app = a;
                    break;
                }
            }
            if(app == null) return;
             */

        } else {
            p.playSound(p, "crystalized:effect.scn3.click", 1, 1); //assuming the item we have we're supposed to click on - Callum
            if(item.getType() == Material.PLAYER_HEAD && identifyInv(event.getView()) == App.useCases.Friends){
                if(event.getSlot() < 7){
                    FriendsMenu.clickedPartyMember(p, item, event.getClick());
                    return;
                }
                FriendsMenu.clickedFriend(item, p, event.getClick());
                return;
            }

            if(event.getCurrentItem().equals(Cosmetic.getShardcore(p).build(true, false, CosmeticView.isViewing(p, Cosmetic.getShardcore(p)))) || event.getCurrentItem().equals(Cosmetic.getShardcore(p).build(true, true, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))))){
                event.getWhoClicked().openInventory(App.prepareInv("\uA000\uA002", 54, App.useCases.Menu, (Player)event.getWhoClicked()));
                for(int i = 0; i <= 54; i++){
                    if(p.getInventory().getItem(i) == null){
                        continue;
                    }
                    if(Objects.equals(p.getInventory().getItem(i), Cosmetic.getShardcore(p).build(true, false, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))))){
                        p.getInventory().setItem(i, Cosmetic.getShardcore(p).build(true, true, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))));
                    }
                }
                return;
            }
        }

        App app = App.identifyApp(item);
        if(app != null) {
            if(event.getClick().isShiftClick() && app.extra instanceof Location){
                String name = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey("crystalized", "app"), PersistentDataType.STRING);
                if(name == null || name.equals("")){
                    return; //TODO I feel like this will one day cause problems
                }
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Connect");
                out.writeUTF(name);
                out.writeUTF("true");
                p.sendPluginMessage(Lobby_plugin.getInstance(), "crystalized:main", out.toByteArray());
                return;
            }
            if(app.self == App.useCases.Set){
                Setting.changeSettings(app, p, event.getSlot());
                return;
            }
            if(app.toString().toLowerCase().contains("scroll")) {
                doScrolling(app, p, event.getView());
                return;
            }
            if(app.equals(App.Back)){
                goBack(identifyInv(event.getView()), p);
                return;
            }
            if(app.equals(App.Home)){
                home(p);
                return;
            }
            app.action((Player) event.getWhoClicked());
        }else if(Cosmetic.identifyCosmetic(item) != null){
            Cosmetic c = Cosmetic.identifyCosmetic(item);
            c.clicked(event.getClick(), p, event.getSlotType(), event.getSlot(), event.getInventory());
        }else{
            App.useCases use = identifyInv(event.getView());
            if(use == null){
                return;
            }
            for(App a : App.values()){
                if(a.use == use && a.slots != null && isSlotInButton(event.getSlot(), a.slots) && use == App.useCases.Shop){
                    Cosmetic.placeCosmetics(p, a, 1);
                    break;
                }else if(a.use == use && a.slots != null && isSlotInButton(event.getSlot(), a.slots) && use == App.useCases.Wardrobe){
                    p.openInventory(CosmeticView.getView(p).getWardrobe(a, 1));
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onInvOpen(InventoryOpenEvent event){
        if(Lobby_plugin.getInstance().passive_mode && !isLobbyInv(event.getView())){
            return;
        }
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
            if(Objects.equals(p.getInventory().getItem(i), Cosmetic.getShardcore(p).build(true, false, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))))){
                p.getInventory().setItem(i, Cosmetic.getShardcore(p).build(true, true, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))));
            }
        }
    }

    @EventHandler
    public void onInvClose(InventoryCloseEvent event){
        if(Lobby_plugin.getInstance().passive_mode && !isLobbyInv(event.getView())){
            return;
        }
        Player p = (Player)event.getPlayer();
        for(int i = 0; i <= 54; i++){
            if(p.getInventory().getItem(i) == null){
                continue;
            }
            if(Objects.equals(p.getInventory().getItem(i), Cosmetic.getShardcore(p).build(true, true, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))))){
                p.getInventory().setItem(i, Cosmetic.getShardcore(p).build(true, false, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))));
            }
        }
        StatView view = StatView.getView((Player) event.getPlayer());
        if(view != null){
            view.stopView();
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
        try {
            for (App a : App.values()) {
                if (inv.title().equals(Component.text("\uA000\uA00A").color(WHITE))) {
                    return App.useCases.ShopPage;
                } else if (inv.title().equals(Component.text("\uA000\uA010").color(WHITE))) {
                    return App.useCases.WardrobePage;
                }
                if (!(a.extra instanceof String)) {
                    continue;
                }
                if (((TextComponent) inv.title()).content().equals(a.extra)) {
                    return a.self;
                }
            }
        }catch(ClassCastException e){}
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
        p.getInventory().setItem(4, Cosmetic.getShardcore(p).build(true, false, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))));
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
        if(use == App.useCases.ShopPage || use == App.useCases.WardrobePage){
            if(view.getTopInventory().getItem(InventoryManager.placeOnRightSlot(0, 51, 4, 1, 0)) == null || view.getTopInventory().getItem(InventoryManager.placeOnRightSlot(15, 51, 3, 1, 0)) == null){
                return;
            }
            EquipmentSlot slot = Cosmetic.identifyCosmetic(view.getTopInventory().getItem(InventoryManager.placeOnRightSlot(0, 51, 4, 1, 0))).slot;
            if(slot == null){
                return;
            }
            int i = 1;
            for(Cosmetic c : Cosmetic.cosmetics){
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
        if(use == App.useCases.ShopPage) {
            App.Shop.action(p);
        }else if (use == App.useCases.WardrobePage){
            App.Wardrobe.action(p);
        }
    }

    public static void home(Player p){
        p.openInventory(App.prepareInv("\uA000\uA002", 54, App.useCases.Menu, p));
    }

    public static boolean isLobbyInv(InventoryView view){
        for(App a : App.values()){
            if(a.extra instanceof String && view.title() instanceof TextComponent && ((TextComponent) view.title()).content() == a.extra){
                return true;
            }
        }

        if(!(view.title() instanceof TextComponent)){
            return false;
        }

        if(((TextComponent) view.title()).content().equals("\uA000\uA002") || ((TextComponent) view.title()).content().equals("\uA000\uA00A")){
            return true;
        }
        return false;
    }

    public static boolean hasLobbyItems(Player p){
        for(App a : App.values()){
            if(a.use == null || a.use != App.useCases.Hotbar){
                continue;
            }
            if(a.uses == null || !Arrays.asList(a.uses).contains(App.useCases.Hotbar)){
                continue;
            }
            if(p.getInventory().contains(a.build())){
                return true;
            }
        }
        return false;
    }
}
