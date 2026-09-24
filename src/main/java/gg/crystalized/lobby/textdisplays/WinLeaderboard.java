package gg.crystalized.lobby.textdisplays;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;

import gg.crystalized.lobby.Leaderboards;
import gg.crystalized.lobby.Lobby_plugin;
import gg.crystalized.lobby.Nametag;
import gg.crystalized.lobby.Ranks;
import gg.crystalized.lobby.parkour.Parkour;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import static gg.crystalized.lobby.parkour.Parkour.findParkourByLeaderboard;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class WinLeaderboard {
	public static HashMap<Player, HashMap<String, Integer>> leaderboards = new HashMap<>();
	boolean lastErrorLogged = false;

	static class LeaderboardSnapshot {
		Component sharedText;
		HashMap<UUID, int[]> player_stats;
		int total;

		LeaderboardSnapshot(Component base, HashMap<UUID, int[]> stats, int total){
			this.sharedText = base;
			this.player_stats = stats;
			this.total = total;
		}
	}

	public WinLeaderboard(String game_type, Location loc) {
		new BukkitRunnable() {
			@Override
			public void run() {
				LeaderboardSnapshot snap = computeSnapshot(game_type, loc);
				for(Player p : Bukkit.getOnlinePlayers()) {
					User user = PacketEvents.getAPI().getPlayerManager().getUser(p);
					if(user == null) continue;

					leaderboards.computeIfAbsent(p, k -> new HashMap<>());
					if(!leaderboards.get(p).containsKey(game_type)){
						createDisplay(user, p, loc, game_type);
					}

					Component text = buildText(p, snap, loc, game_type);

					int lb_entity_id = leaderboards.get(p).get(game_type);
					user.sendPacket(displayMetadata(lb_entity_id, text));
				}
			}
		}.runTaskTimer(Lobby_plugin.getInstance(), 20, (20 * 10));
	}

	static Component buildText(Player p, LeaderboardSnapshot snap, Location loc, String type){
		int[] own = snap.player_stats.get(p.getUniqueId());
		if(own == null){
			return snap.sharedText;
		}
		Parkour parkour = null;
		if(type.contains("pk")) {
			parkour = findParkourByLeaderboard(loc);
		}
		Component rows = snap.sharedText.append(text("\n")).append(text("-----------------").color(GRAY));
		Component num = Leaderboards.get_styles(own[0]);
		String num_str = PlainTextComponentSerializer.plainText().serialize(num);
		int padding = snap.total - (Leaderboards.balance(num_str) + Leaderboards.balance(PlainTextComponentSerializer.plainText().serialize(Ranks.getName(p))) + Leaderboards.balance("" + own[1]));
		String dots = ".".repeat(padding);
		rows = rows.append(text("\n")).append(num);
		rows = rows.append(Ranks.getName(p)).append(text(dots).color(GRAY));
		int wins = own[1];
		if(parkour == null) {
			rows = rows.append(text("" + wins)).color(GREEN);
		}else{
			int hours = wins / 3600000;
			int minutes = (wins % 3600000) / 60000;
			int seconds = (wins % 3600000 % 60000) / 1000;
			int millis = wins % 3600000 % 60000 % 1000;
			rows = rows.append(text(gg.crystalized.lobby.parkour.Timer.buildTimer(millis, seconds, minutes, hours))).color(GREEN);
		}
		return rows;
	}

	static void createDisplay(User user, Player p, Location loc, String type){
		int id = Nametag.EntityId;
		leaderboards.get(p).put(type, id);
		Nametag.EntityId++;
		WrapperPlayServerSpawnEntity entity = new WrapperPlayServerSpawnEntity(id, UUID.randomUUID(), EntityTypes.TEXT_DISPLAY, new com.github.retrooper.packetevents.protocol.world.Location
				(loc.getX(), loc.getY(), loc.getZ(), 0, 0), 0, 0, new Vector3d());
		user.sendPacket(entity);
	}

	static WrapperPlayServerEntityMetadata displayMetadata(int entityId, Component text) {
		List<EntityData<?>> data = List.of(new EntityData<>(15, EntityDataTypes.BYTE, (byte) 3),
				new EntityData<Component>(23, EntityDataTypes.ADV_COMPONENT, text),
				new EntityData<Integer>(25, EntityDataTypes.INT, 1345466930),
				new EntityData<Byte>(27, EntityDataTypes.BYTE, (byte) 1));
		return new WrapperPlayServerEntityMetadata(entityId, data);
	}

	LeaderboardSnapshot computeSnapshot(String type, Location loc){
		GameType t = GameType.findType(type);
		if(t == null){
			return new LeaderboardSnapshot(text("null"), new HashMap<>(), 0);
		}
		try (Connection conn = DriverManager.getConnection(t.url)) {
			PreparedStatement query = conn.prepareStatement("SELECT player_uuid, SUM(" + t.dbColumn + ") FROM " + t.dbName + " GROUP BY player_uuid ORDER BY SUM(" + t.dbColumn + ") DESC;");
			Parkour parkour = null;
			Component base = text("Game Leaderboard\n").color(GOLD).append(t.title);
			if(t == GameType.PARKOUR){
				parkour = findParkourByLeaderboard(loc);
				query = conn.prepareStatement("SELECT player_uuid, SUM(" + t.dbColumn + ") FROM " + t.dbName + " WHERE course = ? GROUP BY player_uuid ORDER BY SUM(" + t.dbColumn + ") ASC;");
				query.setString(1, parkour.name);
				base = base.append(text(parkour.name).color(parkour.color).decoration(BOLD, true));
			}
			ResultSet res = query.executeQuery();

			ArrayList<TextComponent> topKey = new ArrayList<>();
			HashMap<TextComponent, Integer> top = new HashMap<>();
			HashMap<UUID, int[]> stats = new HashMap<>();

			int h = 0;
			String longest = "";
			while (res.next()) {
				UUID uuid = Leaderboards.convertBytesToUUID(res.getBytes("player_uuid"));
				TextComponent name = (TextComponent)Ranks.getName(Bukkit.getOfflinePlayer(uuid));
				String name_str = PlainTextComponentSerializer.plainText().serialize(name);
				if(top.containsKey(name)){
					continue;
				}
				h++;
				int wins = res.getInt("SUM(" + t.dbColumn + ")");

				if(Leaderboards.balance(name_str) > Leaderboards.balance(longest)){
					longest = name_str;
				}
				stats.put(uuid, new int[]{h, wins});

				if(h <= 10){
					topKey.add(name);
					top.put(name, wins);
				}
			}

			lastErrorLogged = false;
			int total = Leaderboards.balance(longest + "......" + "10000000000");
			for(int j = 0; j <= topKey.size()-1; j++){
				String top_str = PlainTextComponentSerializer.plainText().serialize(topKey.get(j));
				Component num = Leaderboards.get_styles(j+1);
				String num_str = PlainTextComponentSerializer.plainText().serialize(num);
				int padding = total - (Leaderboards.balance(num_str) + Leaderboards.balance(top_str) + Leaderboards.balance("" + top.get(topKey.get(j))));
				//Bukkit.getLogger().warning(type + ": " + top.get(topKey.get(j)).content());
				String dots = ".".repeat(padding);
				base = base.append(text("\n")).append(num);
				base = base.append(topKey.get(j)).append(text(dots).color(GRAY));
				int wins = top.get(topKey.get(j));
				if(parkour == null) {
					base = base.append(text("" + wins)).color(GREEN);
				}else{
					int hours = wins / 3600000;
					int minutes = (wins % 3600000) / 60000;
					int seconds = (wins % 3600000 % 60000) / 1000;
					int millis = wins % 3600000 % 60000 % 1000;
					base = base.append(text(gg.crystalized.lobby.parkour.Timer.buildTimer(millis, seconds, minutes, hours))).color(GREEN);
				}
			}

			return new LeaderboardSnapshot(base, stats, total);
		} catch (SQLException e) {
			Component fallbackBase = text("Game Leaderboard\n").color(GOLD).append(t.title);
			try (Connection conn = DriverManager.getConnection(t.url)) {
				ResultSet count = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM " + t.dbName);
				if(count.next() && count.getInt("c") > 0){
					if(!lastErrorLogged){
						Bukkit.getLogger().warning("Leaderboard error (" + type + "): " + e);
						lastErrorLogged = true;
					}
					fallbackBase = fallbackBase.append(text("\n").append(text("Leaderboard Error").color(RED)));
				}
			} catch (SQLException ignored) {
			}
			return new LeaderboardSnapshot(fallbackBase, new HashMap<>(), 0);
		}
	}

	enum GameType{
		LS("ls", Leaderboards.LS_URL, "LsGamesPlayers", "was_winner", text("LITESTRIKE\n").color(GREEN).decoration(BOLD, true)),
		KO("ko", Leaderboards.KO_URL, "KoGamesPlayers", "games_won", text("KNOCKOFF\n").color(GOLD).decoration(BOLD, true)),
		CB("cb", Leaderboards.CB_URL, "CbGamesPlayers", "games_won", text("CRYSTAL BLITZ\n").color(LIGHT_PURPLE).decoration(BOLD, true)),
		PARKOUR("pk", Leaderboards.LOBBY_URL, "ParkourTimes", "best_time", text("").color(WHITE).decoration(BOLD, true));

		final String key;
		final String url;
		final String dbName;
		final String dbColumn;
		final Component title;

		GameType(String key, String url, String dbName, String dbColumn, Component title){
			this.key = key;
			this.url = url;
			this.dbName = dbName;
			this.dbColumn = dbColumn;
			this.title = title;
		}

		public static GameType findType(String type){
			for(GameType t : GameType.values()){
				if(t.key.equals(type) || (Objects.equals(t.key, "pk") && t.key.contains("pk"))){
					return t;
				}
			}
			return null;
		}
	}
}
