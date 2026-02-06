package gg.crystalized.lobby.statistics;

import gg.crystalized.lobby.App;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

public class StatView implements Listener {
    static ArrayList<StatView> views = new ArrayList<>();
    int page = -1;
    String alias;
    Player viewer;
    OfflinePlayer stats;
    boolean isInView = false;
    boolean isGameView = false;
    boolean onSide = false;

    public StatView(Player viewer, String alias){
        this.viewer = viewer;
        this.alias = alias;
    }

    public static StatView create(Player viewer, String alias){
        StatView view = new StatView(viewer, alias);
        return view;
    }
    public void startPlayerView(ItemStack player){
        Inventory inv = Bukkit.createInventory(viewer, 54, Component.text("\uA000\uA006").color(WHITE));
        OfflinePlayer p = Bukkit.getOfflinePlayer(player.getPersistentDataContainer().get(new NamespacedKey("crystalized", "profile_holder"), PersistentDataType.STRING));
        stats = p;
        boolean isLifetime = page == -1;
        Statistics sta = Statistics.stats.get(alias);
        ArrayList<StatItem> stats;
        if(isGameView){
            stats = sta.getPlayerStats(alias, p, sta.getGameId(page), isLifetime);
        }else {
            stats = sta.getPlayerStats(alias, p, sta.getGameId(page, p), isLifetime);
        }
        int[] border = {8, 17, 26, 35, 44, 53};
        int[] nextLine = {2, 11, 20, 29, 38, 47};
        int slot = 29;
        int line = 3;
        for(StatItem s : stats){
           if(slot >= border[line]){
               line++;
               slot = nextLine[line];
           }
           if(slot == 49){
               slot = slot + 2;
           }

           inv.setItem(slot, s.item);
           slot = slot + 2;
        }
        if(!isLifetime){
            inv.setItem(48, App.ProfileScrollLeft.build());
        }
        if(!isGameView) {
            inv.setItem(50, App.ProfileScrollRight.build());
        }
        viewer.openInventory(inv);
        isInView = true;
        views.add(this);
    }

    public void restartPlayerView(){
        Inventory inv = Bukkit.createInventory(viewer, 54, Component.text("\uA000\uA006").color(WHITE));
        boolean isLifetime = page == -1;
        Statistics sta = Statistics.stats.get(alias);
        ArrayList<StatItem> stats = sta.getPlayerStats(alias, this.stats, sta.getGameId(page, this.stats), isLifetime);
        int[] border = {8, 17, 26, 35, 44, 53};
        int[] nextLine = {2, 11, 20, 29, 38, 47};
        int slot = 29;
        int line = 3;
        for(StatItem s : stats){
            if(slot >= border[line]){
                line++;
                slot = nextLine[line];
            }
            if(slot == 49){
                slot = slot + 2;
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

    public void startGameView(boolean first){
        isGameView = true;
        onSide = false;
        if(first){
            page = 0;
        }
        Inventory inv = Bukkit.createInventory(viewer, 54, Component.text("\uA000\uA006").color(WHITE));
        Statistics sta = Statistics.stats.get(alias);
        ArrayList<PlayerItem> stats = sta.getGameStats(page);
        int teamNumber = getTeamNumber(stats);
        ArrayList<ArrayList<PlayerItem>> teams = separateItems(stats, teamNumber);
        int[] middle = {4, 13, 22, 31, 40, 49};
        boolean isSoloOrDuo = isSoloOrDuoTeamed(teams);
        inv.setItem(4, teams.getFirst().getFirst().item);
        int i = 1;
        int slot = 0;
        for(ArrayList<PlayerItem> list : teams){
            if((i == 1 && !(list.size() > 7)) || (isSoloOrDuo && teams.size() > 20)){
                i++;
                slot = 0;
                continue;
            }
            for(PlayerItem item : list){
                inv.setItem(middle[i] + slot, item.item);
                if(slot == Math.floor((double) list.size() /2) || slot == -Math.floor((double) list.size() /2)){
                    slot = 0;
                    i++;
                }
                slot++;
            }
            if(!isSoloOrDuo){
                i++;
                slot = 0;
            }
        }
        if(page != 0) {
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

    public static int getTeamNumber(ArrayList<PlayerItem> items){
        int teamNumber = 1;
        for(PlayerItem i : items){
            if(i.team > teamNumber){
                teamNumber = i.team;
            }
        }
        return teamNumber;
    }

    public static ArrayList<ArrayList<PlayerItem>> separateItems(ArrayList<PlayerItem> items, int teamnumber){
        ArrayList<ArrayList<PlayerItem>> list = new ArrayList<>();
        for(int i = 0; i <= teamnumber +1; i++ ){
            list.add(new ArrayList<>());
        }
        for(PlayerItem i : items){
            list.get(i.team).add(i);
        }
        return list;
    }

    public static boolean isSoloOrDuoTeamed(ArrayList<ArrayList<PlayerItem>> teams){
        int number = 0;
        for(ArrayList<PlayerItem> list : teams){
            if(list.size() == 1 || list.size() == 2){
                number++;
            }
        }
        if((double) number /teams.size() > 0.4 && teams.size() >= 2){
            return true;
        }
        return false;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        Player p = (Player)e.getWhoClicked();
        StatView view = getView(p);
        if(view == null){
            return;
        }
        Statistics sta = Statistics.stats.get(alias);
        int games = sta.getTotalGames();
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
        }else if(e.getCurrentItem().equals(App.ProfileScrollLeft.build())){
            if (!(view.page <= -1) && !onSide) {
                view.page--;
            }
        }else if(e.getCurrentItem().getType() == Material.PLAYER_HEAD){
            view.startPlayerView(e.getCurrentItem());
            onSide = true;
            return;
        }

        if(view.isGameView) {
            view.startGameView(false);
        }else{
            view.restartPlayerView();
        }
    }
}