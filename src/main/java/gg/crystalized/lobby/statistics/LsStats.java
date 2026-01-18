package gg.crystalized.lobby.statistics;

import gg.crystalized.lobby.LobbyDatabase;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.*;
import java.util.function.Function;

import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.bukkit.Material.*;

public class LsStats {
    public static final String URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/litestrike_db.sql";
    static String[] LsItem = new String[]{
        null,
        "item.minecraft.diamond_chestplate",
        "item.minecraft.iron_sword",
        "item.minecraft.iron_axe",
        "item.minecraft.arrow",
        "crystalized.item.defuser.name",
        "item.minecraft.golden_apple",
        "item.minecraft.iron_chestplate",
        "crystalized.crossbow.quickcharge.name",
        "crystalized.sword.pufferfish.name",
        "crystalized.sword.slime.name",
        "crystalized.bow.marksman.name",
        "crystalized.bow.ricochet.name",
        "crystalized.crossbow.multi.name",
        "crystalized.crossbow.charged.name",
        "item.minecraft.potion.effect.swiftness", //16
        "item.minecraft.potion.effect.swiftness", //17
        "Potion of Resistance",
        "item.minecraft.spectral_arrow",
        "crystalized.item.dragonarrow.name",
        "crystalized.item.explosivearrow.name",
        "crystalized.sword.underdog.name",
        "item.minecraft.crossbow"
    };
    public static ArrayList<StatItem> getLsPlayerStats(OfflinePlayer p, int page, boolean isLifetime){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM LsGamesPlayers WHERE player_uuid = ? AND game = ?;");
            int gameId = getGameId(page, p);
            if(gameId == -1){
                ArrayList<StatItem> none = new ArrayList<>();
                none.add(new StatItem(null, "ls", false));
                return none;
            }
            prep.setInt(2, gameId);
            if(isLifetime){
                prep = conn.prepareStatement("SELECT * FROM LsGamesPlayers WHERE player_uuid = ?;");
            }
            prep.setBytes(1, LobbyDatabase.uuid_to_bytes(p));

            ResultSet set = prep.executeQuery();
            set.next();
            ResultSetMetaData data = set.getMetaData();
            int count = data.getColumnCount();
            ArrayList<StatUnit<?>> units = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                Function<ResultSet, ?> fun = StatItem.getMethod(data.getColumnTypeName(i), data.getColumnLabel(i));
                if(fun.apply(set) == null){
                    units.add(new StatUnit<>(p, data.getColumnLabel(i), 0, "ls", isLifetime));
                    continue;
                }
                if (isLifetime && data.getColumnLabel(i).equals("game")) {
                    units.add(new StatUnit<>(p, data.getColumnLabel(i), gameId, "ls", isLifetime));
                } else if (isLifetime && data.getColumnLabel(i).equals("was_winner")) {
                    units.add(new StatUnit<>(p, data.getColumnLabel(i), countGames(p, true), "ls", isLifetime));
                }else if (isLifetime && fun.apply(set) instanceof Integer) {
                    units.add(new StatUnit<>(p, data.getColumnLabel(i), sumColumns(data.getColumnLabel(i), p), "ls", isLifetime));
                }else {
                    units.add(new StatUnit<>(p, data.getColumnLabel(i), fun.apply(set), "ls", isLifetime));
                }
            }
            if(isLifetime){
                units.add(new StatUnit<>(p, "percent", calculatePercent(p), "ls", isLifetime));
            }else{
                units.add(new StatUnit<>(p, "winner", getTeam(gameId, true), "ls", isLifetime));
                units.add(new StatUnit<>(p, "other_team", getTeam(gameId, false), "ls", isLifetime));
                units.add(new StatUnit<>(p, "map", getMap(gameId), "ls", isLifetime));
            }
            units.add(new StatUnit<>(p, "name", p.getName(), "ls", isLifetime));
            return StatItem.makeItemFromUnit(units, "ls", isLifetime);
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get stat data for ls player: " + p.getName());
            return null;
        }
    }

    public static Integer getGameId(int page, OfflinePlayer p){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT game FROM LsGamesPlayers WHERE player_uuid = ? ORDER BY game DESC;");
            prep.setBytes(1, LobbyDatabase.uuid_to_bytes(p));
            ResultSet set = prep.executeQuery();
            if(!set.next()){
                return -1;
            }
            int total = getTotalGames(p);
            if(page <= -1){
                return total;
            }
            if(page > total){
                return 0;
            }
            for(int i = total; i >= total-page; i--){
                if(i == total-page){
                    return set.getInt("game");
                }
                set.next();
            }
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get gameId");
        }
        return -1;
    }

    public static Integer getTotalGames(OfflinePlayer p){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT COUNT(game) AS game FROM LsGamesPlayers WHERE player_uuid = ?;");
            prep.setBytes(1, LobbyDatabase.uuid_to_bytes(p));
            ResultSet set = prep.executeQuery();
            set.next();
            return set.getInt("game");
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get gameId");
        }
        return -1;
    }

    public static ArrayList<String> getTeam(int gameId, boolean won){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT player_uuid FROM LsGamesPlayers WHERE was_winner = ? AND game = ?;");
            if (won) {
                prep.setInt(1, 1);
            } else {
                prep.setInt(1, 0);
            }
            prep.setInt(2, gameId);
            ResultSet set = prep.executeQuery();
            ArrayList<String> names = new ArrayList<>();
            while(set.next()){
                ByteBuffer bb = ByteBuffer.wrap(set.getBytes("player_uuid"));
                OfflinePlayer player = Bukkit.getOfflinePlayer(new UUID(bb.getLong(), bb.getLong()));
                names.add(player.getName());
            }
            return names;
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get team");
            return null;
        }
    }

    public static String getMap(int gameId){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT map FROM LiteStrikeGames WHERE game_id = ?;");
            prep.setInt(1, gameId);
            ResultSet set = prep.executeQuery();
            ArrayList<String> names = new ArrayList<>();
            set.next();
            return set.getString("map");
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get map");
            return null;
        }
    }

    public static int sumColumns(String column, OfflinePlayer p){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT SUM(" + column + ") AS sum FROM LsGamesPlayers WHERE player_uuid = ?;");
            prep.setBytes(1, LobbyDatabase.uuid_to_bytes(p));
            ResultSet set = prep.executeQuery();
            set.next();
            return set.getInt("sum");
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get team");
            return 0;
        }
    }

    public static int countGames(OfflinePlayer p, boolean withWinning){
        try(Connection conn = DriverManager.getConnection(URL)) {
            String add = "";
            if(withWinning){
                add = " AND was_winner = 1";
            }
            PreparedStatement prep = conn.prepareStatement("SELECT COUNT(game) AS count FROM LsGamesPlayers WHERE player_uuid = ?" + add + ";");
            prep.setBytes(1, LobbyDatabase.uuid_to_bytes(p));
            ResultSet set = prep.executeQuery();
            set.next();
            return set.getInt("count");
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get team");
            return 0;
        }
    }

    public static String calculatePercent(OfflinePlayer p){
        int total = countGames(p, false);
        int won = countGames(p, true);
        if(total < 10){
            return "Play at least 10 games to see your percentage";
        }
        return ((won / total) * 100) + "%";
    }

    public static ArrayList<Component> loreForItems(short[] items){
        ArrayList<Component> components = new ArrayList<>();
        if(items.length == 0){
            components.add(Component.text("This player didn't buy any items").decoration(ITALIC, false).color(WHITE));
            return components;
        }
        int i = 1;
        for(short s : items){
            if(LsItem[s] == null){
                components.add(Component.text("Round " + i + ": ").decoration(ITALIC,false).color(GRAY));
            }
            components.add(translatable(LsItem[s]).decoration(ITALIC, false).color(WHITE));
        }
        return components;
    }
}


enum LsGroup {
    GAMES(new String[]{"name", "game", "was_winner", "percent"}, true),
    GAME(new String[]{"game", "map", "name"}, false),
    TEAM(new String[]{"winner", "other_team"}, false),
    WIN(new String[]{"placer_wins", "breaker_wins"}, false),
    DEATHS(new String[]{"kills", "assists", "deaths", "did_leave"}, true),
    DAMAGE(new String[]{"damage_dealt", "hits_dealt"}, true),
    BOMBS(new String[]{"placed_bombs", "broken_bombs"}, true),
    MONEY(new String[]{"gained_money", "spent_money"}, true),
    ITEMS(new String[]{"bought_items"}, false),
    JUMPS(new String[]{"jumps"}, true);
    final String[] columns;
    final boolean isLifetime;
    LsGroup(String[] columns, boolean isLifetime) {
        this.columns = columns;
        this.isLifetime = isLifetime;
    }

    public static LsGroup getGroup(String name, boolean lifetime) {
        LsGroup group = group(name, lifetime);
        if(group != null){
            return group;
        }
        group = group(name, !lifetime);
        if(!(!lifetime && group != null && StatItem.notIndividual(group))) {
            return null;
        }
        return group;
    }

    public static LsGroup group(String name, boolean lifetime){
        for (LsGroup group : LsGroup.values()) {
            if (List.of(group.columns).contains(name) && group.isLifetime == lifetime) {
                return group;
            }
        }
        return null;
    }

    public static ArrayList<StatUnit<?>[]> organise(ArrayList<StatUnit<?>> units){
        ArrayList<StatUnit<?>[]> fin = new ArrayList<>();
        ArrayList<ArrayList<StatUnit<?>>> arrays = new ArrayList<>();
        boolean lifetime = units.getFirst().isLifetime;
        for(StatUnit<?> unit : units){
            LsGroup group = getGroup(unit.name, unit.isLifetime);
            if (group == null) {
                continue;
            }
            boolean life = group.isLifetime;
            boolean doesntexist = true;
            for (ArrayList<StatUnit<?>> arr : arrays) {
                if(getGroup(arr.getFirst().name, life) == null){
                    continue;
                }
                if (getGroup(arr.getFirst().name, life).equals(group)) {
                    arr.add(unit);
                    doesntexist = false;
                    break;
                }
            }
            if (doesntexist) {
                ArrayList<StatUnit<?>> unitArray = new ArrayList<>();
                unitArray.add(unit);
                arrays.add(unitArray);
            }
        }
        for(ArrayList<StatUnit<?>> list : arrays){
            fin.add(StatUnit.toArray(list));
        }
        fin = sortByPriority(fin);
        return fin;
    }

    public static ArrayList<StatUnit<?>[]> sortByPriority(ArrayList<StatUnit<?>[]> stat){
        for(StatUnit<?>[] unit : stat){
            if(unit.length == 1 || unit.length == 0){
                continue;
            }
            LsGroup group = getGroup(unit[0].name, unit[0].isLifetime);
            unit = sortWithTemplate(unit, group);
        }
        
        stat.sort((un1, un2) -> un1.length != 0 && un2.length != 0 ? getGroup(un1[0].name, un1[0].isLifetime).ordinal() > getGroup(un2[0].name, un2[0].isLifetime).ordinal() ? 1 : -1 : 0);
        return stat;
    }

    public static StatUnit<?>[] sortWithTemplate(StatUnit<?>[] unit, LsGroup group){
        List<String> c = Arrays.asList(group.columns);
        StatUnit<?>[] stats = new StatUnit[group.columns.length];
        for(StatUnit<?> stat : unit){
            for(String s : group.columns){
                if(stat.name.equals(s)){
                    stats[c.indexOf(s)] = stat;
                }
            }
        }
        return stats;
    }

    public static ItemStack getBase(StatUnit<?>[] stat){
        if(stat.length == 0){
            return new ItemStack(COAL);
        }
        switch(getGroup(stat[0].name, stat[0].isLifetime)){
            case GAMES, GAME:
                return new ItemStack(COMPASS);
            case WIN:
                return new ItemStack(DIAMOND);
            case DEATHS:
                return new ItemStack(BONE);
            case DAMAGE:
                return new ItemStack(IRON_SWORD);
            case BOMBS:
                ItemStack item = new ItemStack(COAL);
                ItemMeta meta = item.getItemMeta();
                meta.setItemModel(new NamespacedKey("crystalized", "models/bomb/shard"));
                item.setItemMeta(meta);
                return item;
            case MONEY:
                return new ItemStack(GOLD_NUGGET);
            case ITEMS:
                return new ItemStack(DIAMOND_CHESTPLATE);
            case JUMPS:
                return new ItemStack(LEATHER_BOOTS);
            default: return new ItemStack(COAL);
        }
    }
}

