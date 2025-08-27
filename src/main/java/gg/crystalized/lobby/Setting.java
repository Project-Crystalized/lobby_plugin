package gg.crystalized.lobby;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.function.Consumer;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public class Setting {
    public static NamespacedKey key = new NamespacedKey("crystalized", "settings");
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
                Consumer<PersistentDataContainer> con = pdc -> pdc.set(key, PersistentDataType.STRING, (String)a.extra);
                ItemStack item = app.build();
                item.editPersistentDataContainer(con);
                int i = a.slot + 1;
                inv.setItem(i, item);
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
        Consumer<ItemMeta> meta = m -> item.getItemMeta().lore(List.of(getDescription(a, value)));
        item.editMeta(meta);
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
                return Component.text("Small").color(WHITE).decoration(ITALIC, false);
            }else if(value == 0.5){
                return Component.text("Default").color(WHITE).decoration(ITALIC, false);
            }else if(value == 1){
                return Component.text("Tall").color(WHITE).decoration(ITALIC, false);
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

    public static double toDouble(Object o){
        if(o instanceof Double){
            return (Double)o;
        }
        return Double.valueOf((Integer) o);
    }

    public static boolean hasMidValue(App app){
        if(app == App.MsgSetting || app == App.PartyRequestSetting ||  app == App.PlayerHeightSetting || app == App.PlayerVisibilitySetting){
            return true;
        }
        return false;
    }
    public static void changeSettings(App app, Player p){
        if(isToggle(app)){

        }
    }
}
