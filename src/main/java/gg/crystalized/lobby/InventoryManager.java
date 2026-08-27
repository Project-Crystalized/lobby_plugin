package gg.crystalized.lobby;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import gg.crystalized.lobby.statistics.StatView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
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
import org.bukkit.scheduler.BukkitRunnable;

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
        if(!Lobby_plugin.getInstance().passive_mode && event.getItem().equals(Cosmetic.getShardcore(p).build(p, true, false, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))))){
            p.openInventory(App.prepareInv("\uA000\uA002", 54, App.useCases.Menu, event.getPlayer()));
            ScrollableView.setView(p, App.useCases.Menu);
            for(int i = 0; i <= 54; i++){
                if(p.getInventory().getItem(i) == null){
                    continue;
                }
                if(Objects.equals(p.getInventory().getItem(i), Cosmetic.getShardcore(p).build(p, true, false, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))))){
                    p.getInventory().setItem(i, Cosmetic.getShardcore(p).build(p, true, true, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))));
                }
            }
            return;
        }
        App app = App.identifyApp(event.getItem(), p);
        if(app == null) return;
        app.action(p, p);
        if(app.self != null) ScrollableView.setView(p, app.self);
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        Player p = (Player) event.getWhoClicked();
        if(Lobby_plugin.getInstance().passive_mode && ScrollableView.getView(p).view == null){
            return;
        }

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
            if(item.getType() == Material.PLAYER_HEAD){
                if(event.getSlot() < 7 && event.getSlot() != 2){
                    FriendsMenu.clickedPartyMember(p, item, event.getClick());
                    return;
                }
                FriendsMenu.clickedFriend(item, p, event.getClick());
                return;
            }

            if(event.getCurrentItem().equals(Cosmetic.getShardcore(p).build(p,true, false, CosmeticView.isViewing(p, Cosmetic.getShardcore(p)))) || event.getCurrentItem().equals(Cosmetic.getShardcore(p).build(p, true, true, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))))){
                event.getWhoClicked().openInventory(App.prepareInv("\uA000\uA002", 54, App.useCases.Menu, (Player)event.getWhoClicked()));
                ScrollableView.setView(p, App.useCases.Menu);
                for(int i = 0; i <= 54; i++){
                    if(p.getInventory().getItem(i) == null){
                        continue;
                    }
                    if(Objects.equals(p.getInventory().getItem(i), Cosmetic.getShardcore(p).build(p, true, false, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))))){
                        p.getInventory().setItem(i, Cosmetic.getShardcore(p).build(p, true, true, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))));
                    }
                }
                return;
            }
        }

        App app = App.identifyApp(item, p);
        if(app != null) {
            if(event.getClick().isShiftClick() && app.extra instanceof Location){
                String name = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey("crystalized", "app"), PersistentDataType.STRING);
                if(name == null || name.equals("")){
                    return;
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
                goBack(ScrollableView.getView(p).view, p);
                return;
            }
            if(app.equals(App.Home)){
                home(p);
                return;
            }
            app.action(p, p);
            if(app.self != null) ScrollableView.setView(p, app.self);
        }else if(Cosmetic.identifyCosmetic(item) != null){
            Cosmetic c = Cosmetic.identifyCosmetic(item);
            c.clicked(event.getClick(), p, event.getSlotType(), event.getSlot(), event.getInventory());
        }else if(Quest.identifyQuest(p, item) != null){
            if(Quest.identifyQuest(p, item).done){
                Quest.identifyQuest(p, item).claim();
                event.getInventory().remove(item);
            }

            if(LobbyDatabase.canRerollQuest(Quest.identifyQuest(p, item))){
                Quest.identifyQuest(p, item).rerollQuest();
                Quest.setQuests(event.getInventory(), p);
            }
        }else if(Achievement.identifyAchievement(p, item) != null){
            Achievement a = Achievement.identifyAchievement(p, item);
            if(a.done && p.getUniqueId().equals(a.player.getUniqueId())){
                a.claim();
                event.getInventory().setItem(event.getSlot(), a.build());
            }
        }else{
            App.useCases use = ScrollableView.getView(p).view;
            if(use == null){
                return;
            }
            for(App a : App.values()){
                if(a.use == use && a.slots != null && isSlotInButton(event.getSlot(), a.slots) && use == App.useCases.Shop){
                    Cosmetic.placeCosmetics(p, a);
                    ScrollableView.setView(p, App.useCases.ShopPage);
                    break;
                }else if(a.use == use && a.slots != null && isSlotInButton(event.getSlot(), a.slots) && use == App.useCases.Wardrobe){
                    CosmeticView.getView(p).getWardrobe(a);
                    ScrollableView.setView(p, App.useCases.WardrobePage);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onInvOpen(InventoryOpenEvent event){
        Player p = (Player) event.getPlayer();
        ScrollableView.getView(p).hasSomethingOpen = true;
        if(Lobby_plugin.getInstance().passive_mode && ScrollableView.getView(p).view == null){
            return;
        }
        for(int i = 0; i <= 54; i++){
            if(p.getInventory().getItem(i) == null){
                continue;
            }
            if(Objects.equals(p.getInventory().getItem(i), Cosmetic.getShardcore(p).build(p, true, false, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))))){
                p.getInventory().setItem(i, Cosmetic.getShardcore(p).build(p, true, true, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))));
            }
        }
        App.useCases use = ScrollableView.getView(p).view;
        if(use == null){
            return;
        }
        for(App a : App.values()){
            if(a.use == use && a.slots != null){
                fillButtons(a.slots, event.getInventory(), a.name);
            }
        }
    }

    @EventHandler
    public void onInvClose(InventoryCloseEvent event){
        Player p = (Player)event.getPlayer();
        ScrollableView.getView(p).hasSomethingOpen = false;
        ScrollableView.getView(p).view = null;
        if(Lobby_plugin.getInstance().passive_mode && ScrollableView.getView(p).view == null){
            return;
        }
        for(int i = 0; i <= 54; i++){
            if(p.getInventory().getItem(i) == null){
                continue;
            }
            if(Objects.equals(p.getInventory().getItem(i), Cosmetic.getShardcore(p).build(p, true, true, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))))){
                p.getInventory().setItem(i, Cosmetic.getShardcore(p).build(p, true, false, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))));
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

    public static void giveLobbyItems(Player p){
        if (Lobby_plugin.getInstance().passive_mode) {
            return;
            // if this method activates when you get an achievement (which will happen because of the flashing shardcore), you will be given
            // the lobby items during gameplay, returning here to fix that bug. - Callum
        }
        Inventory i = p.getInventory();
        int in = 0;
        for(App a : App.values()){
            if(a.uses == null){
                continue;
            }
            if(Arrays.asList(a.uses).contains(App.useCases.Hotbar)) {
                i.setItem(in, a.build(p));
                in++;
            }
        }
        p.getInventory().setItem(4, Cosmetic.getShardcore(p).build(p, true, false, CosmeticView.isViewing(p, Cosmetic.getShardcore(p))));
        if(Ranks.getPayRank(p) == Ranks.sun_sub.ordinal()){
            p.getInventory().setItem(8, App.ToggleFlight_true.build());
            p.getInventory().setItem(7, App.ToggleAbility_true.build());
            return;
        }
        if(Ranks.getPayRank(p) == Ranks.moon_one.ordinal()){
            p.getInventory().setItem(8, App.ToggleAbility_true.build());
        }
    }

    //TODO this needs testing v
    public static void doScrolling(App a, Player p, InventoryView view){
        App.useCases use = ScrollableView.getView(p).view;

        if(a == App.ScrollRight) {
            ScrollableView.getView(p).page++;
        }else if(ScrollableView.getView(p).page != 0) {
            ScrollableView.getView(p).page--;
        }

        if(use == App.useCases.ShopPage){
            Cosmetic.placeCosmetics(p, Cosmetic.getButton(view));
        }else if(use == App.useCases.WardrobePage){
            CosmeticView.getView(p).getWardrobe(Cosmetic.getButton(view));
        }else if(use == App.useCases.Friends){
            App.Friends.action(p, p);
        }
        ScrollableView.getView(p).view = use;
    }

    public static void goBack(App.useCases use, Player p){
        if(use == App.useCases.ShopPage) {
            App.Shop.action(p, p);
            ScrollableView.setView(p, App.useCases.Shop);
        }else if (use == App.useCases.WardrobePage){
            App.Wardrobe.action(p, p);
            ScrollableView.setView(p, App.useCases.Wardrobe);
        } else if (use == App.useCases.AchievementsPage) {
            App.Achieve.action(p, p); //unsure about this - Callum
            ScrollableView.setView(p, App.useCases.Achievements);
        }
    }

    public static void home(Player p){
        p.openInventory(App.prepareInv("\uA000\uA002", 54, App.useCases.Menu, p));
    }

    public static boolean hasLobbyItems(Player p){
        for(App a : App.values()){
            if(a.use == null || a.use != App.useCases.Hotbar){
                continue;
            }
            if(a.uses == null || !Arrays.asList(a.uses).contains(App.useCases.Hotbar)){
                continue;
            }
            if(p.getInventory().contains(a.build(p))){
                return true;
            }
        }
        return false;
    }
}

class ScrollableView{
    //this class takes note of all lobby inventory types not just scrollables
    public static ArrayList<ScrollableView> views = new ArrayList<>();
    Player viewer;
    int page = 0;
    App.useCases view;
    boolean hasSomethingOpen = false;

    public ScrollableView(Player viewer) {
        this.viewer = viewer;
        views.add(this);
    }

    public static ScrollableView getView(Player p){
        //gets the player's view or generates a new one if there is none
        ScrollableView view = null;
        for(ScrollableView s : views){
            if(s.viewer.equals(p)){
                view = s;
                break;
            }
        }
        if(view == null){
            return new ScrollableView(p);
        }
        return view;
    }

    public static void setView(Player p, App.useCases use){
        ScrollableView view = getView(p);
        view.view = use;
        view.page = 0;
    }

    public static void removeView(Player p){
        ScrollableView view = null;
        for(ScrollableView s : views){
            if(s.viewer.equals(p)){
                view = s;
                break;
            }
        }
        views.remove(view);
    }
}