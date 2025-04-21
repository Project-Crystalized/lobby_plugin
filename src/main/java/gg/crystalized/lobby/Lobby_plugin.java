package gg.crystalized.lobby;

import gg.crystalized.lobby.minigames.CrystalizedChess;
import gg.crystalized.lobby.minigames.CrystalizedChessListener;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import static net.kyori.adventure.text.Component.text;

public final class Lobby_plugin extends JavaPlugin{

	List<CrystalizedChess> ChessGames = new ArrayList<>();

	@Override
	public void onEnable() {
		this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		this.getServer().getPluginManager().registerEvents(new LevelManager(), this);
		this.getServer().getPluginManager().registerEvents(new CrystalizedChessListener(), this);
		this.getServer().getPluginManager().registerEvents(new InvListener(), this);
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "crystalized:main");
		new Leaderboards();

		Commands dc = new Commands();
		this.getCommand("chess").setExecutor(dc);
		this.getCommand("pig_hunt").setExecutor(dc);
		this.getCommand("give_xp").setExecutor(dc);
		this.getCommand("give_money").setExecutor(dc);
		new RankDisplay();

		LobbyDatabase.setup_databases();
	}

	@Override
	public void onDisable() {
	}

	public static Lobby_plugin getInstance() {
		return getPlugin(Lobby_plugin.class);
	}


	//This will be called in this class *and* when a minigames.CrystalizedChess game ends
	/*
	public void GivePlayerSpawnItems(Player p) {
		p.getInventory().clear();

		ItemStack item = new ItemStack(Material.EMERALD);
		ItemMeta meta = item.getItemMeta();
		meta.customName(text("Placeholder item for shardcore nexus (this does nothing atm)"));
		meta.setItemModel(new NamespacedKey("crystalized", "shardcorenexus3/blue"));
		item.setItemMeta(meta);
		p.getInventory().setItem(4, item);
	}

	 */

	public void newChessGame(Player white, Player black) {
		CrystalizedChess chess = new CrystalizedChess(white, black);
		ChessGames.add(chess);
	}

	public void removeChessGame(UUID id) {
		for (CrystalizedChess chess : ChessGames) {
			if (chess.ChessID == id) {
				ChessGames.remove(chess);
				return;
			}
		}

		getLogger().log(Level.WARNING, "[CrystalizedChess] removeChessGame failed with ID \"" + id + "\", This may impact future chess games unless this server restarts.");
	}

	public CrystalizedChess getChessGame(Player player) {
		for (CrystalizedChess chess : ChessGames) {
			if (chess.white.equals(player)) {
				return chess;
			}
		}
		for (CrystalizedChess chess : ChessGames) {
			if (chess.black.equals(player)) {
				return chess;
			}
		}

		//TODO if we implement a debug mode, make this message appear when thats active
		//getLogger().log(Level.WARNING, "[CrystalizedChess] getChessGame(Player) failed with player \"" + player.getName() + "\", This may or may not be safe to ignore depending on where this was called. Returning Null.");
		return null;
	}

	//fuck PlayerConnectionCloseEvent for not exposing a player object and instead a string
	public CrystalizedChess getChessGame(String player) {
		for (CrystalizedChess chess : ChessGames) {
			if (chess.white.getName().equals(player)) {
				return chess;
			}
		}
		for (CrystalizedChess chess : ChessGames) {
			if (chess.black.getName().equals(player)) {
				return chess;
			}
		}

		//TODO if we implement a debug mode, make this message appear when thats active
		//getLogger().log(Level.WARNING, "[CrystalizedChess] getChessGame(String) failed with player \"" + player + "\", This may or may not be safe to ignore depending on where this was called. Returning Null.");
		return null;
	}
}
