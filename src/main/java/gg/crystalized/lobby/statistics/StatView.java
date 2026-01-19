package gg.crystalized.lobby.statistics;

import gg.crystalized.lobby.App;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;

public class StatView implements Listener {
    static ArrayList<StatView> views = new ArrayList<>();
    int page = -1;
    String alias;
    Player viewer;
    OfflinePlayer stats;
    boolean isInView = false;

    public StatView(Player viewer, String alias){
        this.viewer = viewer;
        this.alias = alias;
    }

    public static StatView create(Player viewer, String alias){
        StatView view = new StatView(viewer, alias);
        return view;
    }
    public void startPlayerView(Inventory profile){
        Inventory inv = Bukkit.createInventory(viewer, 54, "\uA000\uA006");
        OfflinePlayer p = Bukkit.getOfflinePlayer(profile.getItem(2).getPersistentDataContainer().get(new NamespacedKey("crystalized", "profile_holder"), PersistentDataType.STRING));
        stats = p;
        boolean isLifetime = page == -1;
        ArrayList<StatItem> stats = getForGame(alias, p, isLifetime, page);
        int[] border = {8, 17, 26, 35, 44, 53};
        int[] nextLine = {2, 11, 20, 29, 38, 47};
        int slot = 29;
        int line = 3;
        for(StatItem s : stats){
           if(slot >= border[line]){
               line++;
               slot = nextLine[line];
           }
           if(slot != 49){
               inv.setItem(slot, s.item);
           }
           slot = slot + 2;
            inv.setItem(slot, s.item);
        }
        if(!isLifetime){
            inv.setItem(48, App.ProfileScrollLeft.build());
        }
        inv.setItem(50, App.ProfileScrollRight.build());
        viewer.openInventory(inv);
        isInView = true;
        views.add(this);
    }

    public void restartPlayerView(){
        Inventory inv = Bukkit.createInventory(viewer, 54, "\uA000\uA006");
        boolean isLifetime = page == -1;
        ArrayList<StatItem> stats = getForGame(alias, this.stats, isLifetime, page);
        int[] border = {8, 17, 26, 35, 44, 53};
        int[] nextLine = {2, 11, 20, 29, 38, 47};
        int slot = 29;
        int line = 3;
        for(StatItem s : stats){
            if(slot >= border[line]){
                line++;
                slot = nextLine[line];
            }
            inv.setItem(slot, s.item);
            slot = slot + 2;
        }
        if(!isLifetime){
            inv.setItem(48, App.ProfileScrollLeft.build());
        }
        inv.setItem(50, App.ProfileScrollRight.build());
        viewer.openInventory(inv);
        isInView = true;
        views.add(this);
    }

    public void stopView(){
        views.remove(this);
        isInView = false;
    }

    public static StatView getView(Player p){
        for(StatView view : views){
            if(view.viewer == null){
                continue;
            }
            if(view.viewer.equals(p)){
                return view;
            }
        }
        return null;
    }

    public static ArrayList<StatItem> getForGame(String alias, OfflinePlayer p, boolean isLifetime, int page){
        return (ArrayList<StatItem>) GameDistributor.distribute(GameDistributor.types.getForGame, alias, p, isLifetime, page);
    }

    public static Integer getTotal(String alias, OfflinePlayer p){
        return (Integer) GameDistributor.distribute(GameDistributor.types.getTotal, alias, p, false, -1);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        Player p = (Player)e.getWhoClicked();
        StatView view = getView(p);
        if(view == null){
            return;
        }
        int games = getTotal(view.alias, view.stats);
        if(!view.isInView){
            return;
        }
        if(e.getCurrentItem() == null){
            return;
        }
        if(e.getCurrentItem().equals(App.ProfileScrollRight.build())){
            if(!(view.page >= games)) {
                view.page++;
            }
            view.restartPlayerView();
        }else if(e.getCurrentItem().equals(App.ProfileScrollLeft.build())){
            if (!(view.page <= -1)) {
                view.page--;
            }
            view.restartPlayerView();
        }
    }
}
