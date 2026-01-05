package gg.crystalized.lobby.statistics;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.function.Function;

import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static org.bukkit.Material.COAL;

public class GameDistributor{
    enum types{
        getForGame,
        style,
        getBase,
        organiseForGroup,
        getTotal
    }

    public static Object distribute(types type, String alias, Object obj, boolean b, int i){
        return switch(alias){
            case "ls" -> {
                yield type == types.getForGame ? LsStats.getLsPlayerStats((OfflinePlayer) obj, i, b) :
                type == types.style ? ((Component)obj).color(GREEN) :
                type == types.getBase ? LsGroup.getBase((StatUnit<?>[])obj) :
                type == types.organiseForGroup ? LsGroup.organise((ArrayList<StatUnit<?>>) obj):
                type == types.getTotal ? LsStats.getGameId(-1, (OfflinePlayer)obj) :
                null;
            }
            case "ko" -> {
                //c.color(GOLD)
                yield null;
            }
            case "cb" -> {
                //c.color(LIGHT_PURPLE)
                yield null;
            }
            default -> {
              yield type == types.style ? obj :
              type == types.getBase ? new ItemStack(COAL) :
              null;
            }
        };
    }
}
