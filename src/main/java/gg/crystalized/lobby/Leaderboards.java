package gg.crystalized.lobby;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import static gg.crystalized.lobby.Leaderboards.LS_URL;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class Leaderboards {
	public static final String LS_URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/litestrike_db.sql";
	public static final String KO_URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/knockoff_db.sql";
	public static final String CB_URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/crystalblitz_db.sql";

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

	public WinLeaderboard(String type, Location loc) {
		/*
		for(Player p : Bukkit.getOnlinePlayers()) {
			createDisplay(p, loc, type);
		}
		 */
		new BukkitRunnable() {

			@Override
			public void run() {
				for(Player p : Bukkit.getOnlinePlayers()) {
					Optional<TextDisplay> opt = loc.getNearbyEntitiesByType(TextDisplay.class, 2.0).stream().findAny();
					if(!opt.isPresent()){
						createDisplay(p, loc, type);
						continue;
					}
					TextDisplay	display = opt.get();
					display.teleport(loc);
					display.text(generateText(p, type));
					display.setShadowed(true);
					display.setBillboard(Billboard.CENTER);
					display.setBackgroundColor(Color.fromARGB(80, 50, 50, 50));
					for (Player player : Bukkit.getOnlinePlayers()) {
						player.hideEntity(Lobby_plugin.getInstance(), display);
					}
					p.showEntity(Lobby_plugin.getInstance(), display);
				}
			}
		}.runTaskTimer(Lobby_plugin.getInstance(), 1, (20 * 10));
	}

	static void createDisplay(Player p, Location loc, String type){
		TextDisplay display = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
		display.teleport(loc);
		display.text(generateText(p, type));
		display.setShadowed(true);
		display.setBillboard(Billboard.CENTER);
		display.setBackgroundColor(Color.fromARGB(80, 50, 50, 50));
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.hideEntity(Lobby_plugin.getInstance(), display);
		}
		p.showEntity(Lobby_plugin.getInstance(), display);
	}

	public static Component generateText(Player p, String type){
		GameType t = GameType.findType(type);

		if(t == null){
			return text("null");
		}
		try (Connection conn = DriverManager.getConnection(t.url)) {
			String query = "SELECT player_uuid, SUM(" + t.dbColumn + ") FROM " + t.dbName + " GROUP BY player_uuid ORDER BY SUM(" + t.dbColumn + ") DESC;";
			ResultSet res = conn.createStatement().executeQuery(query);

			Component leaderboard_rows = text("Game Leaderboard\n").color(GOLD).append(t.title);
			ArrayList<Integer> topKey = new ArrayList<>();
			HashMap<Integer, TextComponent> top = new HashMap<>();
			Integer rank = null;
			Integer win = null;

			int h = 0;
			TextComponent longest = text("");
			while (res.next()) {
				UUID uuid = Leaderboards.convertBytesToUUID(res.getBytes("player_uuid"));
				TextComponent name = (TextComponent)Ranks.getName(Bukkit.getOfflinePlayer(uuid));
				if(top.containsValue(name)){
					continue;
				}
				h++;
				int wins = res.getInt("SUM(" + t.dbColumn + ")");

				if(balance(name.content()) > balance(longest.content())){
					longest = name;
				}

				if(Bukkit.getOfflinePlayer(uuid).equals(p)){
					win = wins;
					rank = h;
					if(balance(((TextComponent)Ranks.getName(p)).content()) > balance(longest.content())){
						longest = (TextComponent)Ranks.getName(p);
					}
				}

				if(h <= 10){
					topKey.add(wins);
					top.put(wins, name);
				}
			}

			int total = balance(longest.content() + "......");
			for(int j = 0; j <= topKey.size()-1; j++){
				int padding = total - (balance(top.get(topKey.get(j)).content()) + balance("" + topKey.get(j)));
				String dots = ".".repeat(padding);
				Component num = Leaderboards.get_styles(j+1);
				leaderboard_rows = leaderboard_rows.append(text("\n")).append(num);
				leaderboard_rows = leaderboard_rows.append(top.get(topKey.get(j))).append(text(dots).color(GRAY));
				leaderboard_rows = leaderboard_rows.append(text("" + topKey.get(j))).color(GREEN);
			}

			if(rank == null){
				return leaderboard_rows;
			}

			leaderboard_rows = leaderboard_rows.append(text("\n")).append(text("-----------------").color(GRAY));
			int padding = total - (balance(((TextComponent)Ranks.getName(p)).content()) + balance("" + win));
			String dots = ".".repeat(padding);
			Component num = Leaderboards.get_styles(rank);
			leaderboard_rows = leaderboard_rows.append(text("\n")).append(num);
			leaderboard_rows = leaderboard_rows.append(Ranks.getName(p)).append(text(dots).color(GRAY));
			leaderboard_rows = leaderboard_rows.append(text("" + win)).color(GREEN);

			return leaderboard_rows;
		} catch (SQLException e) {
			//Bukkit.getLogger().warning("error opening database: " + e);
			return null;
		}
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
