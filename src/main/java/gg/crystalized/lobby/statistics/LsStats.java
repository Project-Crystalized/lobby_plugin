package gg.crystalized.lobby.statistics;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class LsStats {

    public static HashMap<String, Number> calculateLifetime(OfflinePlayer p){
        ArrayList<HashMap<String, Object>> data = StatsDatabase.getLsPlayerStats(p);
        Set<String> set = data.getFirst().keySet();
        HashMap<String, Number> done = new HashMap<>();
        for(String s : set){
            if(!(data.getFirst().get(s) instanceof Number)){
                continue;
            }

            double n = 0;

            for(HashMap<String, Object> map : data){
                if(map.get(s) == null){
                    continue;
                }

                n = n + (Double)map.get(s);
            }
            done.put(s, n);
        }
        done.put("games", data.size());
        if(data.size() >= 10){
            done.put("won_percent", (Double)done.get("was_winner")/data.size());
        }
        return done;
    }

    public static ItemStack[] createLifetimeItems(OfflinePlayer p){

        ItemStack games = new ItemStack(Material.COAL);
        ItemMeta games_meta = games.getItemMeta();
        games_meta.displayName(Component.text("Total games played: ").color(GREEN).decoration(BOLD, true));
        ArrayList<Component> games_lore = new ArrayList<>();
        games_lore.add(Component.text(""));
        //TODO
        return null;
    }
}
