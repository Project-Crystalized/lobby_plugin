package gg.crystalized.lobby;

import gg.crystalized.lobby.minigames.CrystalizedChess;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Commands implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command,  @NotNull String label, @NotNull String[] args) {

        switch (label) {
            case "chess":
                return run_chess(args, commandSender);
            default:
                return false;
        }
    }

    private boolean run_chess(String[] args,CommandSender commandSender) {
        if (commandSender instanceof Player) {
            if (commandSender == Bukkit.getPlayer(args[0])) {
                commandSender.sendMessage("Chess isn't a singleplayer game, choose someone else who is online currently.");
            } else if (!(Bukkit.getPlayer(args[0]) == null)) {
                //TODO function to ask players if they want to player chess (they can accept or decline)
                //TODO check if player is already in a chess game or another game
                CrystalizedChess chess = new CrystalizedChess((Player) commandSender, Bukkit.getPlayer(args[0]));
            } else {
                commandSender.sendMessage("This player isn't online");
            }
        } else {
            commandSender.sendMessage("Chess game cannot be started by console. Please run this command in-game.");
        }
        return true;
    }
}
