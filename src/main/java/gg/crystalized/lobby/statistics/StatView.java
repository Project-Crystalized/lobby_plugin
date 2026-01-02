package gg.crystalized.lobby.statistics;

import gg.crystalized.lobby.App;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;

public class StatView {
    static ArrayList<StatView> views = new ArrayList<>();
    int page = 0;
    Player viewer;

    public StatView(Player viewer){
        this.viewer = viewer;
        views.add(this);
    }
    public void startPlayerView(String alias, Inventory profile){
        Inventory inv = Bukkit.createInventory(viewer, 54, "\uA000\uA006");
        OfflinePlayer p = Bukkit.getOfflinePlayer(profile.getItem(2).getPersistentDataContainer().get(new NamespacedKey("crystalized", "profile_holder"), PersistentDataType.STRING));
        ArrayList<StatItem> stats = getForGame(alias, p, true, page);
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
        //TODO arrows for scrolling
        inv.setItem(50, App.ProfileScrollRight.build());
        viewer.openInventory(inv);
    }

    public static void stopView(Player p){
        if(isInView(p)) {
            views.removeIf(v -> v.viewer.equals(p));
        }
    }

    private static boolean isInView(Player p){
        for(StatView view : views){
            if(view.viewer.equals(p)){
                return true;
            }
        }
        return false;
    }

    public static ArrayList<StatItem> getForGame(String alias, OfflinePlayer p, boolean isLifetime, int page){
        return switch(alias){
            case "ls" -> LsStats.getLsPlayerStats(p, page, isLifetime);
            default -> null;
        };
    }
}
