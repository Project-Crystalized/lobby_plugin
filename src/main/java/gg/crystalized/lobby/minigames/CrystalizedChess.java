package gg.crystalized.lobby.minigames;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
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
    Could be optimised, a lot, this is kinda messy lol
     */

    private final ArrayList Players = new ArrayList();
    private String CurrentTurn = "white";
    private Inventory whiteview = Bukkit.getServer().createInventory(null, 54, text("\uA000\uA001 white"));
    private Inventory blackview = Bukkit.getServer().createInventory(null, 54, text("\uA000\uA001 black"));

    // 0 = move from, 1 = move to
    private int SelectionType = 0;

    //Can be "playing" (playing), "end" (game just ended)
    private String gamestatus = "playing";

    public CrystalizedChess(Player white, Player black) {

        Players.clear();
        Players.add(white);
        Players.add(black);
        Board board = new Board();
        white.getInventory().clear();
        black.getInventory().clear();

        //board.doMove(new Move(Square.E2, Square.E4));
        white.sendMessage(text("[DEBUG] hello white"));
        black.sendMessage(text("[DEBUG] hello black"));

        //Main game loop
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!(white.getInventory() == whiteview)) {
                    white.openInventory(whiteview);
                }
                if (!(black.getInventory() == blackview)) {
                    black.openInventory(blackview);
                }

                switch (CurrentTurn) {
                    case "white":

                        break;

                    case "black":

                        break;
                }

                //I have no idea how chess works, but I think this is when the game should end, weather the king is attacked and has nowhere to go or a stalemate happens (google it)
                if (board.isStaleMate() || board.isKingAttacked()) {

                }
                if (gamestatus.equals("end")) {
                    cancel();
                }
            }
        }.runTaskTimer(Lobby_plugin.getInstance(), 0, 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (gamestatus.equals("end")) {
                    white.closeInventory();
                    black.closeInventory();
                    cancel();
                }
                UpdateBoard(board, whiteview, blackview);
            }
        }.runTaskTimer(Lobby_plugin.getInstance(), 0, 10);

        new BukkitRunnable() {
            @Override
            public void run() {
                //Temporary, in order to prevent client softlocks
                gamestatus = "end";
                cancel();
            }
        }.runTaskTimer(Lobby_plugin.getInstance(), 100, 1);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        switch (CurrentTurn) {
            case "white":
                if (p.getInventory() == whiteview) {

                } else {
                    p.sendMessage(text("not your move, wait for your turn"));
                    e.setCancelled(true);
                }
                break;
            case "black":
                if (p.getInventory() == blackview) {

                } else {
                    p.sendMessage(text("not your move, wait for your turn"));
                    e.setCancelled(true);
                }
                break;
            default:
                break;
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

    private static void doMove(String from, String to, Board board) {
        board.doMove(new Move(Square.fromValue(from), Square.fromValue(to)));
    }

    private static void UpdateBoard(Board board, Inventory whiteview, Inventory blackview) {
        Piece[] boardstring = board.boardToArray();
        whiteview.clear();
        blackview.clear();
        Bukkit.getServer().sendMessage(text("[chesslib_debug] \n" + boardstring.toString()));
    }
}