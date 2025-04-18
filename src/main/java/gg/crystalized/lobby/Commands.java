package gg.crystalized.lobby;

import gg.crystalized.lobby.minigames.CrystalizedChess;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Commands implements CommandExecutor {

public static List<Pig> pig_trackerA = new ArrayList<Pig>();
public static Map<Player, Integer> player_pig_counters = new HashMap<Player, Integer>();

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        switch (label) {
            case "chess":
                return run_chess(args, commandSender);
            case "pig_hunt":
            case "ph":
                return run_pig_hunt(args, commandSender);
            case "give_xp":
                return give_xp(args, commandSender);
            case "give_money":
                return give_money(args, commandSender);
            default:
                return false;

        }
    }

    private boolean run_pig_hunt(String[] args, CommandSender commandSender) {

        if (commandSender instanceof Player) {
            Location spawn_location = ((Player)commandSender).getLocation();
            Pig pig = spawn_location.getWorld().createEntity(spawn_location, Pig.class);
            pig.registerAttribute(Attribute.SCALE);
            pig.getAttribute(Attribute.SCALE).setBaseValue(0.3);
            pig.spawnAt(spawn_location);
            pig_trackerA.add(pig);

                        }
        Bukkit.getLogger().severe(pig_trackerA.toString());
        return true;

    }

    private boolean run_chess(String[] args, CommandSender commandSender) {
        if (commandSender instanceof Player) {
            if (commandSender == Bukkit.getPlayer(args[0])) {
                commandSender
                        .sendMessage("Chess isn't a singleplayer game, choose someone else who is online currently.");
            } else if (!(Bukkit.getPlayer(args[0]) == null)) {
                // TODO function to ask players if they want to player chess (they can accept or
                // decline)
                // TODO check if player is already in a chess game or another game
                CrystalizedChess chess = new CrystalizedChess((Player) commandSender, Bukkit.getPlayer(args[0]));
            } else {
                commandSender.sendMessage("This player isn't online");
            }
        } else {
            commandSender.sendMessage("Chess game cannot be started by console. Please run this command in-game.");
        }
        return true;
    }

    private boolean give_xp(String[] args, CommandSender sender){
        if(sender instanceof Player){
            LevelManager.giveExperience((Player) sender, Integer.parseInt(args[0]));
            return true;
        }
        return false;
    }

    private boolean give_money(String[] args, CommandSender sender){
        if(sender instanceof Player){
            LevelManager.giveMoney((Player) sender, Integer.parseInt(args[0]));
            return true;
        }
        return false;
    }
}
