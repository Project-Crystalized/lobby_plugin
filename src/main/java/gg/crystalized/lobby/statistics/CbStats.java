package gg.crystalized.lobby.statistics;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.bukkit.Material.*;
import static org.bukkit.Material.COAL;

public class CbStats extends Statistics{

    public CbStats(Statistics stat) {
        super(stat);
    }

    public String getGameType(int gameId){
        try(Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement prep = conn.prepareStatement("SELECT gametype FROM CrystalBlitzGames WHERE game_id = ?;");
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

    @Override
    public void extraNoLifetimeStats(OfflinePlayer p, ArrayList<StatUnit<?>> units, int gameId, boolean isLifetime){
        units.add(new StatUnit<>(p, "gametype", getGameType(gameId), "cb", isLifetime));
        units.add(new StatUnit<>(p, "other_team", Statistics.stats.get("cb").getTeam(gameId, false), "cb", isLifetime));
    }

    @Override
    public void addExtraGameStats(ArrayList<Component> lore, int gameId){
        lore.add(StatItem.getItemName("gametype", false).append(Component.text(getGameType(gameId)).color(WHITE).decoration(ITALIC, false)));
    }

    @Override
    public ItemStack getBase(StatUnit<?>[] stat){
        if(stat.length == 0){
            return new ItemStack(COAL);
        }
        switch(KoStats.Group.getGroup(stat[0].name, stat[0].isLifetime)){
            case GAMES, GAME:
                return new ItemStack(COMPASS);
            case TEAM:
                return new ItemStack(PUMPKIN_SEEDS);
            case DEATHS:
                return new ItemStack(BONE);
                /*
            case BLOCKS:
                return new ItemStack(AMETHYST_BLOCK);
            case ITEMS:
                ItemStack item = new ItemStack(COAL);
                ItemMeta meta = item.getItemMeta();
                meta.setItemModel(new NamespacedKey("crystalized", "items/cloud_totem"));
                item.setItemMeta(meta);
                return item;
                 */
            default: return new ItemStack(COAL);
        }
    }

    @Override
    public ArrayList<StatUnit<?>[]> organise(ArrayList<StatUnit<?>> units){
        ArrayList<StatUnit<?>[]> fin = new ArrayList<>();
        ArrayList<ArrayList<StatUnit<?>>> arrays = new ArrayList<>();
        boolean lifetime = units.getFirst().isLifetime;
        for(StatUnit<?> unit : units){
            LsStats.Group group = LsStats.Group.getGroup(unit.name, unit.isLifetime);
            if (group == null) {
                continue;
            }
            boolean life = group.isLifetime;
            boolean doesntexist = true;
            for (ArrayList<StatUnit<?>> arr : arrays) {
                if(LsStats.Group.getGroup(arr.getFirst().name, life) == null){
                    continue;
                }
                if (LsStats.Group.getGroup(arr.getFirst().name, life).equals(group)) {
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
        fin = LsStats.Group.sortByPriority(fin);
        return fin;
    }
    enum Group {
        GAMES(new String[]{"name", "game", "games_won", "percent"}, true),
        GAME(new String[]{"game", "gametype","map", "name"}, false),
        TEAM(new String[]{"winner", "other_team"}, false),
        DEATHS(new String[]{"kills", "deaths", "nexus_kills"}, true);
        //BLOCKS(new String[]{"blocks_placed", "blocks_broken"}, true),
        //ITEMS(new String[]{"items_collected", "items_used"}, true);
        final String[] columns;
        final boolean isLifetime;
        Group(String[] columns, boolean isLifetime) {
            this.columns = columns;
            this.isLifetime = isLifetime;
        }

        public static KoStats.Group getGroup(String name, boolean lifetime) {
            KoStats.Group group = group(name, lifetime);
            if(group != null){
                return group;
            }
            group = group(name, !lifetime);
            if(!(!lifetime && group != null && StatItem.notIndividual(group))) {
                return null;
            }
            return group;
        }

        public static KoStats.Group group(String name, boolean lifetime){
            for (KoStats.Group group : KoStats.Group.values()) {
                if (List.of(group.columns).contains(name) && group.isLifetime == lifetime) {
                    return group;
                }
            }
            return null;
        }

        public static ArrayList<StatUnit<?>[]> sortByPriority(ArrayList<StatUnit<?>[]> stat){
            for(StatUnit<?>[] unit : stat){
                if(unit.length == 1 || unit.length == 0){
                    continue;
                }
                KoStats.Group group = getGroup(unit[0].name, unit[0].isLifetime);
                unit = sortWithTemplate(unit, group);
            }

            stat.sort((un1, un2) -> un1.length != 0 && un2.length != 0 ? getGroup(un1[0].name, un1[0].isLifetime).ordinal() > getGroup(un2[0].name, un2[0].isLifetime).ordinal() ? 1 : -1 : 0);
            return stat;
        }

        public static StatUnit<?>[] sortWithTemplate(StatUnit<?>[] unit, KoStats.Group group){
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
    }
}
