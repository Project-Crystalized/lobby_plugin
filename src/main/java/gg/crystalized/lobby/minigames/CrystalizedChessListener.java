package gg.crystalized.lobby.minigames;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import gg.crystalized.lobby.Lobby_plugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import static net.kyori.adventure.text.Component.text;

public class CrystalizedChessListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        CrystalizedChess chess = Lobby_plugin.getInstance().getChessGame((Player) e.getWhoClicked());
        if (chess == null) {return;}
        Player p = (Player) e.getWhoClicked();
        switch (chess.CurrentTurn) {
            case CrystalizedChess.PlayerTypes.White:
                if (p == chess.white) {

                    chess.gamestatus = CrystalizedChess.GameStatuses.end;

                } else {
                    p.sendMessage(text("not your move, wait for your turn"));
                    e.setCancelled(true);
                }
                break;
            case CrystalizedChess.PlayerTypes.Black:
                if (p == chess.black) {

                } else {
                    p.sendMessage(text("not your move, wait for your turn"));
                    e.setCancelled(true);
                }
                break;
            default:
                break;
        }
    }

    //TODO replace the gamestatus enum with a method to end the game with a reason that takes a string/component
    //Make sure it doesn't make a giant StackOverflowException :)

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        CrystalizedChess chess = Lobby_plugin.getInstance().getChessGame((Player) e.getPlayer());
        if (chess == null) {return;}
        Player p = (Player) e.getPlayer();
        chess.gamestatus = CrystalizedChess.GameStatuses.end;
        //TODO message for intentionally escaping
    }

    @EventHandler
    public void onPlayerLeave(PlayerConnectionCloseEvent e) {
        CrystalizedChess chess = Lobby_plugin.getInstance().getChessGame(e.getPlayerName());
        if (chess == null) {return;}
        chess.gamestatus = CrystalizedChess.GameStatuses.end;
        //TODO message for opponent Alt+F4-ing
    }
}
