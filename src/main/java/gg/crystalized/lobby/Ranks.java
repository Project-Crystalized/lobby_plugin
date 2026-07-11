package gg.crystalized.lobby;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.*;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.lang3.SerializationUtils;
import org.bukkit.*;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.Nullable;


import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.bukkit.attribute.Attribute.SCALE;
import static org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR;
import static org.bukkit.entity.EntityType.TEXT_DISPLAY;
import static org.bukkit.entity.TextDisplay.TextAlignment.CENTER;

public enum Ranks {
    /*
    Rank ID system:
    0 = rankless
    1 = admin
    2 = mod
    3 = dev
    4 = contributer
    5 = sub-project maker
    6 = creator

    7 = supporter
    8 = one time payment (moon)
    9 = subscription (sun)
     */

    rankless("#a1a1a1", "", "", 10, "[J] rankless", "ui/invisible", false),
    admin("#ba1560", "\uE301","\uE300", 1, "[A] admin", "ui/scn3/profile/rank_admin", false),
    mod("#22d87a", "\uE307", "\uE306", 2, "[B] mod", "ui/scn3/profile/rank_mod", false),
    dev("#379fe5", "\uE303", "\uE302", 3, "[C] dev", "ui/scn3/profile/rank_dev", false),
    contributor("#bf750f", "\uE305", "\uE304", 4, "[D] contributor", "ui/scn3/profile/rank_contributor", false),
    sub_project_maker("#087544", "\uE309", "\uE308", 5, "[E] sub-project", "ui/scn3/profile/rank_sub_project", false),
    creator("#f63c3c", "\uE30B", "\uE30A", 6, "[F] creator", "ui/scn3/profile/creator", false),
    sun_sub("#d3af10", "\uE30D", "\uE30C", 7, "[G] sun_sub", "ui/scn3/profile/sun", true),
    moon_one("#512f7c", "\uE30F", "\uE30E", 8, "[H] moon_one", "ui/scn3/profile/moon", true),
    supporter("#1a1a1a", "\uE311", "\uE310", 9,"[I] supporter", "ui/scn3/profile/supporter", true);

    final String color;
    final String icon;
    final String iconWithName;
    final int priority;
    final String teamName;
    final String model;
    final boolean payed;

    Ranks(String color, String icon, String iconWithName, int priority, String teamName, String model, boolean payed) {
        this.color = color;
        this.icon = icon;
        this.iconWithName = iconWithName;
        this.priority = priority;
        this.teamName = teamName;
        this.model = model;
        this.payed = payed;
    }

    public static Component getName(OfflinePlayer p){
        return getIcon(p).append(text(" ")).append(getColoredName(p));
    }

    private static String getPName(OfflinePlayer p){
        if(p.getName() != null){
            return p.getName();
        }
        if(LobbyDatabase.getPlayerName(p) != null) {
            return LobbyDatabase.getPlayerName(p);
        }
        return "null";
    }

    public static Component getNameWithName(OfflinePlayer p){
        return getRankWithName(p).append(text(" ")).append(getColoredName(p));
    }

    public static Component getColoredName(OfflinePlayer p){
        return Component.text(getPName(p)).color(TextColor.fromHexString(getRank(p).color)).decoration(ITALIC, false);
    }

    public static Component getRankWithName(OfflinePlayer p){
        return text(getRank(p).iconWithName).decoration(ITALIC, false);
    }

    public static Component getIcon(OfflinePlayer p){
        return text(getRank(p).icon).color(WHITE).decoration(ITALIC, false);
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
        if(getRank(p) == rankless){
            return Component.text("");
        }

        return getNameWithName(p).append(Component.text(" joined the game").color(GREEN));
    }

    public static void renderTabList(Player p){
        p.sendPlayerListHeaderAndFooter(

                // Header
                text("\nProject Crystalized Lobby\n").

                        color(LIGHT_PURPLE),

                // Footer
                        text("\ncrystalized.cc\n").

                                color(NamedTextColor.DARK_GRAY));

        p.playerListName(getName(p));
        //orderList();
    }
    /*
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

     */

    public static Player nextInLine(int i){
        for(Player p : Bukkit.getOnlinePlayers()){
            if(p.getPlayerListOrder() == i + 1){
                return p;
            }
        }
        return null;
    }

    public static void doRankTeams(Player p){
        Scoreboard s = p.getScoreboard();
        for(Player player : Bukkit.getOnlinePlayers()){
            String name = getRank(player).teamName;
            Team team = s.getTeam(name);
            if(team == null){
                team = s.registerNewTeam(name);
                team.prefix(getRankWithName(p));
                //team.color(getRank(p).color);
            }
            team.addPlayer(player);
        }
    }


    public static ItemStack buildItem(OfflinePlayer p){
        ItemStack item = new ItemStack(Material.COAL);
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey("crystalized", getRank(p).model);
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

    public static Ranks getRank(OfflinePlayer p){
        HashMap<String, Object> data = LobbyDatabase.fetchPlayerData(p);
        if(data.get("rank_id") == null){
            return rankless;
        }

        if((Integer)data.get("rank_id") != 0){
            return values()[(Integer)data.get("rank_id")];
        }

        return values()[getPayRank(p)];
    }

    public static int getPayRank(OfflinePlayer p){
        Ranks smallest = rankless;

        for(short s : convertByteToShort(LobbyDatabase.getPayedRank(p))){
            if(values()[s].priority < smallest.priority) smallest = values()[s];
        }

        return smallest.ordinal();
    }

    public static short[] addOrRemovePayedRank(OfflinePlayer p, Integer rankId){
        short[] ranks = convertByteToShort(LobbyDatabase.getPayedRank(p));
        short[] ne = new short[ranks.length+1];
        boolean remove = false;
        if(arrayContains(ranks, rankId.shortValue())){
            ne = new short[ranks.length-1];
            remove = true;
        }

        int w = 0;
        for(short s : ranks){
            if(rankId == s){
                continue;
            }
            ne[w] = s;
            w++;
        }

        if(!remove){
            ne[ne.length-1] = rankId.shortValue();
        }

        return ne;
    }

    public static short[] convertByteToShort(byte[] b){
        if(b == null || b.length == 0) return new short[]{};
        short[] s = new short[b.length/2];
        int j = 0;
        for(int i = 0; i < b.length; i = i+2){
            short sh = ByteBuffer.wrap(new byte[]{b[i], b[i+1]}).getShort();
            s[j] = sh;
            j++;
        }
        return s;
    }

    public static boolean arrayContains(short[] array, Short sho){
        ArrayList<Short> list = new ArrayList<>();
        for(short s : array) list.add(s);
        return list.contains(sho);
    }
}
