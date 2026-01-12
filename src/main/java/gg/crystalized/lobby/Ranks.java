package gg.crystalized.lobby;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.Nullable;


import java.util.*;
import java.util.concurrent.ExecutionException;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.bukkit.entity.EntityType.TEXT_DISPLAY;
import static org.bukkit.entity.TextDisplay.TextAlignment.CENTER;

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
    private static int EntityId = 1000000;

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

    public static TextColor getColor(int i){
        String hexColor = "#a1a1a1";
        if(i == 1) {
            hexColor = "#ba1560";
        } else if(i == 2) {
            hexColor = "#22d87a";
        } else if(i == 3) {
            hexColor = "#379fe5";
        } else if(i == 4) {
            hexColor = "#bf750f";
        } else if(i== 5) {
            hexColor = "#087544";
        }
        return TextColor.fromHexString(hexColor);
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

    public static String getRankWithNameAsString(OfflinePlayer p){
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
        return icon;
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

    public static String getIconAsString(OfflinePlayer p){
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

        return icon;
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

    public static String getTeamName(int i){
        return switch (i){
            case 1 -> "[A] admin";
            case 2 -> "[B] mod";
            case 3 -> "[C] dev";
            case 4 -> "[D] contrib";
            case 5 -> "[E] sub-project";
            case 6 -> "[F] one-time";
            case 7 -> "[G] subscription";
            default -> "[H] rankless";
        };
    }

    public static void renderNameTags(Player p){
        Scoreboard s = p.getScoreboard();
        for(int i = 0; i <= 7; i++){
            Team team  = s.registerNewTeam(getTeamName(i));
            team.color(NamedTextColor.namedColor(getColor(i).value()));
            for(Player player : Bukkit.getOnlinePlayers()){
                if(getRank(player) == i){
                    team.addPlayer(player);
                }
            }
        }
        new Nametag(p);
        /*
        UUID uuid = UUID.randomUUID();
        PacketContainer spawnEntity = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
        spawnEntity.getIntegers().write(0, EntityId);
        spawnEntity.getUUIDs().write(0, uuid);
        spawnEntity.getEntityTypeModifier().write(0, TEXT_DISPLAY);
        spawnEntity.getDoubles().write(0, p.getLocation().getX()).write(1, p.getLocation().getY()).write(2, p.getLocation().getZ());

        PacketContainer setMetaData = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        setMetaData.getIntegers().write(0, EntityId);
        WrappedDataWatcher watcher = new WrappedDataWatcher();
        watcher.setEntity(Bukkit.getEntity(uuid));
        watcher.setByte(0, (byte)0x00, true);
        Component text = getRankWithName(p).append(text("\n")).append(getColoredName(p)).append(text("\n"));
        watcher.setChatComponent(23, WrappedChatComponent.fromJson("{\"extra\":{\"hex\":\""+ getColor(getRank(p)) +"\",\"text\":\"" + getRankWithNameAsString(p) + "\\n" + p.getName() + "\"}}"), false);
        setMetaData.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

        for(Player player : Bukkit.getOnlinePlayers()) {
            Lobby_plugin.protocolManager.sendServerPacket(player, spawnEntity);
            Lobby_plugin.protocolManager.sendServerPacket(player, setMetaData);
        }

        EntityId++;

        /*
        TextDisplay display = (TextDisplay) Bukkit.getWorld("world").spawnEntity(new Location(Bukkit.getWorld("world"), 0, 0, 0), TEXT_DISPLAY);
        Component text = getRankWithName(p).append(text("\n")).append(getColoredName(p)).append(text("\n"));
        display.text(text);
        display.setAlignment(CENTER);
        display.setBillboard(Display.Billboard.CENTER);
        display.setPersistent(false);
        display.getPersistentDataContainer().set(new NamespacedKey("crystalized", "nametag"), PersistentDataType.STRING, p.getName() + "_nametag");
        p.addPassenger(display);
        p.hideEntity(Lobby_plugin.getInstance(), display);
         */
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
