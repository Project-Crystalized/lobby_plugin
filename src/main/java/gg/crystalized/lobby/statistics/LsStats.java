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
import org.bukkit.profile.PlayerTextures;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.*;
import java.util.function.Function;

import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.bukkit.Material.*;

public class LsStats extends Statistics{
    static String URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/litestrike_db.sql";
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

    public LsStats(Statistics stat) {
        super(stat);
    }


    @Override
    public void extraNoLifetimeStats(OfflinePlayer p, ArrayList<StatUnit<?>> units, int gameId, boolean isLifetime){
        units.add(new StatUnit<>(p, "other_team", Statistics.stats.get("ls").getTeam(gameId, false), "ls", isLifetime));
    }

    @Override
    public void addExtraGameStats(ArrayList<Component> lore, int gameId){
        lore.add(StatItem.getItemName("placer_wins", false).color(WHITE).append(Component.text(getWins(gameId, true)).color(WHITE).decoration(ITALIC, false)));
        lore.add(StatItem.getItemName("breaker_wins", false).color(WHITE).append(Component.text(getWins(gameId, false)).color(WHITE).decoration(ITALIC, false)));
    }

    @Override
    public ArrayList<StatUnit<?>[]> organise(ArrayList<StatUnit<?>> units){
        ArrayList<StatUnit<?>[]> fin = new ArrayList<>();
        ArrayList<ArrayList<StatUnit<?>>> arrays = new ArrayList<>();
        boolean lifetime = units.getFirst().isLifetime;
        for(StatUnit<?> unit : units){
            Group group = Group.getGroup(unit.name, unit.isLifetime);
            if (group == null) {
                continue;
            }
            boolean life = group.isLifetime;
            boolean doesntexist = true;
            for (ArrayList<StatUnit<?>> arr : arrays) {
                if(Group.getGroup(arr.getFirst().name, life) == null){
                    continue;
                }
                if (Group.getGroup(arr.getFirst().name, life).equals(group)) {
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
        fin = Group.sortByPriority(fin);
        return fin;
    }

    public static int getWins(int gameId, boolean placer){
        try(Connection conn = DriverManager.getConnection(URL)) {
            String type = "breaker_wins";
            if(placer){
                type = "placer_wins";
            }
            PreparedStatement prep = conn.prepareStatement("SELECT " + type + " FROM LiteStrikeGames WHERE game_id = ?;");
            prep.setInt(1, gameId);
            ResultSet set = prep.executeQuery();
            set.next();
            return set.getInt(type);
        }catch(SQLException e){
            Bukkit.getLogger().warning(e.getMessage());
            Bukkit.getLogger().warning("couldn't get map");
            return 0;
        }
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

    public ItemStack getBase(StatUnit<?>[] stat){
        if(stat.length == 0){
            return new ItemStack(COAL);
        }
        switch(Group.getGroup(stat[0].name, stat[0].isLifetime)){
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
            case TEAM:
                return new ItemStack(PUMPKIN_SEEDS);
            default: return new ItemStack(COAL);
        }
    }

    enum Group {
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
        Group(String[] columns, boolean isLifetime) {
            this.columns = columns;
            this.isLifetime = isLifetime;
        }

        public static Group getGroup(String name, boolean lifetime) {
            Group group = group(name, lifetime);
            if(group != null){
                return group;
            }
            group = group(name, !lifetime);
            if(!(!lifetime && group != null && StatItem.notIndividual(group))) {
                return null;
            }
            return group;
        }

        public static Group group(String name, boolean lifetime){
            for (Group group : Group.values()) {
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
                Group group = getGroup(unit[0].name, unit[0].isLifetime);
                unit = sortWithTemplate(unit, group);
            }

            stat.sort((un1, un2) -> un1.length != 0 && un2.length != 0 ? getGroup(un1[0].name, un1[0].isLifetime).ordinal() > getGroup(un2[0].name, un2[0].isLifetime).ordinal() ? 1 : -1 : 0);
            return stat;
        }

        public static StatUnit<?>[] sortWithTemplate(StatUnit<?>[] unit, Group group){
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

