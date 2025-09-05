package gg.crystalized.lobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.*;


import java.util.*;
import java.util.logging.Level;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

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
        int rank = getRank(p);
        Component name;
        if(p.getName() == null){
            name = text("null");
        }else {
            name = text(p.getName());
        }
        String icon = "";
        String hexColor = "#a1a1a1";

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

        if(rank == 1) {
            hexColor = "#ba1560";
        } else if(rank == 2) {
            hexColor = "#22d87a";
        } else if(rank == 3) {
            hexColor = "#379fe5";
        } else if(rank == 4) {
            hexColor = "#bf750f";
        } else if(rank == 5) {
            hexColor = "#087544"; //TODO get the right color for this
        }

        name = name.color(TextColor.fromHexString(hexColor)).decoration(ITALIC, false);

        return Component.text(icon + " ").decoration(ITALIC, false).color(WHITE).append(name);
    }

    public static Component getNameWithName(OfflinePlayer p){
        int rank = getRank(p);
        Component name = text(p.getName());
        String icon = "";
        String hexColor = "#a1a1a1";

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


        if(rank == 1) {
            hexColor = "#ba1560";
        } else if(rank == 2) {
            hexColor = "#22d87a";
        } else if(rank == 3) {
            hexColor = "#379fe5";
        } else if(rank == 4) {
            hexColor = "#bf750f";
        } else if(rank == 5) {
            hexColor = "#087544"; //TODO get the right color for this
        }

        name = name.color(TextColor.fromHexString(hexColor)).decoration(ITALIC, false);

        return Component.text(icon + " ").color(WHITE).decoration(ITALIC, false).append(name);
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
        HashMap<String, Object> data = LobbyDatabase.fetchPlayerData(p);
        Component level = Component.text("\n" + data.get("level") + "   ").color(GREEN);
        Component money = Component.text("" + data.get("money")); //TODO style this
        p.displayName(getNameWithName(p).append(level).append(money));
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

        Scoreboard s = p.getScoreboard();
        String team = "[H] Rankless";
        int rank = getRank(p);

        if(rank == 1) {
            team = "[A] Admin";
        } else if(rank == 2) {
            team = "[B] Mod";
        } else if(rank == 3) {
            team = "[C] Dev";
        } else if(rank == 4) {
            team = "[D] Contrib";
        } else if(rank == 5) {
            team = "[E] Sub_project";
        } else if(rank == 6){
            team = "[F] One time payment";
        } else if(rank == 7){
            team = "[G] Subscription";
        }

        Team t = s.registerNewTeam(team);
        t.addEntity(p);

        p.setScoreboard(s);
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
            model = "ui/scn3/profile/rank_contributer";
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
