package gg.crystalized.lobby.minigames;

import com.github.bhlangonijr.chesslib.Board;
import gg.crystalized.lobby.Lobby_plugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

import static net.kyori.adventure.text.Component.text;

public class CrystalizedChess implements Listener {

    /*
    https://github.com/bhlangonijr/chesslib
    https://github.com/puffinsoft/jchessify
    Made possible with these libraries (because there's no way im recoding chess from scratch)

    Heavy work in progress also
     */

    private final ArrayList Players = new ArrayList();
    private String CurrentTurn = "white";
    private Inventory whiteview = Bukkit.getServer().createInventory(null, 54, text("\uA000\uA001 white null"));
    private Inventory blackview = Bukkit.getServer().createInventory(null, 54, text("\uA000\uA001 black null"));
    private boolean whiteclicked = false;
    private boolean blackclicked = false;

    public CrystalizedChess(Player white, Player black) {

        Players.clear();
        Players.add(white);
        Players.add(black);
        Board board = new Board();

        //board.doMove(new Move(Square.E2, Square.E4));
        white.sendMessage(text("hello white"));
        black.sendMessage(text("hello black"));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!(white.getInventory() == whiteview)) {
                    white.openInventory(whiteview);
                }
                if (!(black.getInventory() == blackview)) {
                    black.openInventory(blackview);
                }


                cancel(); //Temporary, in order to prevent game softlocks
            }
        }.runTaskTimer(Lobby_plugin.getInstance(), 0, 1);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getInventory().equals(whiteview)) {

        } else if (e.getInventory().equals(blackview)) {

        } else {
            return;
        }
    }

    //TODO implement ItemStacks for all the chess pieces
    private static ItemStack getChessPiece(String Piece) {
        switch (Piece) {
            case "white_pawn":
                ItemStack wpawn = new ItemStack(Material.IRON_INGOT);
                ItemMeta wpawnim = wpawn.getItemMeta();
                wpawnim.displayName(text("White Pawn"));
                wpawn.setItemMeta(wpawnim);
                return wpawn;
            case "black_pawn":
                ItemStack bpawn = new ItemStack(Material.NETHERITE_INGOT);
                ItemMeta bpawnim = bpawn.getItemMeta();
                bpawnim.displayName(text("Black Pawn"));
                bpawn.setItemMeta(bpawnim);
                return bpawn;
            default:
                ItemStack UnknownPiece = new ItemStack(Material.BARRIER);
                ItemMeta UnknownPieceim = UnknownPiece.getItemMeta();
                UnknownPieceim.displayName(text("Unknown Piece. This is a bug pls report thx"));
                UnknownPiece.setItemMeta(UnknownPieceim);
                return UnknownPiece;
        }
    }
}