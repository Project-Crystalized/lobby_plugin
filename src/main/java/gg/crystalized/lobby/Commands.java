package gg.crystalized.lobby;

import gg.crystalized.lobby.minigames.CrystalizedChess;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
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
            case "set_rank":
                return set_rank(args, commandSender);
            case "spawn":
                return spawn(commandSender);
            default:
                return false;

        }
    }

    private boolean run_pig_hunt(String[] args, CommandSender commandSender) {
        if(Lobby_plugin.getInstance().passive_mode){
            return false;
        }
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
        if(Lobby_plugin.getInstance().passive_mode){
            return false;
        }
        if (commandSender instanceof Player) {
            if (commandSender == Bukkit.getPlayer(args[0])) {
                commandSender
                        .sendMessage("Chess isn't a singleplayer game, choose someone else who is online currently.");
            } else if (!(Bukkit.getPlayer(args[0]) == null)) {
                // TODO function to ask players if they want to player chess (they can accept or decline)
                // TODO check if player is already in a chess game or another game
                Lobby_plugin.getInstance().newChessGame((Player) commandSender, Bukkit.getPlayer(args[0]));
            } else {
                commandSender.sendMessage("This player isn't online");
            }
        } else {
            commandSender.sendMessage("Chess game cannot be started by console. Please run this command in-game.");
        }
        return true;
    }

    private boolean give_xp(String[] args, CommandSender sender){
        if(Lobby_plugin.getInstance().passive_mode){
            return false;
        }
        if(sender instanceof Player){
            LevelManager.giveExperience((Player) sender, Integer.parseInt(args[0]));
            return true;
        }
        return false;
    }

    private boolean give_money(String[] args, CommandSender sender){
        if(Lobby_plugin.getInstance().passive_mode){
            return false;
        }
        if(sender instanceof Player){
            LevelManager.giveMoney((Player) sender, Integer.parseInt(args[0]));
            return true;
        }
        return false;
    }

    private boolean set_rank(String[] args, CommandSender sender) {
        if (sender instanceof Player) {
            return false;
        }

        if (args.length < 2) {
            return false;
        }

        OfflinePlayer p = Bukkit.getServer().getOfflinePlayer(args[0]);

        if(p == null){
            return false;
        }

        int rank = 0;

        if(args[1].equals("rankless")){
            rank = 0;
        }else if(args[1].equals("admin")){
            rank = 1;
        } else if(args[1].equals("mod")){
            rank = 2;
        } else if(args[1].equals("dev")){
            rank = 3;
        } else if(args[1].equals("contrib")){
            rank = 4;
        } else if(args[1].equals("sub-project")){
            rank = 5;
        } else if(args[1].equals("one-time-payment")){
            rank = 6;
        } else if(args[1].equals("subscription")){
            rank = 7;
        }

        if(rank != 6 && rank != 7){
            LobbyDatabase.setRank(p, rank);
        }else{
            LobbyDatabase.setPayedRank(p, rank);
        }

        if(p.isOnline() && !Lobby_plugin.getInstance().passive_mode) {
            Ranks.renderTabList(p.getPlayer());
            Ranks.renderNameTags(p.getPlayer());
        }
        return true;
    }

    private boolean spawn(CommandSender sender){
        if(!(sender instanceof Player)){
            return false;
        }

        if(Lobby_plugin.getInstance().passive_mode){
            return false;
        }

        ((Player)sender).teleport(LobbyConfig.Locations.get("spawn"), TeleportFlag.EntityState.RETAIN_PASSENGERS);
        return true;
    }
}
