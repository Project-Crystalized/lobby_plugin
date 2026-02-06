package gg.crystalized.lobby.statistics;

import com.destroystokyo.paper.profile.PlayerProfile;
import gg.crystalized.lobby.LobbyDatabase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerTextures;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public class Statistics implements Methods{
    static HashMap<String, Statistics> stats = new HashMap<>();
    String URL;
    String playerTableName;
    String gameTableName;
    String uuid;
    String wasWinner;
    String map;
    String[] gameStats;
    String[] gameStatsGeneralData;
    Class<? extends Statistics> statClass;
    TextColor color;

    public Statistics(String URL, String playerTableName, String gameTableName, String uuid, String wasWinner, String map, String[] gameStats, String[] gameStatsGeneralData, Class<? extends Statistics> statClass, TextColor color) {
        this.URL = URL;
        this.playerTableName = playerTableName;
        this.gameTableName = gameTableName;
        this.uuid = uuid;
        this.wasWinner = wasWinner;
        this.map = map;
        this.gameStats = gameStats;
        this.gameStatsGeneralData = gameStatsGeneralData;
        this.statClass = statClass;
        this.color = color;
    }

    public Statistics(Statistics stat){
        this.URL = stat.URL;
        this.playerTableName = stat.playerTableName;
        this.gameTableName = stat.gameTableName;
        this.uuid = stat.uuid;
        this.wasWinner = stat.wasWinner;
        this.map = stat.map;
        this.gameStats = stat.gameStats;
        this.gameStatsGeneralData = stat.gameStatsGeneralData;
        this.statClass = stat.statClass;
        this.color = stat.color;
    }

    public static void createStatistics(){
        Statistics ls = new Statistics(
                "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/litestrike_db.sql",
                "LsGamesPlayers",
                "LiteStrikeGames",
                "player_uuid",
                "was_winner",
                "map",
                new String[]{"kills", "assists", "deaths", "damage_dealt"},
                null,
                LsStats.class,
                GREEN);
        stats.put("ls", ls);

        Statistics ko = new Statistics(
                "jdbc:sqlite:"+ System.getProperty("user.home")+"/databases/knockoff_db.sql",
                "KoGamesPlayers",
                "KnockoffGames",
                "player_uuid",
                "games_won",
                "map",
                new String[]{"team", "kills", "deaths"},
                null,
                KoStats.class,
                GOLD);
        stats.put("ko", ko);
    }

    public ArrayList<StatItem> getPlayerStats(String alias, OfflinePlayer p, int gameId, boolean isLifetime){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT * FROM " + playerTableName +" WHERE " + uuid + " = ? AND game = ?;");
            if(gameId == -1){
                ArrayList<StatItem> none = new ArrayList<>();
                none.add(new StatItem(null, alias, false));
                return none;
            }
            prep.setInt(2, gameId);
            if(isLifetime){
                prep = conn.prepareStatement("SELECT * FROM " + playerTableName + " WHERE " + uuid +" = ?;");
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
                    units.add(new StatUnit<>(p, data.getColumnLabel(i), 0, alias, isLifetime));
                    continue;
                }
                if (isLifetime && data.getColumnLabel(i).equals("game")) {
                    units.add(new StatUnit<>(p, data.getColumnLabel(i), gameId, alias, isLifetime));
                } else if (isLifetime && data.getColumnLabel(i).equals(wasWinner)) {
                    units.add(new StatUnit<>(p, data.getColumnLabel(i), getPlayerWins(p), alias, isLifetime));
                }else if (isLifetime && fun.apply(set) instanceof Integer) {
                    units.add(new StatUnit<>(p, data.getColumnLabel(i), sumColumns(data.getColumnLabel(i), p), alias, isLifetime));
                }else {
                    units.add(new StatUnit<>(p, data.getColumnLabel(i), fun.apply(set), alias, isLifetime));
                }
            }
            if(isLifetime){
                units.add(new StatUnit<>(p, "percent", calculatePercent(p), alias, isLifetime));
            }else{
                units.add(new StatUnit<>(p, "winner", getTeam(gameId, true), alias, isLifetime));
                (statClass.cast(newInst())).extraNoLifetimeStats(p, units, gameId, isLifetime);
                units.add(new StatUnit<>(p, map, getMap(gameId), alias, isLifetime));
            }
            units.add(new StatUnit<>(p, "name", p.getName(), alias, isLifetime));
            return StatItem.makeItemFromUnit(units, alias, isLifetime);
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get stat data for player: " + p.getName());
            return null;
        }
    }

    public ArrayList<PlayerItem> getGameStats(int page){
        try(Connection conn = DriverManager.getConnection(URL)) {
            int gameId = getGameId(page);
            PreparedStatement prep = conn.prepareStatement(buildGameString());
            prep.setInt(1, gameId);
            ResultSet set = prep.executeQuery();
            ArrayList<PlayerItem> players = new ArrayList<>();
            while(set.next()){
                ByteBuffer bb = ByteBuffer.wrap(set.getBytes(uuid));
                OfflinePlayer player = Bukkit.getOfflinePlayer(new UUID(bb.getLong(), bb.getLong()));

                ItemStack member = new ItemStack(Material.PLAYER_HEAD, 1);
                SkullMeta skull = (SkullMeta) member.getItemMeta();
                PlayerProfile profile = (PlayerProfile) Bukkit.createPlayerProfile(UUID.randomUUID());
                PlayerTextures texture = profile.getTextures();
                texture.setSkin(player.getPlayerProfile().getTextures().getSkin());
                profile.setTextures(texture);
                skull.setPlayerProfile(profile);
                member.setItemMeta(skull);
                member.editPersistentDataContainer(pdc -> pdc.set(new NamespacedKey("crystalized", "profile_holder"), PersistentDataType.STRING, player.getName()));
                ItemMeta meta = member.getItemMeta();
                ArrayList<Component> lore = new ArrayList<>();
                for(String s : gameStats){
                    if(s.equals(wasWinner)) continue;
                    lore.add(StatItem.getItemName(s, false).color(WHITE).append(Component.text(set.getInt(s)).color(WHITE).decoration(ITALIC, false)));
                }
                meta.lore(lore);

                NamedTextColor color = RED;
                String add = "";
                if(set.getInt(wasWinner) == 1){
                    color = GREEN;
                    add = "[w]";
                    players.add(new PlayerItem(member, 1));
                }else{
                    players.add(new PlayerItem(member, 2));
                }
                meta.displayName(Component.text(add + player.getName()).color(color).decoration(ITALIC, false));
                member.setItemMeta(meta);
            }

            ItemStack info = new ItemStack(Material.IRON_BLOCK);
            ItemMeta meta = info.getItemMeta();
            meta.displayName(StatItem.getItemName("game", false).append(Component.text(gameId).color(WHITE).decoration(ITALIC, false)));
            ArrayList<Component> lore = new ArrayList<>();
            lore.add(StatItem.getItemName(map, false).color(WHITE).append(Component.text(getMap(gameId)).color(WHITE).decoration(ITALIC, false)));
            (statClass.cast(newInst())).addExtraGameStats(lore, gameId);
            meta.lore(lore);
            info.setItemMeta(meta);
            players.add(new PlayerItem(info, 0));
            return players;
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get game stats");
            return null;
        }
    }

    public String buildGameString(){
        StringBuilder builder = new StringBuilder("SELECT " + uuid + ", " + wasWinner);
        for(String s : gameStats){
            builder.append(", ").append(s);
        }
        builder.append(" FROM ").append(playerTableName).append(" WHERE game = ?;");
        return builder.toString();
    }

    public Integer getGameId(int page, OfflinePlayer p){
        if(p == null){
            return getGameId(page);
        }
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT game FROM " + playerTableName + " WHERE player_uuid = ? ORDER BY game DESC;");
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

    public Integer getGameId(int page){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT game FROM " + playerTableName + " ORDER BY game DESC;");
            ResultSet set = prep.executeQuery();
            if(!set.next()){
                return -1;
            }
            int total = getTotalGames();
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

    public Integer getTotalGames(OfflinePlayer p){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT COUNT(game) AS game FROM " + playerTableName + " WHERE player_uuid = ?;");
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

    public Integer getTotalGames(){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT COUNT(game) AS game FROM " + playerTableName + ";");
            ResultSet set = prep.executeQuery();
            set.next();
            return set.getInt("game");
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get gameId");
        }
        return -1;
    }

    public ArrayList<String> getTeam(int gameId, boolean won){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT " + uuid + " FROM " + playerTableName + " WHERE " + wasWinner + " = ? AND game = ?;");
            if (won) {
                prep.setInt(1, 1);
            } else {
                prep.setInt(1, 0);
            }
            prep.setInt(2, gameId);
            ResultSet set = prep.executeQuery();
            ArrayList<String> names = new ArrayList<>();
            while(set.next()){
                ByteBuffer bb = ByteBuffer.wrap(set.getBytes(uuid));
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

    public String getMap(int gameId){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT " + map + " FROM " + gameTableName + " WHERE game_id = ?;");
            prep.setInt(1, gameId);
            ResultSet set = prep.executeQuery();
            set.next();
            return set.getString(map);
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get map");
            return null;
        }
    }

    public int sumColumns(String column, OfflinePlayer p){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT SUM(" + column + ") AS sum FROM " + playerTableName + " WHERE " + uuid + " = ?;");
            prep.setBytes(1, LobbyDatabase.uuid_to_bytes(p));
            ResultSet set = prep.executeQuery();
            set.next();
            return set.getInt("sum");
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't sum columns");
            return 0;
        }
    }

    public int getPlayerWins(OfflinePlayer p){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT COUNT(game) AS count FROM " + playerTableName + " WHERE " + uuid + " = ? AND " + wasWinner + " = 1;");
            prep.setBytes(1, LobbyDatabase.uuid_to_bytes(p));
            ResultSet set = prep.executeQuery();
            set.next();
            return set.getInt("count");
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get player wins");
            return 0;
        }
    }

    public String calculatePercent(OfflinePlayer p){
        int total = getTotalGames(p);
        int won = getPlayerWins(p);
        if(total < 10){
            return "Play at least 10 games to see your percentage";
        }
        return ((won / total) * 100) + "%";
    }

    public Object newInst(){
        try{
            return statClass.getConstructor(Statistics.class).newInstance(this);
        }catch(NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e){
            Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
            Bukkit.getLogger().log(Level.SEVERE, "couldn't get constructor");
        }
        return null;
    }
    @Override
    public void extraNoLifetimeStats(OfflinePlayer p, ArrayList<StatUnit<?>> units, int gameId, boolean isLifetime) {}

    @Override
    public void addExtraGameStats(ArrayList<Component> lore, int gameId) {}

    @Override
    public ArrayList<StatUnit<?>[]> organise(ArrayList<StatUnit<?>> units) {return null;}

    @Override
    public ItemStack getBase(StatUnit<?>[] stat) {return null;}
}

interface Methods{
    public void extraNoLifetimeStats(OfflinePlayer p, ArrayList<StatUnit<?>> units, int gameId, boolean isLifetime);
    public void addExtraGameStats(ArrayList<Component> lore, int gameId);
    public ArrayList<StatUnit<?>[]> organise(ArrayList<StatUnit<?>> units);
    public ItemStack getBase(StatUnit<?>[] stat);
}
