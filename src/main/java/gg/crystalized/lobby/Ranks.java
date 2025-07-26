package gg.crystalized.lobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.*;

import static java.util.Arrays.stream;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.bukkit.scoreboard.NameTagVisibility.NEVER;

public class Ranks {

    /*
    Rank ID system:
    0 = rankless
    1 = admin
    2 = mod
    3 = dev
    4 = contributer
    5 = sub-project maker
    6 = ???
    7 = ???
     */

    public static Component getName(OfflinePlayer p){
        HashMap<String, Object> data = LobbyDatabase.fetchPlayerData(p);
        Component name = text(p.getName());
        String icon = "";
        String hexColor = "#a1a1a1";

        if((Integer)data.get("rank_id") == 1) {
            icon = "\uE301";
        } else if((Integer)data.get("rank_id") == 2) {
            icon = "\uE307";
        } else if((Integer)data.get("rank_id") == 3) {
            icon = "\uE303";
        } else if((Integer)data.get("rank_id") == 4) {
            icon = "\uE305";
        } else if((Integer)data.get("rank_id") == 5) {
            icon = "\uE309";
        }

        if(Lobby_plugin.getInstance().passive_mode){
          return Component.text(icon + " ").color(WHITE).decoration(ITALIC, false).append(name);
        }

        if((Integer)data.get("rank_id") == 1) {
            hexColor = "#ba1560";
        } else if((Integer)data.get("rank_id") == 2) {
            hexColor = "#22d87a";
        } else if((Integer)data.get("rank_id") == 3) {
            hexColor = "#379fe5";
        } else if((Integer)data.get("rank_id") == 4) {
            hexColor = "#bf750f";
        } else if((Integer)data.get("rank_id") == 5) {
            hexColor = "#087544"; //TODO get the right color for this
        }

        name = name.color(TextColor.fromHexString(hexColor)).decoration(ITALIC, false);

        return Component.text(icon + " ").color(WHITE).append(name);
    }

    public static Component getNameWithName(Player p){
        HashMap<String, Object> data = LobbyDatabase.fetchPlayerData(p);
        Component name = text(p.getName());
        String icon = "";
        String hexColor = "#a1a1a1";

        if((Integer)data.get("rank_id") == 1) {
            icon = "\uE300";
        } else if((Integer)data.get("rank_id") == 2) {
            icon = "\uE306";
        } else if((Integer)data.get("rank_id") == 3) {
            icon = "\uE302";
        } else if((Integer)data.get("rank_id") == 4) {
            icon = "\uE304";
        } else if((Integer)data.get("rank_id") == 5) {
            icon = "\uE308";
        }

        if(Lobby_plugin.getInstance().passive_mode){
            return Component.text(icon + " ").color(WHITE).append(name);
        }

        if((Integer)data.get("rank_id") == 1) {
            hexColor = "#ba1560";
        } else if((Integer)data.get("rank_id") == 2) {
            hexColor = "#22d87a";
        } else if((Integer)data.get("rank_id") == 3) {
            hexColor = "#379fe5";
        } else if((Integer)data.get("rank_id") == 4) {
            hexColor = "#bf750f";
        } else if((Integer)data.get("rank_id") == 5) {
            hexColor = "#087544"; //TODO get the right color for this
        }

        name = name.color(TextColor.fromHexString(hexColor)).decoration(ITALIC, false);

        return Component.text(icon + " ").color(WHITE).append(name);
    }

    public static Component getRankWithName(Player p){
        HashMap<String, Object> data = LobbyDatabase.fetchPlayerData(p);
        String icon = "";

        if((Integer)data.get("rank_id") == 1) {
            icon = "\uE300";
        } else if((Integer)data.get("rank_id") == 2) {
            icon = "\uE306";
        } else if((Integer)data.get("rank_id") == 3) {
            icon = "\uE302";
        } else if((Integer)data.get("rank_id") == 4) {
            icon = "\uE304";
        } else if((Integer)data.get("rank_id") == 5) {
            icon = "\uE308";
        }
        return text(icon);
    }

    public static Component getJoinMessage(Player p){
        HashMap<String, Object> data = LobbyDatabase.fetchPlayerData(p);

        if((Integer)data.get("rank_id") == 0 || (Integer)data.get("rank_id") == 0){
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
        HashMap<String, Object> data = LobbyDatabase.fetchPlayerData(p);

        if((Integer)data.get("rank_id") == 1) {
            team = "[A] Admin";
        } else if((Integer)data.get("rank_id") == 2) {
            team = "[B] Mod";
        } else if((Integer)data.get("rank_id") == 3) {
            team = "[C] Dev";
        } else if((Integer)data.get("rank_id") == 4) {
            team = "[D] Contrib";
        } else if((Integer)data.get("rank_id") == 5) {
            team = "[E] Sub_project";
        }

        Team t = s.registerNewTeam(team);
        t.addEntity(p);

        p.setScoreboard(s);
    }
}
