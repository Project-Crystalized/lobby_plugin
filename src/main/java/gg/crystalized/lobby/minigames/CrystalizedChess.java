package gg.crystalized.lobby.minigames;

import com.github.bhlangonijr.chesslib.Board;
import gg.crystalized.lobby.Lobby_plugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import static net.kyori.adventure.text.Component.text;

public class CrystalizedChess {
    /*
    https://github.com/bhlangonijr/chesslib
    https://github.com/puffinsoft/jchessify
    Made possible with these libraries (because there's no way im recoding chess from scratch)

    Heavy work in progress also
    I honestly don't know where im going with this lol
     */

    private String CurrentTurn = "white";

    public static void StartChessGame(Player white, Player black) {
        Board board = new Board();

        //board.doMove(new Move(Square.E2, Square.E4));

        new BukkitRunnable() {
            @Override
            public void run() {
                //TODO main game loop
                ShowBoard(white, black);
                cancel();
            }
        }.runTaskTimer(Lobby_plugin.getInstance(), 0, 1);

    }

    private static void ShowBoard(Player white, Player black) {
        Inventory PlayerWhiteView = Bukkit.getServer().createInventory(white, 54, text("White View"));
        Inventory PlayerBlackView = Bukkit.getServer().createInventory(black, 54, text("Black View"));
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