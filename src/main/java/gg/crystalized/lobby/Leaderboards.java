package gg.crystalized.lobby;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.*;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.Component.text;

public class Leaderboards {
	public static final String LS_URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/litestrike_db.sql";
	public static final String KO_URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/knockoff_db.sql";
	public static final String CB_URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/databases/crystalblitz_db.sql";

	public Leaderboards() {
		new WinLeaderboard(Bukkit.getWorld("world"), "ko");
		new WinLeaderboard(Bukkit.getWorld("world"), "ls");
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
				s = s.color(GREEN).decoration(TextDecoration.BOLD, true);
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
	public static final String LS_DB = "LsGamesPlayers";
	public static final String KO_DB = "KoGamesPlayers";
	public static final String CB_DB = "CbGamesPlayers";

	public static final Component LS_TITLE = text("Game Leaderboard\n").color(GOLD).append(text("LITESTRIKE").color(GREEN));
	public static final Component KO_TITLE = text("Game Leaderboard\n").color(GOLD).append(text("KNOCKOFF").color(GOLD));
	public static final Component CB_TITLE = text("Game Leaderboard\n").color(LIGHT_PURPLE).append(text("CRYSTAL BLITZ").color(LIGHT_PURPLE));

	public WinLeaderboard(World w, String type) {
		Location loc = null;
		// FYI, pitch and yaw doesn't matter when the leaderboard rotates automatically
		// based on how the client looks at it
		switch (type) {
			case "ls":
				loc = LobbyConfig.Locations.get("ls-leaderboard");
				break;
			case "ko":
				loc = LobbyConfig.Locations.get("ko-leaderboard");
				break;
			case "cb":
				loc = LobbyConfig.Locations.get("cb-leaderboard");
				break;
		}

		// Location loc = new Location(w, -19, -57.5, -110.5, 90.0f, 26.565f);
		Location finalLoc = loc;
		for(Player p : Bukkit.getOnlinePlayers()) {
			TextDisplay display = (TextDisplay) finalLoc.getWorld().spawnEntity(finalLoc, EntityType.TEXT_DISPLAY);
			display.teleport(finalLoc);
			display.text(generateText(p, type));
			display.setShadowed(true);
			display.setBillboard(Billboard.CENTER);
			display.setBackgroundColor(Color.fromARGB(80, 50, 50, 50));
			for (Player player : Bukkit.getOnlinePlayers()) {
				player.hideEntity(Lobby_plugin.getInstance(), display);
			}
			p.showEntity(Lobby_plugin.getInstance(), display);
		}
		new BukkitRunnable() {

			@Override
			public void run() {

				try {
					for(Player p : Bukkit.getOnlinePlayers()) {
						TextDisplay display = finalLoc.getNearbyEntitiesByType(TextDisplay.class, 2.0).stream().findAny().get();
						display.teleport(finalLoc);
						display.text(generateText(p, type));
						display.setShadowed(true);
						display.setBillboard(Billboard.CENTER);
						display.setBackgroundColor(Color.fromARGB(80, 50, 50, 50));
						for (Player player : Bukkit.getOnlinePlayers()) {
							player.hideEntity(Lobby_plugin.getInstance(), display);
						}
						p.showEntity(Lobby_plugin.getInstance(), display);
					}
				} catch (NoSuchElementException e) {
				}
			}
		}.runTaskTimer(Lobby_plugin.getInstance(), (20 * 5), (20 * 10));
	}

	private Component generate_ls_text() {

		String query = "SELECT player_uuid, SUM(was_winner) FROM LsGamesPlayers GROUP BY player_uuid ORDER BY SUM(was_winner) DESC LIMIT 10;";
		try (Connection conn = DriverManager.getConnection(Leaderboards.LS_URL)) {
			ResultSet res = conn.createStatement().executeQuery(query);
			Component leaderbaord_rows = text("Game Leaderboard\n").color(GOLD).append(text("LITESTRIKE").color(GREEN));
			int i = 0;
			while (res.next()) {
				i++;
				UUID uuid = Leaderboards.convertBytesToUUID(res.getBytes("player_uuid"));
				Component name = Ranks.getName(Bukkit.getOfflinePlayer(uuid));
				Component num = Leaderboards.get_styles(i);
				leaderbaord_rows = leaderbaord_rows.append(text("\n")).append(num);
				leaderbaord_rows = leaderbaord_rows.append(name).append(text(" - ").color(GRAY));
				leaderbaord_rows = leaderbaord_rows.append(text("" + res.getInt("SUM(was_winner)")).color(GREEN));
			}
			return leaderbaord_rows;
		} catch (SQLException e) {
			//Bukkit.getLogger().warning("error opening database: " + e);
			return null;
		}
	}

	private Component generate_ko_text() {
		// was_winner -> games_won
		// TODO this is completely invisible in-game, no idea why so that needs to be
		// sorted out
		// Knockoff's Database code for reference
		// https://github.com/Project-Crystalized/knockoff-game/blob/main/src/main/java/gg/knockoff/game/KnockoffDatabase.java

		String query = "SELECT player_uuid, SUM(games_won) FROM KoGamesPlayers GROUP BY player_uuid ORDER BY SUM(games_won) DESC LIMIT 10;";
		try (Connection conn = DriverManager.getConnection(Leaderboards.KO_URL)) {
			ResultSet res = conn.createStatement().executeQuery(query);
			Component leaderbaord_rows = text("Game Leaderboard\n").color(GOLD).append(text("KNOCKOFF").color(GOLD));
			int i = 0;
			while (res.next()) {
				i++;
				UUID uuid = Leaderboards.convertBytesToUUID(res.getBytes("player_uuid"));
				Component name = Ranks.getName(Bukkit.getOfflinePlayer(uuid));
				Component num = Leaderboards.get_styles(i);
				leaderbaord_rows = leaderbaord_rows.append(text("\n")).append(num);
				leaderbaord_rows = leaderbaord_rows.append(name).append(text(" - ").color(GRAY));
				leaderbaord_rows = leaderbaord_rows.append(text("" + res.getInt("SUM(games_won)")).color(GREEN));
			}
			return leaderbaord_rows;
		} catch (SQLException e) {
			//Bukkit.getLogger().warning("error opening database: " + e);
			return null;
		}
	}

	private Component generate_cb_text() {
		// was_winner -> games_won
		// TODO this is completely invisible in-game, no idea why so that needs to be
		// sorted out

		String query = "SELECT player_uuid, SUM(games_won) FROM CbGamesPlayers GROUP BY player_uuid ORDER BY SUM(games_won) DESC LIMIT 10;";
		try (Connection conn = DriverManager.getConnection(Leaderboards.CB_URL)) {
			ResultSet res = conn.createStatement().executeQuery(query);
			Component leaderbaord_rows = text("Game Leaderboard\n").color(LIGHT_PURPLE).append(text("CRYSTAL BLITZ").color(LIGHT_PURPLE));
			NavigableMap<Integer, TextComponent> names = Collections.emptyNavigableMap();

			while (res.next()) {
				UUID uuid = Leaderboards.convertBytesToUUID(res.getBytes("player_uuid"));
				Component name = Ranks.getName(Bukkit.getOfflinePlayer(uuid));
				int wins = res.getInt("SUM(games_won)");
				names.put(wins, (TextComponent)name);
			}

			names.descendingMap();
			TextComponent longest = names.firstEntry().getValue();
			Integer key = names.firstKey();
			for(int j = 1; j <= names.size(); j++){
				if(balance(names.lowerEntry(key).getValue().content()) > balance(longest.content())){
					longest = names.lowerEntry(key).getValue();
				}
				key = names.lowerKey(key);
			}

			key = names.firstKey();
			int total = balance(longest.content()) + 3;

			int i = 0;
			for(int j = 1; j <= names.size(); j++){
				i++;
				int padding = total - (balance(names.get(key).content()) + balance("" + key));
				String dots = ".".repeat(padding);
				Component num = Leaderboards.get_styles(i);
				leaderbaord_rows = leaderbaord_rows.append(text("\n")).append(num);
				leaderbaord_rows = leaderbaord_rows.append(names.get(key)).append(text(dots).color(GRAY));
				leaderbaord_rows = leaderbaord_rows.append(text("" + key)).color(GREEN);
				key = names.lowerKey(key);
			}
			return leaderbaord_rows;
		} catch (SQLException e) {
			//Bukkit.getLogger().warning("error opening database: " + e);
			return null;
		}
	}

	public static Component generateText(Player p, String type){
		String db = null;
		switch (type) {
			case "ls":
				db = LS_DB;
				break;
			case "ko":
				db = KO_DB;
				break;
			case "cb":
				db = CB_DB;
				break;
		}
		if(db == null){
			return text("null");
		}
		String query = "SELECT player_uuid, SUM(games_won) FROM " + db + " GROUP BY player_uuid ORDER BY SUM(games_won) DESC;";
		try (Connection conn = DriverManager.getConnection(Leaderboards.CB_URL)) {
			ResultSet res = conn.createStatement().executeQuery(query);

			Component leaderboard_rows = text("");
			switch (type) {
				case "ls":
					leaderboard_rows = LS_TITLE;
					break;
				case "ko":
					leaderboard_rows = KO_TITLE;
					break;
				case "cb":
					leaderboard_rows = CB_TITLE;
					break;
			}

			NavigableMap<Integer, TextComponent> names = Collections.emptyNavigableMap();
			Integer rank = null;
			Integer win = null;

			int h = 0;
			while (res.next()) {
				h++;
				UUID uuid = Leaderboards.convertBytesToUUID(res.getBytes("player_uuid"));
				Component name = Ranks.getName(Bukkit.getOfflinePlayer(uuid));
				int wins = res.getInt("SUM(games_won)");
				names.put(wins, (TextComponent)name);

				if(Bukkit.getOfflinePlayer(uuid).equals(p)){
					win = wins;
					rank = h;
				}
			}

			names.descendingMap();
			TextComponent longest = names.firstEntry().getValue();
			Integer key = names.firstKey();
			for(int j = 1; j <= 10 && j <= names.size(); j++){
				if(balance(names.lowerEntry(key).getValue().content()) > balance(longest.content())){
					longest = names.lowerEntry(key).getValue();
				}
				key = names.lowerKey(key);
			}

			if(balance(((TextComponent)Ranks.getName(p)).content()) > balance(longest.content())){
				longest = (TextComponent)Ranks.getName(p);
			}

			key = names.firstKey();
			int total = balance(longest.content()) + 3;

			int i = 0;
			for(int j = 1; j <= names.size(); j++){
				i++;
				int padding = total - (balance(names.get(key).content()) + balance("" + key));
				String dots = ".".repeat(padding);
				Component num = Leaderboards.get_styles(i);
				leaderboard_rows = leaderboard_rows.append(text("\n")).append(num);
				leaderboard_rows = leaderboard_rows.append(names.get(key)).append(text(dots).color(GRAY));
				leaderboard_rows = leaderboard_rows.append(text("" + key)).color(GREEN);
				key = names.lowerKey(key);
			}

			if(rank == null){
				return leaderboard_rows;
			}

			leaderboard_rows.append(text("\n")).append(text("-----------------").color(GRAY));
			int padding = total - (balance(((TextComponent)Ranks.getName(p)).content() + balance("" + win)));
			String dots = ".".repeat(padding);
			Component num = Leaderboards.get_styles(rank);
			leaderboard_rows = leaderboard_rows.append(text("\n")).append(num);
			leaderboard_rows = leaderboard_rows.append(Ranks.getName(p)).append(text(dots).color(GRAY));
			leaderboard_rows = leaderboard_rows.append(text("" + win)).color(GREEN);

			return leaderboard_rows;
		} catch (SQLException e) {
			Bukkit.getLogger().warning("error opening database: " + e);
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

}
