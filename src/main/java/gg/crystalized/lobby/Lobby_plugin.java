package gg.crystalized.lobby;

import com.github.retrooper.packetevents.PacketEvents;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import gg.crystalized.lobby.minigames.CrystalizedChess;
import gg.crystalized.lobby.minigames.CrystalizedChessListener;
import gg.crystalized.lobby.statistics.StatView;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;

import static gg.crystalized.lobby.statistics.Statistics.createStatistics;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.UNDERLINED;


public final class Lobby_plugin extends JavaPlugin implements PluginMessageListener{

	List<CrystalizedChess> ChessGames = new ArrayList<>();
	public boolean passive_mode = false;


	@Override
	public void onLoad(){
		PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
		PacketEvents.getAPI().getSettings().reEncodeByDefault(false).checkForUpdates(true).bStats(false);
		PacketEvents.getAPI().load();
	}
	@Override
	public void onEnable() {
		LobbyDatabase.setup_databases();
		new LobbyConfig();
		this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		this.getServer().getPluginManager().registerEvents(new LevelManager(), this);
		this.getServer().getPluginManager().registerEvents(new CrystalizedChessListener(), this);
		this.getServer().getPluginManager().registerEvents(new InventoryManager(), this);
		this.getServer().getPluginManager().registerEvents(new EntityRefresh(), this);
		this.getServer().getPluginManager().registerEvents(new StatView(null, "ls"), this);
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "crystalized:main");
		this.getServer().getMessenger().registerIncomingPluginChannel(this, "crystalized:main", this);

		Commands dc = new Commands();
		this.getCommand("set_rank").setExecutor(dc);
		Cosmetic.createCosmetics();
		PacketEvents.getAPI().init();

		//this needs to load in game servers - Callum
		Achievement.getAchievementsFromJson();

		if(Lobby_plugin.getInstance().passive_mode){
			return;
		}

		EntityRefresh.setupEntityRefresh();
		createStatistics();

		this.getCommand("chess").setExecutor(dc);
		this.getCommand("pig_hunt").setExecutor(dc);
		this.getCommand("give_xp").setExecutor(dc);
		this.getCommand("give_money").setExecutor(dc);
		this.getCommand("spawn").setExecutor(dc);
		this.getCommand("reload_cosmetics").setExecutor(dc);
		this.getCommand("reload_achievements").setExecutor(dc);

		doLobbyMessages();
		saveResource("achievements.json", true);
	}

	@Override
	public void onDisable() {
		Bukkit.getOnlinePlayers().forEach(p -> p.getPassengers().forEach(Entity::remove));
		PacketEvents.getAPI().terminate();
	}

	public static Lobby_plugin getInstance() {
		return getPlugin(Lobby_plugin.class);
	}


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
		}else if(message1.equals("Settings") && !passive_mode){
			if(in.readUTF().equals("player_visibility")){
				Setting.updatePlayerVisibility(player);
			}
		}else if(message1.equals("Online") && !passive_mode){
			String p = in.readUTF();
			int i = in.readInt();
			if(p.equals("a")){
				ScoreboardManager.onlinePlayers = i;
				return;
			}
			if(i == 1){
				FriendsMenu.areOnline.put(p, true);
			}else{
				FriendsMenu.areOnline.put(p, false);
			}
		}
	}

	private static void doLobbyMessages(){
		Component divider = Component.text("------------------------------------").color(WHITE);
		Component[] messages = new Component[]{
			Component.text("\nFound any bugs?").color(LIGHT_PURPLE).append(Component.text("\nReport them in our Discord!").color(WHITE)).append(Component.text("\nhttps://discord.gg/saAGYcncd7\n").color(LIGHT_PURPLE).decoration(UNDERLINED, true).clickEvent(ClickEvent.openUrl("https://discord.gg/saAGYcncd7"))),
			Component.text("\nShine as bright as the sun!").color(TextColor.fromHexString(Ranks.sun_sub.color)).append(Component.text("\nBuy the " + Ranks.sun_sub.iconWithName + " rank in our Shop").color(WHITE)).append(Component.text("\ncrystalized.cc\n").color(LIGHT_PURPLE).decoration(UNDERLINED, true).clickEvent(ClickEvent.openUrl("crystalized.cc"))),
			Component.text("\nStay safe online!").color(DARK_RED).append(Component.text("\nCrystalized Staff will never ask for your personal information.\n").color(WHITE)),
			Component.text("\nConnect with the community").color(LIGHT_PURPLE).append(Component.text("\nJoin our Discord!").color(WHITE)).append(Component.text("\nhttps://discord.gg/saAGYcncd7\n").color(LIGHT_PURPLE).decoration(UNDERLINED, true).clickEvent(ClickEvent.openUrl("https://discord.gg/saAGYcncd7"))),
			Component.text("\nShimmer like the moon!").color(TextColor.fromHexString(Ranks.moon_one.color)).append(Component.text("\nBuy the " + Ranks.moon_one.iconWithName + " rank in our Shop").color(WHITE)).append(Component.text("\ncrystalized.cc\n").color(LIGHT_PURPLE).decoration(UNDERLINED, true).clickEvent(ClickEvent.openUrl("crystalized.cc"))),
			Component.text("\nWant to join the staff?").color(LIGHT_PURPLE).append(Component.text("\nApply now in our Discord").color(WHITE)).append(Component.text("\nhttps://discord.gg/saAGYcncd7\n").color(LIGHT_PURPLE).decoration(UNDERLINED, true).clickEvent(ClickEvent.openUrl("https://discord.gg/saAGYcncd7")))
		};

		new BukkitRunnable(){
			int i = 0;
			public void run(){
				if(i == messages.length) i = 0;
				for(Player p : Bukkit.getOnlinePlayers()){
					p.setSaturation(20);
					p.sendMessage(divider.append(messages[i]).append(divider));
				}
				i++;
			}
		}.runTaskTimer(getInstance(), 5, 600 * 20);
	}
}
