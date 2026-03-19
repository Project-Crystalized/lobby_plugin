package gg.crystalized.lobby.statistics;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.function.Function;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
public class StatItem {

    public ItemStack item;

    public StatUnit<?>[] stat;

    public StatItem(StatUnit<?>[] stat, String gameAlias, boolean isLifetime){
        this.stat = stat;
        ItemStack item = makeNoneItem();
        if(stat != null && getItemName(stat[0].name, isLifetime) != null) {
            item = Statistics.stats.get(gameAlias).statClass.cast(Statistics.stats.get(gameAlias).newInst()).getBase(stat);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(style(getItemName(stat[0].name, isLifetime), gameAlias));
            meta.lore(getLore(stat, gameAlias, isLifetime));
            item.setItemMeta(meta);
        }
        this.item = item;
    }

    public static Component getItemName(String s, boolean isLifetime){
        return switch (s) {
            case "placed_bombs" -> Component.translatable("crystalized.shardcore.stats.placed_bombs").decoration(ITALIC, false);
            case "broken_bombs" -> Component.translatable("crystalized.shardcore.stats.broken_bombs").decoration(ITALIC, false);
            case "kills" -> Component.translatable("crystalized.shardcore.stats.kills").decoration(ITALIC, false);
            case "assists" -> Component.translatable("crystalized.shardcore.stats.assists").decoration(ITALIC, false);
            case "gained_money" -> Component.translatable("crystalized.shardcore.stats.gained_money").decoration(ITALIC, false);
            case "spent_money" -> Component.translatable("crystalized.shardcore.stats.spent_money").decoration(ITALIC, false);
            case "bought_items" -> Component.translatable("crystalized.shardcore.stats.bought_items").decoration(ITALIC, false);
            case "was_winner", "games_won" -> Component.translatable("crystalized.shardcore.stats.games_won").decoration(ITALIC, false);
            case "damage_dealt" -> Component.translatable("crystalized.shardcore.stats.damage_dealt").decoration(ITALIC, false);
            case "deaths" -> Component.translatable("crystalized.shardcore.stats.deaths").decoration(ITALIC, false);
            case "did_leave" -> Component.translatable("crystalized.shardcore.stats.did_leave").decoration(ITALIC, false);
            case "jumps" -> Component.translatable("crystalized.shardcore.stats.jumps").decoration(ITALIC, false);
            case "hits_dealt" -> Component.translatable("crystalized.shardcore.stats.hits_dealt").decoration(ITALIC, false);
            case "game_id", "game" -> isLifetime ? Component.translatable("crystalized.shardcore.stats.games_played").decoration(ITALIC, false) :  Component.translatable("crystalized.shardcore.stats.game_id").decoration(ITALIC, false);
            case "placer_wins" -> Component.translatable("crystalized.shardcore.stats.placer_wins").decoration(ITALIC, false);
            case "breaker_wins" -> Component.translatable("crystalized.shardcore.stats.breaker_wins").decoration(ITALIC, false);
            case "map" -> Component.translatable("crystalized.shardcore.stats.map").decoration(ITALIC, false);
            case "winner" -> Component.translatable("crystalized.shardcore.stats.winner").decoration(ITALIC, false);
            case "other_team" -> Component.translatable("crystalized.shardcore.stats.other_team").decoration(ITALIC, false);
            case "percent" -> Component.translatable("crystalized.shardcore.stats.percent").decoration(ITALIC, false);
            case "name" -> Component.translatable("crystalized.shardcore.stats.name").decoration(ITALIC, false);
            case "blocks_broken" -> Component.translatable("crystalized.shardcore.stats.blocks_broken").decoration(ITALIC, false);
            case "blocks_placed" -> Component.translatable("crystalized.shardcore.stats.blocks_placed").decoration(ITALIC, false);
            case "items_collected" -> Component.translatable("crystalized.shardcore.stats.items_collected").decoration(ITALIC, false);
            case "items_used" -> Component.translatable("crystalized.shardcore.stats.items_used").decoration(ITALIC, false);
            case "gametype" -> Component.translatable("crystalized.shardcore.stats.gametype").decoration(ITALIC, false);
            default -> Component.text(s).decoration(ITALIC, false);
        };
        //TODO cb stuff
        //TODO translations
    }

    public Component style(Component c, String alias){
        Statistics sta = Statistics.stats.get(alias);
        return c.color(sta.color);
    }

    public ItemStack makeNoneItem(){
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.translatable("crystalized.shardcore.stats.never_played").color(RED).decoration(ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    public ArrayList<Component> getLore(StatUnit<?>[] stat, String alias, boolean isLifetime){
        if(stat.length == 0){
            return null;
        }
        ArrayList<Component> lore;
        if(stat[0].value instanceof byte[]){
            if(stat[0].name.contains("bought_items") && alias.equals("ls")){
                lore = LsStats.loreForItems(convertByteToShort((byte[]) stat[0].value));
            } else {
                lore = new ArrayList<>();
            }
            return lore;
        }else {
            lore = new ArrayList<>();
            if (stat[0].value instanceof ArrayList){
                ArrayList<?> list = (ArrayList<?>) stat[0].value;
                list.forEach(e -> lore.add(Component.text(e + "").color(WHITE).decoration(ITALIC, false)));
                for(int i = 1; i < stat.length; i++){
                    list = (ArrayList<?>) stat[i].value;
                    lore.add(style(getItemName(stat[i].name, isLifetime), alias));
                    list.forEach(e -> lore.add(Component.text(e + "").color(WHITE).decoration(ITALIC, false)));
                }
                return lore;
            }
        }
        Component com = Component.text(stat[0].value + "").color(WHITE).decoration(ITALIC, false);
        lore.add(com);
        for(int i = 1; i < stat.length; i++){
            lore.add(style(getItemName(stat[i].name, isLifetime), alias));
            Component value = Component.text(stat[i].value + "").color(WHITE).decoration(ITALIC, false);
            lore.add(value);
        }
        return lore;
    }

    public static ArrayList<StatItem> makeItemFromUnit(ArrayList<StatUnit<?>> units, String alias, boolean isLifetime) {
        ArrayList<StatItem> items = new ArrayList<>();
        for (StatUnit<?>[] stats : Statistics.stats.get(alias).statClass.cast(Statistics.stats.get(alias).newInst()).organise(units)) {
            items.add(new StatItem(stats, alias, isLifetime));
        }
        return items;
    }

    public static short[] convertByteToShort(byte[] b){
        short[] s = new short[b.length/2];
        int j = 0;
        for(int i = 0; i < b.length; i = i+2){
            short sh = ByteBuffer.wrap(new byte[]{b[i], b[i+1]}).getShort();
            s[j] = sh;
            j++;
        }
        return s;
    }

    public static Function<ResultSet, ?> getMethod(String s, String label){
        Function<ResultSet, ?> f;
        switch (s){
            case "INTEGER":
                f = set -> {
                    try {
                        return set.getInt(label);
                    } catch (SQLException e) {
                        Bukkit.getLogger().severe(e.getMessage());
                        throw new RuntimeException();
                    }
                };
            case "BLOB":
                f = set -> {
                    try {
                        return set.getBlob(label);
                    } catch (SQLException e) {
                        Bukkit.getLogger().severe(e.getMessage());
                        throw new RuntimeException();
                    }
                };
            case "DOUBLE":
                f = set -> {
                    try {
                        return set.getDouble(label);
                    } catch (SQLException e) {
                        Bukkit.getLogger().severe(e.getMessage());
                        throw new RuntimeException();
                    }
                };
            case "STRING":
                f = set -> {
                    try {
                        return set.getString(label);
                    } catch (SQLException e) {
                        Bukkit.getLogger().severe(e.getMessage());
                        throw new RuntimeException();
                    }
                };
            case "REAL":
                f = set -> {
                    try {
                        return Math.floor(set.getFloat(label));
                    } catch (SQLException e) {
                        Bukkit.getLogger().severe(e.getMessage());
                        throw new RuntimeException();
                    }
                };
            case "BYTES":
                f = set ->{
                    try {
                        return set.getBytes(label);
                    } catch (SQLException e) {
                        Bukkit.getLogger().severe(e.getMessage());
                        throw new RuntimeException();
                    }
                };
            default:
                f = set -> {
                    try {
                        return set.getObject(label);
                    } catch (SQLException e) {
                        Bukkit.getLogger().severe(e.getMessage());
                        throw new RuntimeException();
                    }
                };
        }
        return f;
    }

    public static boolean notIndividual(Object o){
        return !o.equals(LsStats.Group.GAMES) && !o.equals(KoStats.Group.GAMES);
    }
}

class StatUnit<T> {
    OfflinePlayer p;
    public String name;
    public T value;
    public String gameAlias;
    public boolean isLifetime;
    public StatUnit(OfflinePlayer p, String name, T value, String gameAlias, boolean lifetime){
        this.p = p;
        this.name = name;
        this.value = value;
        this.gameAlias = gameAlias;
        this.isLifetime = lifetime;
    }

    public static StatUnit<?>[] toArray(ArrayList<StatUnit<?>> list){
        StatUnit<?>[] array = new StatUnit[list.size()];
        for(int i = 0; i < array.length; i++){
            array[i] = list.get(i);
        }
        return array;
    }
}

class PlayerItem{
    public ItemStack item;
    public int team;

    public PlayerItem(ItemStack item, int team){
        this.item = item;
        this.team = team;
    }
}
