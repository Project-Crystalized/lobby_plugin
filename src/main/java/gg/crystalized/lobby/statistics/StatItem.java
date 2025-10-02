package gg.crystalized.lobby.statistics;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

enum StatType{
    LS("jdbc:sqlite:" + System.getProperty("user.home") + "/databases/litestrike_db.sql", "LitestrikeGames", "LsGamesPlayers"),
    KO("jdbc:sqlite:" + System.getProperty("user.home") + "/databases/knockoff_db.sql", "KnockoffGames", "KoGamesPlayers"),
    CB("jdbc:sqlite:" + System.getProperty("user.home") + "/databases/crystalblitz_db.sql", "CrystalblitzGames", "CbGamesPlayer");

    final String url;
    final String db;
    final String playerDB;
    StatType(String url, String db, String playerDB){
        this.url = url;
        this.db = db;
        this.playerDB = playerDB;
    }
}
public class StatItem {
    public static ArrayList<StatItem> statItems = new ArrayList<>();
    public ItemStack item;
    public StatType type;
    public String name;
    public Integer gameId;

    public StatItem(StatType type, String name, Integer gameId){
        this.type = type;
        this.name = name;
        this.gameId = gameId;

        //TODO get stuff from database
        ItemStack item = new ItemStack(Material.COAL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("").color(GREEN).decoration(BOLD, true));//TODO make the text
        ArrayList<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        item.setItemMeta(meta);

        this.item = item;
    }
}
