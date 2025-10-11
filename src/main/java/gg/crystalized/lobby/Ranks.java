package gg.crystalized.lobby;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.citizensnpcs.api.CitizensAPI;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.NotNull;


import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.bukkit.entity.EntityType.TEXT_DISPLAY;
import static org.bukkit.entity.TextDisplay.TextAlignment.CENTER;
import static org.bukkit.scoreboard.DisplaySlot.BELOW_NAME;

public class Ranks {
    /*
    Rank ID system:
    0 = rankless
    1 = admin
    2 = mod
    3 = dev
    4 = contributer
    5 = sub-project maker
    6 = one time payment
    7 = subscription
     */

    public static Component getName(OfflinePlayer p){
        return getIcon(p).append(text(" ")).append(getColoredName(p));
    }

    public static Component getNameWithName(OfflinePlayer p){
        return getRankWithName(p).append(text(" ")).append(getColoredName(p));
    }

    public static Component getColoredName(OfflinePlayer p){
        int rank = getRank(p);
        String hexColor = "#a1a1a1";
        if(rank == 1) {
            hexColor = "#ba1560";
        } else if(rank == 2) {
            hexColor = "#22d87a";
        } else if(rank == 3) {
            hexColor = "#379fe5";
        } else if(rank == 4) {
            hexColor = "#bf750f";
        } else if(rank == 5) {
            hexColor = "#087544";
        }
        return Component.text(p.getName()).color(TextColor.fromHexString(hexColor)).decoration(ITALIC, false);
    }

    public static Component getRankWithName(OfflinePlayer p){
        int rank = getRank(p);
        String icon = "";

        if(rank == 1) {
            icon = "\uE300";
        } else if(rank == 2) {
            icon = "\uE306";
        } else if(rank == 3) {
            icon = "\uE302";
        } else if(rank == 4) {
            icon = "\uE304";
        } else if(rank == 5) {
            icon = "\uE308";
        }
        return text(icon).decoration(ITALIC, false);
    }

    public static Component getIcon(OfflinePlayer p){
        int rank = getRank(p);
        String icon = "";

        if(rank == 1) {
            icon = "\uE301";
        } else if(rank == 2) {
            icon = "\uE307";
        } else if(rank == 3) {
            icon = "\uE303";
        } else if(rank == 4) {
            icon = "\uE305";
        } else if(rank == 5) {
            icon = "\uE309";
        }

        return text(icon).color(WHITE).decoration(ITALIC, false);
    }

    public static boolean isRankSymbol(char c){
        String s = "\\uE30";
        for(int i = 0; i <= 9; i++){
            String f = s + i;
            if(String.valueOf(c).equals(f)){
                return true;
            }
        }
        return false;
    }

    public static Component getJoinMessage(Player p){
        int rank = getRank(p);

        if(rank == 0){
            return Component.text("");
        }

        return getNameWithName(p).append(Component.text(" joined the game").color(GREEN));
    }

    public static void renderNameTags(Player p){
        TextDisplay display = (TextDisplay) Bukkit.getWorld("world").spawnEntity(new Location(Bukkit.getWorld("world"), 0, 0, 0), TEXT_DISPLAY);
        Component text = getRankWithName(p).append(text("\n")).append(getColoredName(p)).append(text("\n"));
        display.text(text);
        display.setAlignment(CENTER);
        display.setBillboard(Display.Billboard.CENTER);
        display.setPersistent(false);
        display.getPersistentDataContainer().set(new NamespacedKey("crystalized", "nametag"), PersistentDataType.STRING, p.getName() + "_nametag");
        p.addPassenger(display);
        p.hideEntity(Lobby_plugin.getInstance(), display);
    }

    public static void renderTabList(Player p){
        p.sendPlayerListHeaderAndFooter(

                // Header
                text("\nProject Crystalized Lobby\n").

                        color(NamedTextColor.LIGHT_PURPLE),

                // Footer
                        text("\ncrystalized.cc\n").

                                color(NamedTextColor.DARK_GRAY));

        p.playerListName(getName(p));
        orderList();
    }

    public static void orderList(){
        HashMap<Integer, ArrayList<Player>> map = new HashMap<>();
        for(Player p : Bukkit.getOnlinePlayers()){
            if(!map.containsKey(getRank(p))){
                ArrayList<Player> list = new ArrayList<>();
                list.add(p);
                map.put(getRank(p), list);
            }else {
                ArrayList<Player> list = map.get(getRank(p));
                list.add(p);
                map.replace(getRank(p), list);
            }
        }

        int i = 1;

        for(int in = 10; in >= 1; in--){
            if(!map.containsKey(in)){
                continue;
            }
            for(Player p : map.get(in)){
                p.setPlayerListOrder(i);
                i++;
            }
            map.remove(in);
        }
        for(ArrayList<Player> li : map.values()){
            for(Player pl : li){
                pl.setPlayerListOrder(i);
                i++;
            }
        }
    }

    public static Player nextInLine(int i){
        for(Player p : Bukkit.getOnlinePlayers()){
            if(p.getPlayerListOrder() == i + 1){
                return p;
            }
        }
        return null;
    }


    public static ItemStack buildItem(OfflinePlayer p){
        ItemStack item = new ItemStack(Material.COAL);
        ItemMeta meta = item.getItemMeta();
        String model = "ui/invisible";

        int rank = getRank(p);
        if(rank == 1) {
            model = "ui/scn3/profile/rank_admin";
        } else if(rank == 2) {
            model = "ui/scn3/profile/rank_mod";
        } else if(rank == 3) {
            model = "ui/scn3/profile/rank_dev";
        } else if(rank == 4) {
            model = "ui/scn3/profile/rank_contributor";
        } else if(rank == 5) {
            model = "ui/scn3/profile/rank_sub_project";
        }

        NamespacedKey key = new NamespacedKey("crystalized", model);
        meta.setItemModel(key);
        meta.displayName(getRankWithName(p));
        item.setItemMeta(meta);
        return item;
    }

    public static void passiveNames(Player p, TextColor color, Component before, Component after){
        Component a = getIcon(p);
        Component b = text(" ").append(text(p.getName()).color(color));
        if(before != null){
           a =  a.append(before);
        }

        if(after != null){
           b = b.append(after);
        }

        p.displayName(a.append(b));
    }

    public static int getRank(OfflinePlayer p){
        HashMap<String, Object> data = LobbyDatabase.fetchPlayerData(p);
        if(data.get("rank_id") == null){
            return 0;
        }

        if((Integer)data.get("rank_id") != 0){
            return (Integer)data.get("rank_id");
        }

        return (Integer)data.get("pay_rank_id");
    }
}
