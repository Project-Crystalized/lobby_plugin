package gg.crystalized.lobby.statistics;

import gg.crystalized.lobby.LobbyDatabase;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.bukkit.Material.*;
import static org.bukkit.Material.COAL;

public class KoStats {
    public static final String URL = "jdbc:sqlite:"+ System.getProperty("user.home")+"/databases/knockoff_db.sql";
    public static ArrayList<StatItem> getKoPlayerStats(OfflinePlayer p, int page, boolean isLifetime){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM KoGamesPlayers WHERE player_uuid = ? AND game = ?;");
            int gameId = getGameId(page, p);
            if(gameId == -1){
                ArrayList<StatItem> none = new ArrayList<>();
                none.add(new StatItem(null, "ko", false));
                return none;
            }
            prep.setInt(2, gameId);
            if(isLifetime){
                prep = conn.prepareStatement("SELECT * FROM KoGamesPlayers WHERE player_uuid = ?;");
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
                    units.add(new StatUnit<>(p, data.getColumnLabel(i), 0, "ko", isLifetime));
                    continue;
                }
                if (isLifetime && data.getColumnLabel(i).equals("game")) {
                    units.add(new StatUnit<>(p, data.getColumnLabel(i), gameId, "ko", isLifetime));
                } else if (isLifetime && data.getColumnLabel(i).equals("games_won")) {
                    units.add(new StatUnit<>(p, data.getColumnLabel(i), countGames(p, true), "ko", isLifetime));
                }else if (isLifetime && fun.apply(set) instanceof Integer) {
                    units.add(new StatUnit<>(p, data.getColumnLabel(i), sumColumns(data.getColumnLabel(i), p), "ko", isLifetime));
                }else {
                    units.add(new StatUnit<>(p, data.getColumnLabel(i), fun.apply(set), "ko", isLifetime));
                }
            }
            if(isLifetime){
                units.add(new StatUnit<>(p, "percent", calculatePercent(p), "ko", isLifetime));
            }else{
                units.add(new StatUnit<>(p, "winner", getTeam(gameId, true), "ko", isLifetime));
                units.add(new StatUnit<>(p, "gametype", getGameType(gameId), "ko", isLifetime));
                units.add(new StatUnit<>(p, "map", getMap(gameId), "ko", isLifetime));
            }
            units.add(new StatUnit<>(p, "name", p.getName(), "ko", isLifetime));
            return StatItem.makeItemFromUnit(units, "ko", isLifetime);
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get stat data for ko player: " + p.getName());
            return null;
        }
    }

    public static Integer getGameId(int page, OfflinePlayer p){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT game FROM KoGamesPlayers WHERE player_uuid = ? ORDER BY game DESC;");
            prep.setBytes(1, LobbyDatabase.uuid_to_bytes(p));
            ResultSet set = prep.executeQuery();
            if(!set.next()){
                return -1;
            }
            int total = countGames(p, false);
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

    public static int countGames(OfflinePlayer p, boolean withWinning){
        try(Connection conn = DriverManager.getConnection(URL)) {
            String add = "";
            if(withWinning){
                add = " AND games_won = 1";
            }
            PreparedStatement prep = conn.prepareStatement("SELECT COUNT(game) AS count FROM KoGamesPlayers WHERE player_uuid = ?" + add + ";");
            prep.setBytes(1, LobbyDatabase.uuid_to_bytes(p));
            ResultSet set = prep.executeQuery();
            set.next();
            return set.getInt("count");
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't count games");
            return 0;
        }
    }

    public static ArrayList<String> getTeam(int gameId, boolean won){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT player_uuid FROM KoGamesPlayers WHERE games_won = ? AND game = ?;");
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
            PreparedStatement prep = conn.prepareStatement("SELECT map FROM KnockoffGames WHERE game_id = ?;");
            prep.setInt(1, gameId);
            ResultSet set = prep.executeQuery();
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
            PreparedStatement prep = conn.prepareStatement("SELECT SUM(" + column + ") AS sum FROM KoGamesPlayers WHERE player_uuid = ?;");
            prep.setBytes(1, LobbyDatabase.uuid_to_bytes(p));
            ResultSet set = prep.executeQuery();
            set.next();
            return set.getInt("sum");
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't sum Columns");
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

    public static String getGameType(int gameId){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT gametype FROM KnockoffGames WHERE game_id = ?;");
            prep.setInt(1, gameId);
            ResultSet set = prep.executeQuery();
            set.next();
            return set.getString("gametype");
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get gametype");
            return null;
        }
    }
}

enum KoGroup {
    GAMES(new String[]{"name", "game", "games_won", "percent"}, true),
    GAME(new String[]{"game", "gametype","map", "name"}, false),
    TEAM(new String[]{"winner", "other_team"}, false),
    DEATHS(new String[]{"kills", "deaths"}, true),
    BLOCKS(new String[]{"blocks_placed", "blocks_broken"}, true),
    ITEMS(new String[]{"items_collected", "items_used"}, true);
    final String[] columns;
    final boolean isLifetime;
    KoGroup(String[] columns, boolean isLifetime) {
        this.columns = columns;
        this.isLifetime = isLifetime;
    }

    public static KoGroup getGroup(String name, boolean lifetime) {
        KoGroup group = group(name, lifetime);
        if(group != null){
            return group;
        }
        group = group(name, !lifetime);
        if(!(!lifetime && group != null && StatItem.notIndividual(group))) {
            return null;
        }
        return group;
    }

    public static KoGroup group(String name, boolean lifetime){
        for (KoGroup group : KoGroup.values()) {
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
            KoGroup group = getGroup(unit.name, unit.isLifetime);
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
            KoGroup group = getGroup(unit[0].name, unit[0].isLifetime);
            unit = sortWithTemplate(unit, group);
        }

        stat.sort((un1, un2) -> un1.length != 0 && un2.length != 0 ? getGroup(un1[0].name, un1[0].isLifetime).ordinal() > getGroup(un2[0].name, un2[0].isLifetime).ordinal() ? 1 : -1 : 0);
        return stat;
    }

    public static StatUnit<?>[] sortWithTemplate(StatUnit<?>[] unit, KoGroup group){
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
            case TEAM:
                return new ItemStack(PUMPKIN_SEEDS);
            case DEATHS:
                return new ItemStack(BONE);
            case BLOCKS:
                return new ItemStack(AMETHYST_BLOCK);
            case ITEMS:
                ItemStack item = new ItemStack(COAL);
                ItemMeta meta = item.getItemMeta();
                meta.setItemModel(new NamespacedKey("crystalized", "items/cloud_totem"));
                item.setItemMeta(meta);
                return item;
            default: return new ItemStack(COAL);
        }
    }
}
