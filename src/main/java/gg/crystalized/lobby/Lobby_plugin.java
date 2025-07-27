package gg.crystalized.lobby;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import gg.crystalized.lobby.minigames.CrystalizedChess;
import gg.crystalized.lobby.minigames.CrystalizedChessListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;


public final class Lobby_plugin extends JavaPlugin implements PluginMessageListener{

	List<CrystalizedChess> ChessGames = new ArrayList<>();
	public boolean passive_mode = false;
	@Override
	public void onEnable() {
		LobbyDatabase.setup_databases();
		new LobbyConfig();
		this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		this.getServer().getPluginManager().registerEvents(new LevelManager(), this);
		this.getServer().getPluginManager().registerEvents(new CrystalizedChessListener(), this);
		this.getServer().getPluginManager().registerEvents(new InventoryManager(), this);
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "crystalized:main");
		this.getServer().getMessenger().registerIncomingPluginChannel(this, "crystalized:main", this);

		Commands dc = new Commands();
		this.getCommand("set_rank").setExecutor(dc);

		if(Lobby_plugin.getInstance().passive_mode){
			return;
		}

		new Leaderboards();


		this.getCommand("chess").setExecutor(dc);
		this.getCommand("pig_hunt").setExecutor(dc);
		this.getCommand("give_xp").setExecutor(dc);
		this.getCommand("give_money").setExecutor(dc);

		new RankDisplay();


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

	@Override
	public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
		ArrayList<String> members = new ArrayList<>();
		if (!channel.equals("crystalized:main")) {
			return;
		}
		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String message1 = in.readUTF();
		if (message1.equals("Party")) {
			while(true){
				try {
					members.add(in.readUTF());
				}catch(Exception e){
					FriendsMenu.placePartyMembers(members, player, FriendsMenu.waitingForPartyMembers.get(player));
					FriendsMenu.waitingForPartyMembers.remove(player);
					return;
				}
			}
		}
	}
}
