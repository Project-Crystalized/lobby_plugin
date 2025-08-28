package gg.crystalized.lobby;

import net.citizensnpcs.api.CitizensAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


import java.util.ArrayList;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public class Setting {
    /*
       0 = off
       0.5 = mid
       1 = on
        */
    public static void placeToggle(Player p, App a, Inventory inv){
        if(a.self != App.useCases.Set){
            return;
        }
        double value = getValue((String)a.extra, p);
        for(App app : App.values()){
            if(isThatToggle(app, value)){
                int i = a.slot + 1;
                inv.setItem(i, app.build());
                return;
            }
        }
    }

    public static double getValue(String a, Player p){
        Object v = LobbyDatabase.fetchSettings(p).get(a);
        if(v instanceof Double){
            return (Double)v;
        }
        return Double.valueOf((Integer)v);
    }

    public static ItemStack addDescription(App a, Player p){
        if(a.self != App.useCases.Set){
            return a.build();
        }
        double value = getValue((String)a.extra, p);
        ItemStack item = a.build();
        if(getDescription(a, value) == null){
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        ArrayList<Component> l = new ArrayList<>();
        l.add(getDescription(a, value));
        meta.lore(l);
        item.setItemMeta(meta);
        return item;
    }

    public static Component getDescription(App a, double value){
        boolean isOneOfThese = a == App.MsgSetting || a == App.PartyRequestSetting || a == App.PlayerVisibilitySetting;
        if((!hasMidValue(a) || isOneOfThese) && value == 1){
            return Component.text("On").color(GREEN).decoration(ITALIC, false);
        }else if((!hasMidValue(a) || isOneOfThese) && value == 0){
            return Component.text("Off").color(RED).decoration(ITALIC, false);
        }

        if(isOneOfThese && value == 0.5){
            return Component.text("Only friends").color(YELLOW).decoration(ITALIC, false);
        }

        if(a == App.PlayerHeightSetting){
            if(value == 0){
                return Component.text("Small").color(RED).decoration(ITALIC, false);
            }else if(value == 0.5){
                return Component.text("Default").color(YELLOW).decoration(ITALIC, false);
            }else if(value == 1){
                return Component.text("Tall").color(GREEN).decoration(ITALIC, false);
            }
        }
        return null;
    }
    public static boolean isToggle(App app){
        if(app.self != null && app.self.equals(App.useCases.Set) && app.slot == 0){
            return true;
        }
        return false;
    }

    public static boolean isThatToggle(App app, double value){
        if(app.self != null && app.self.equals(App.useCases.Set) && app.extra instanceof Number && value == toDouble(app.extra)){
            return true;
        }
        return false;
    }

    public static Double toDouble(Object o){
        if(o instanceof Double){
            return (Double)o;
        }
        if(!(o instanceof Integer)){
            return null;
        }
        return Double.valueOf((Integer) o);
    }

    public static boolean hasMidValue(App app){
        if(app == App.MsgSetting || app == App.PartyRequestSetting ||  app == App.PlayerHeightSetting || app == App.PlayerVisibilitySetting){
            return true;
        }
        return false;
    }
    public static void changeSettings(App app, Player p, int slot){
        App a = app;
        if(isToggle(app)){
            for(App ap : App.values()){
                if(ap.self == App.useCases.Set && ap.slot != null && ap.slot == slot - 1){
                    a = ap;
                    break;
                }
            }
        }

        if(a == App.PlayerHeightSetting){
            if(((Integer)LobbyDatabase.fetchPlayerData(p).get("pay_rank_id")) != 7){
                p.sendMessage(Component.text("Buy [rank name here] to change your height.").color(RED));
                return;
            }
        }

        double value = toDouble(LobbyDatabase.fetchSettings(p).get((String) a.extra));

        if(value == 0 && hasMidValue(a)){
            value = 0.5;
        }else if(value == 0){
            value = 1;
        }else if(value == 0.5){
            value = 1;
        }else{
            value = 0;
        }

        LobbyDatabase.updateSetting(p, (String) a.extra, value);
        App.Settings.action(p);
        updatePlayerVisibility(p);
    }

    public static void updatePlayerVisibility(Player p){
        double value = toDouble(LobbyDatabase.fetchSettings(p).get("show_players"));
        for(Player player : Bukkit.getOnlinePlayers()){
            if (CitizensAPI.getNPCRegistry().isNPC(player)) {
                continue;
            }

            if((value == 0.5 && LobbyDatabase.areFriends(p, player)) || value == 1){
                p.showPlayer(Lobby_plugin.getInstance(), player);
            }else if((value == 0.5 && !LobbyDatabase.areFriends(p, player)) || value == 0){
                p.hidePlayer(Lobby_plugin.getInstance(), player);
            }
        }
    }
}
