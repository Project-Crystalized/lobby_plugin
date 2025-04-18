package gg.crystalized.lobby.minigames;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import gg.crystalized.lobby.Lobby_plugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

import static net.kyori.adventure.text.Component.text;

public class CrystalizedChess implements Listener {

    /*
    https://github.com/bhlangonijr/chesslib
    https://github.com/puffinsoft/jchessify
    Made possible with these libraries

    Heavy work in progress also
    Could be optimised, a lot, this is kinda messy lol

    TODO:
    Game saving for when you leave and come back to the server (Mira's Suggestion)
     */

    public static Player white;
    public static Player black;
    public PlayerTypes CurrentTurn = PlayerTypes.White;
    public UUID ChessID = UUID.randomUUID();
    static Inventory whiteview = Bukkit.getServer().createInventory(null, 54, text("\uA000\uA007 white").color(NamedTextColor.WHITE));
    static Inventory blackview = Bukkit.getServer().createInventory(null, 54, text("\uA000\uA007 black").color(NamedTextColor.WHITE));

    GameStatuses gamestatus = GameStatuses.playing;
    static Board board;

    enum PlayerTypes{
        White,
        Black
    }

    enum GameStatuses{
        playing,
        end,
    }

    public CrystalizedChess(Player w, Player b) {
        white = w;
        black = b;

        board = new Board();
        white.getInventory().clear();
        black.getInventory().clear();

        //board.doMove(new Move(Square.E2, Square.E4));
        white.sendMessage(text("[DEBUG] hello white"));
        black.sendMessage(text("[DEBUG] hello black"));

        UpdateBoard();

        //Main game loop
        new BukkitRunnable() {
            @Override
            public void run() {
                switch (CurrentTurn) {
                    case PlayerTypes.White:

                        break;

                    case PlayerTypes.Black:

                        break;
                }

                //I have no idea how chess works, but I think this is when the game should end, weather the king is attacked and has nowhere to go or a stalemate happens (google it)
                if (board.isStaleMate() || board.isKingAttacked()) {

                }
                if (gamestatus.equals(GameStatuses.end)) {
                    cancel();
                }
            }
        }.runTaskTimer(Lobby_plugin.getInstance(), 0, 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (gamestatus.equals(GameStatuses.end)) {
                    white.closeInventory();
                    black.closeInventory();
                    Bukkit.getServer().sendMessage(text("[Chess] TODO: broadcast results here")); //TODO
                    Lobby_plugin.getInstance().GivePlayerSpawnItems(white);
                    Lobby_plugin.getInstance().GivePlayerSpawnItems(black);
                    Lobby_plugin.getInstance().removeChessGame(ChessID);
                    cancel();
                }
                //UpdateBoard(board, whiteview, blackview);
            }
        }.runTaskTimer(Lobby_plugin.getInstance(), 0, 20);

        new BukkitRunnable() {
            @Override
            public void run() {
                //TODO Temporary
                //gamestatus = GameStatuses.end;
                //cancel();
            }
        }.runTaskTimer(Lobby_plugin.getInstance(), 100, 1);
    }


    private static ItemStack getItemStack(Piece p) {
        switch (p) {
            case Piece.WHITE_PAWN -> {
                ItemStack item = new ItemStack(Material.COAL);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(text("White Pawn"));
                meta.setItemModel(new NamespacedKey("crystalized", "chess/pawn_white"));
                item.setItemMeta(meta);
                return item;
            }
            case Piece.BLACK_PAWN -> {
                ItemStack item = new ItemStack(Material.COAL);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(text("Black Pawn"));
                meta.setItemModel(new NamespacedKey("crystalized", "chess/pawn_black"));
                item.setItemMeta(meta);
                return item;
            }
            case Piece.WHITE_BISHOP -> {
                ItemStack item = new ItemStack(Material.COAL);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(text("White Bishop"));
                meta.setItemModel(new NamespacedKey("crystalized", "chess/bishop_white"));
                item.setItemMeta(meta);
                return item;
            }
            case Piece.BLACK_BISHOP -> {
                ItemStack item = new ItemStack(Material.COAL);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(text("Black Bishop"));
                meta.setItemModel(new NamespacedKey("crystalized", "chess/bishop_black"));
                item.setItemMeta(meta);
                return item;
            }
            case Piece.WHITE_KING -> {
                ItemStack item = new ItemStack(Material.COAL);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(text("White King"));
                meta.setItemModel(new NamespacedKey("crystalized", "chess/king_white"));
                item.setItemMeta(meta);
                return item;
            }
            case Piece.BLACK_KING -> {
                ItemStack item = new ItemStack(Material.COAL);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(text("Black King"));
                meta.setItemModel(new NamespacedKey("crystalized", "chess/king_black"));
                item.setItemMeta(meta);
                return item;
            }
            case Piece.WHITE_KNIGHT -> {
                ItemStack item = new ItemStack(Material.COAL);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(text("White Knight"));
                meta.setItemModel(new NamespacedKey("crystalized", "chess/knight_white"));
                item.setItemMeta(meta);
                return item;
            }
            case Piece.BLACK_KNIGHT -> {
                ItemStack item = new ItemStack(Material.COAL);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(text("Black Knight"));
                meta.setItemModel(new NamespacedKey("crystalized", "chess/knight_black"));
                item.setItemMeta(meta);
                return item;
            }
            case Piece.WHITE_QUEEN -> {
                ItemStack item = new ItemStack(Material.COAL);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(text("White Queen"));
                meta.setItemModel(new NamespacedKey("crystalized", "chess/queen_white"));
                item.setItemMeta(meta);
                return item;
            }
            case Piece.BLACK_QUEEN -> {
                ItemStack item = new ItemStack(Material.COAL);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(text("Black Queen"));
                meta.setItemModel(new NamespacedKey("crystalized", "chess/queen_black"));
                item.setItemMeta(meta);
                return item;
            }
            case Piece.WHITE_ROOK -> {
                ItemStack item = new ItemStack(Material.COAL);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(text("White Rook"));
                meta.setItemModel(new NamespacedKey("crystalized", "chess/rook_white"));
                item.setItemMeta(meta);
                return item;
            }
            case Piece.BLACK_ROOK -> {
                ItemStack item = new ItemStack(Material.COAL);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(text("Black Rook"));
                meta.setItemModel(new NamespacedKey("crystalized", "chess/rook_black"));
                item.setItemMeta(meta);
                return item;
            }
            default -> {
                ItemStack item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(text("Empty Space"));
                item.setItemMeta(meta);
                return item;
            }
        }
    }

    private static void doMove(String from, String to, Board board) {
        board.doMove(new Move(Square.fromValue(from), Square.fromValue(to)));
    }

    //Could be optimised
    public static void UpdateBoard() {
        whiteview.clear();
        blackview.clear();
        Piece[] b = board.boardToArray();

        int i = 0;
        int j = 9;
        for (Piece p : b) {
            if (i > 53 || i == 53) {
                white.getInventory().setItem(j, getItemStack(p));
                black.getInventory().setItem(j, getItemStack(p));
                j++;
                if (j == 17) {
                    j++;
                } else if (j == 26) {
                    //Random empty space for some reason in the board
                    white.getInventory().setItem(26, new ItemStack(Material.AIR));
                    black.getInventory().setItem(26, new ItemStack(Material.AIR));
                }
            } else {
                whiteview.setItem(i, getItemStack(p));
                blackview.setItem(i, getItemStack(p));
                i++;
                switch (i) {
                    case 8, 17, 26, 35, 44 -> {
                        i++;
                    }
                }
            }
        }
        white.openInventory(whiteview);
        black.openInventory(blackview);
    }
}