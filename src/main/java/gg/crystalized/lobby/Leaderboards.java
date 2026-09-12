package gg.crystalized.lobby;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.*;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class Leaderboards {
	public static final String LS_URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/litestrike_db.sql";
	public static final String KO_URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/knockoff_db.sql";
	public static final String CB_URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/crystalblitz_db.sql";
	public static final String LOBBY_URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/lobby_db.sql";

	public Leaderboards() {
		//new WinLeaderboard("ko");
		//new WinLeaderboard("ls");
	}

	public static UUID convertBytesToUUID(byte[] bytes) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		long high = byteBuffer.getLong();
		long low = byteBuffer.getLong();
		return new UUID(high, low);
	}

	public static String convertToSmallCaps(int number) {
		String[] suffixes = { "th. ", "st. ", "nd. ", "rd. " };
		String[] exceptions = { "11", "12", "13" };

		String numberStr = String.valueOf(number);
		String suffix = suffixes[0]; // default suffix

		// check if the number is in the exceptions list
		for (String exception : exceptions) {
			if (numberStr.endsWith(exception)) {
				suffix = suffixes[0]; // use "th" for exceptions
				break;
			}
		}

		// check the last digit of the number
		int lastDigit = number % 10;
		if (lastDigit == 1 && !numberStr.endsWith("11")) {
			suffix = suffixes[1]; // use "st" for numbers ending with 1 (except 11)
		} else if (lastDigit == 2 && !numberStr.endsWith("12")) {
			suffix = suffixes[2]; // use "nd" for numbers ending with 2 (except 12)
		} else if (lastDigit == 3 && !numberStr.endsWith("13")) {
			suffix = suffixes[3]; // use "rd" for numbers ending with 3 (except 13)
		}

		// convert the number to small caps
		String smallCapsNumber = numberStr.substring(0, 1).toUpperCase() + numberStr.substring(1).toLowerCase();

		return smallCapsNumber + suffix;
	}

	public static Component get_styles(int i) {
		Component s = text(convertToSmallCaps(i));
		switch (i) {
			case 1:
				s = s.color(GREEN).decoration(BOLD, true);
				break;
			case 2:
				s = s.color(YELLOW);
				break;
			case 3:
				s = s.color(RED);
				break;
		}
		return s;
	}
}

class WinLeaderboard {
	static HashMap<Player, HashMap<String, Integer>> leaderboards = new HashMap<>();
	boolean lastErrorLogged = false;

	static class LeaderboardSnapshot {
		Component base;
		HashMap<UUID, int[]> stats;
		int total;

		LeaderboardSnapshot(Component base, HashMap<UUID, int[]> stats, int total){
			this.base = base;
			this.stats = stats;
			this.total = total;
		}
	}

	public WinLeaderboard(String type, Location loc) {
		new BukkitRunnable() {
			@Override
			public void run() {
				LeaderboardSnapshot snap = computeSnapshot(type);
				for(Player p : Bukkit.getOnlinePlayers()) {
					leaderboards.computeIfAbsent(p, k -> new HashMap<>());
					if(!leaderboards.get(p).containsKey(type)){
						createDisplay(p, loc, type, snap);
						continue;
					}
					Component text = buildText(p, snap);
					Integer num = 3;
					Integer one = 1;
					List<EntityData<?>> data = List.of(new EntityData(15, EntityDataTypes.BYTE, num.byteValue()), new EntityData(23, EntityDataTypes.ADV_COMPONENT, text), new EntityData(25, EntityDataTypes.INT, 1345466930), new EntityData(27, EntityDataTypes.BYTE, one.byteValue()));
					WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(leaderboards.get(p).get(type), data);
					User user = PacketEvents.getAPI().getPlayerManager().getUser(p);
					if(user != null) {
						user.sendPacket(metadata);
					}
				}
			}
		}.runTaskTimer(Lobby_plugin.getInstance(), 1, (20 * 10));
	}

	static void createDisplay(Player p, Location loc, String type, LeaderboardSnapshot snap){
		int id = Nametag.EntityId;
		leaderboards.get(p).put(type, id);
		Nametag.EntityId++;
		WrapperPlayServerSpawnEntity entity = new WrapperPlayServerSpawnEntity(id, UUID.randomUUID(), EntityTypes.TEXT_DISPLAY, new com.github.retrooper.packetevents.protocol.world.Location
				(loc.getX(), loc.getY(), loc.getZ(), 0, 0), 0, 0, new Vector3d());
		PacketEvents.getAPI().getPlayerManager().getUser(p).sendPacket(entity);

		Integer num = 3;
		Integer one = 1;
		List<EntityData<?>> data = List.of(new EntityData(15, EntityDataTypes.BYTE, num.byteValue()), new EntityData(23, EntityDataTypes.ADV_COMPONENT, buildText(p, snap)), new EntityData(25, EntityDataTypes.INT, 1345466930), new EntityData(27, EntityDataTypes.BYTE, one.byteValue()));
		WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(id, data);
		User user = PacketEvents.getAPI().getPlayerManager().getUser(p);
		if(user != null) {
			PacketEvents.getAPI().getPlayerManager().getUser(p).sendPacket(metadata);
		}
	}

	LeaderboardSnapshot computeSnapshot(String type){
		GameType t = GameType.findType(type);
		if(t == null){
			return new LeaderboardSnapshot(text("null"), new HashMap<>(), 0);
		}
		try (Connection conn = DriverManager.getConnection(t.url)) {
			String query = "SELECT player_uuid, SUM(" + t.dbColumn + ") FROM " + t.dbName + " GROUP BY player_uuid ORDER BY SUM(" + t.dbColumn + ") DESC;";
			ResultSet res = conn.createStatement().executeQuery(query);

			Component base = text("Game Leaderboard\n").color(GOLD).append(t.title);
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

				if(balance(name_str) > balance(longest)){
					longest = name_str;
				}
				stats.put(uuid, new int[]{h, wins});

				if(h <= 10){
					topKey.add(name);
					top.put(name, wins);
				}
			}

			lastErrorLogged = false;
			int total = balance(longest + "......" + "1000000");
			for(int j = 0; j <= topKey.size()-1; j++){
				String top_str = PlainTextComponentSerializer.plainText().serialize(topKey.get(j));
				Component num = Leaderboards.get_styles(j+1);
				String num_str = PlainTextComponentSerializer.plainText().serialize(num);
				int padding = total - (balance(num_str) + balance(top_str) + balance("" + top.get(topKey.get(j))));
				//Bukkit.getLogger().warning(type + ": " + top.get(topKey.get(j)).content());
				String dots = ".".repeat(padding);
				base = base.append(text("\n")).append(num);
				base = base.append(topKey.get(j)).append(text(dots).color(GRAY));
				base = base.append(text("" + top.get(topKey.get(j)))).color(GREEN);
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

	static Component buildText(Player p, LeaderboardSnapshot snap){
		int[] own = snap.stats.get(p.getUniqueId());
		if(own == null){
			return snap.base;
		}
		Component rows = snap.base.append(text("\n")).append(text("-----------------").color(GRAY));
		Component num = Leaderboards.get_styles(own[0]);
		String num_str = PlainTextComponentSerializer.plainText().serialize(num);
		int padding = snap.total - (balance(num_str) + balance(PlainTextComponentSerializer.plainText().serialize(Ranks.getName(p))) + balance("" + own[1]));
		String dots = ".".repeat(padding);
		rows = rows.append(text("\n")).append(num);
		rows = rows.append(Ranks.getName(p)).append(text(dots).color(GRAY));
		rows = rows.append(text("" + own[1])).color(GREEN);
		return rows;
	}

	String get_small_cap_num(int i) {
		switch (i) {
			case 0:
				return "𝟢";
			case 1:
				return "𝟣";
			case 2:
				return "𝟤";
			case 3:
				return "𝟥";
			case 4:
				return "𝟦";
			case 5:
				return "𝟧";
			case 6:
				return "𝟨";
			case 7:
				return "𝟩";
			case 8:
				return "𝟪";
			case 9:
				return "𝟫";
			default:
				return "";
		}
	}

	public static int balance(String name) {
		char[] chars = name.toCharArray();
		int sum = 0;
		for (char c : chars) {
			sum += BitmapGlyphInfo.getBitmapGlyphInfo(c).width;
		}
		sum += name.length() - 1;
		return sum / 2;
	}

	enum GameType{
		LS("ls", Leaderboards.LS_URL, "LsGamesPlayers", "was_winner", text("LITESTRIKE\n").color(GREEN).decoration(BOLD, true)),
		KO("ko", Leaderboards.KO_URL, "KoGamesPlayers", "games_won", text("KNOCKOFF\n").color(GOLD).decoration(BOLD, true)),
		CB("cb", Leaderboards.CB_URL, "CbGamesPlayers", "games_won", text("CRYSTAL BLITZ\n").color(LIGHT_PURPLE).decoration(BOLD, true));

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
				if(t.key.equals(type)){
					return t;
				}
			}
			return null;
		}
	}
}