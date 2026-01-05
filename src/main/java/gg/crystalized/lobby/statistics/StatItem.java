package gg.crystalized.lobby.statistics;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.function.Function;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.bukkit.Material.COAL;

public class StatItem {

    public ItemStack item;

    public StatUnit<?>[] stat;

    public StatItem(StatUnit<?>[] stat, String gameAlias, boolean isLifetime){
        this.stat = stat;

        ItemStack item = getBase(gameAlias, stat);
        if(getItemName(0, isLifetime) != null) {
            ItemMeta meta = item.getItemMeta();
            meta.displayName(style(getItemName(0, isLifetime), gameAlias));
            meta.lore(getLore(stat, gameAlias, isLifetime));
            item.setItemMeta(meta);
        }
        this.item = item;
    }

    public Component getItemName(int i, boolean isLifetime){
        if(stat.length == 0){
            return null;
        }
        return switch (stat[i].name) {
            case "placed_bombs" -> Component.text("Bombs placed: ").decoration(ITALIC, false);
            case "broken_bombs" -> Component.text("Bombs broken: ").decoration(ITALIC, false);
            case "kills" -> Component.text("Kills: ").decoration(ITALIC, false);
            case "assists" -> Component.text("Assists: ").decoration(ITALIC, false);
            case "gained_money" -> Component.text("Money earned: ").decoration(ITALIC, false);
            case "spent_money" -> Component.text("Money spent: ").decoration(ITALIC, false);
            case "bought_items" -> Component.text("Items bought: ").decoration(ITALIC, false);
            case "was_winner" -> Component.text("Games won: ").decoration(ITALIC, false);
            case "damage_dealt" -> Component.text("Damage dealt: ").decoration(ITALIC, false);
            case "deaths" -> Component.text("Deaths: ").decoration(ITALIC, false);
            case "did_leave" -> Component.text("Times disconnected: ").decoration(ITALIC, false);
            case "jumps" -> Component.text("Jumps: ").decoration(ITALIC, false);
            case "hits_dealt" -> Component.text("Dealt hits: ").decoration(ITALIC, false);
            case "game_id", "game" -> isLifetime ? Component.text("Games played: ").decoration(ITALIC, false) :  Component.text("Games ID: ").decoration(ITALIC, false);
            case "placer_wins" -> Component.text("Placer wins: ").decoration(ITALIC, false);
            case "breaker_wins" -> Component.text("Breaker wins: ").decoration(ITALIC, false);
            case "map" -> Component.text("Map: ").decoration(ITALIC, false);
            case "winner" -> Component.text("Winner: ").decoration(ITALIC, false);
            case "other_team" -> Component.text("Other team: ").decoration(ITALIC, false);
            case "percent" -> Component.text("Percentage of won games: ").decoration(ITALIC, false);
            case "name" -> Component.text("Player: ").decoration(ITALIC, false);
            default -> Component.text(stat[i].name).decoration(ITALIC, false);
        };
        //TODO names for lifetime
        //TODO ko and cb stuff
    }

    public Component style(Component c, String alias){
        return (Component) GameDistributor.distribute(GameDistributor.types.style, alias, c, false, 0);
    }



    public ArrayList<Component> getLore(StatUnit<?>[] stat, String alias, boolean isLifetime){
        if(stat.length == 0){
            return null;
        }
        ArrayList<Component> lore;
        if(stat[0].value instanceof byte[]){
            if(stat[0].name.contains("bought_items") && alias.equals("ls")){
                lore = LsStats.loreForItems(convertByteToShort((byte[])stat[0].value));
            } else {
                lore = new ArrayList<>();
            }
            return lore;
        }else {
            lore = new ArrayList<>();
            if(stat[0].value instanceof ArrayList){
                ArrayList<?> list = (ArrayList<?>) stat[0].value;
                list.forEach(e -> lore.add(Component.text(e + "").color(WHITE).decoration(ITALIC, false)));
            }
        }
        Component com = Component.text(stat[0].value + "").color(WHITE).decoration(ITALIC, false);
        lore.add(com);
        for(int i = 1; i < stat.length; i++){
            lore.add(style(getItemName(i, isLifetime), alias));
            Component value = Component.text(stat[i].value + "").color(WHITE).decoration(ITALIC, false);
            lore.add(value);
        }
        return lore;
    }

    public static ArrayList<StatItem> makeItemFromUnit(ArrayList<StatUnit<?>> units, String alias, boolean isLifetime){
        ArrayList<StatItem> items = new ArrayList<>();
        for(StatUnit<?>[] stats : StatUnit.organiseForGroup(units, alias)){
            items.add(new StatItem(stats, alias, isLifetime));
        }
        return items;
    }

    public ItemStack getBase(String alias, StatUnit<?>[] stats){
        return (ItemStack) GameDistributor.distribute(GameDistributor.types.getBase, alias, stats, false, 0);
    }

    public static short[] convertByteToShort(byte[] b){
        short[] array = new short[b.length/2];
        int j = 0;
        for(int i = 0; i < b.length; i = i+2){
            ByteBuffer bb = ByteBuffer.allocate(2);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.put(b[i]);
            bb.put(b[i+1]);
            array[j] = bb.getShort();
            j++;
        }
        return array;
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
            case "BLOB", "BYTES":
                f = set -> {
                    try {
                        return set.getBytes(label);
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
                        return set.getFloat(label);
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
        };
        return f;
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

    public static ArrayList<StatUnit<?>[]> organiseForGroup(ArrayList<StatUnit<?>> units, String alias){
        return (ArrayList<StatUnit<?>[]>) GameDistributor.distribute(GameDistributor.types.organiseForGroup, alias, units, false, 0);
    }


}
