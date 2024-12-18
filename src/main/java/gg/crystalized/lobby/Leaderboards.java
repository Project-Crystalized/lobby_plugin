package gg.crystalized.lobby;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import static net.kyori.adventure.text.format.NamedTextColor.*;

import static net.kyori.adventure.text.Component.text;


public class Leaderboards {
	public static final String URL = "jdbc:sqlite:"+ System.getProperty("user.home")+"/databases/litestrike_db.sql";

	public Leaderboards() {
		new LsWinLeaderboard(Bukkit.getWorld("world"));
	}
}

class LsWinLeaderboard {
	public LsWinLeaderboard(World w) {
		Location loc = new Location(w, -19, -57.5, -110.5, 90.0f, 26.565f);
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					TextDisplay display = loc.getNearbyEntitiesByType(TextDisplay.class, 2.0).stream().findAny().get();
					display.teleport(loc);
					display.text(generate_text());
					display.setShadowed(true);
					display.setBillboard(Billboard.CENTER);
					display.setBackgroundColor(Color.fromARGB(80, 50, 50, 50));
				} catch (NoSuchElementException e) {
					if (loc.getChunk().isLoaded()) {
						Bukkit.getLogger().info("A leaderboards Display ENtity might be missing");
					}
				}
			}
		}.runTaskTimer(Lobby_plugin.getInstance(), (20 * 5), (20 * 10));
	}


	private Component generate_text() {
		String query = "SELECT player_uuid, SUM(was_winner) FROM LsGamesPlayers GROUP BY player_uuid ORDER BY SUM(was_winner) DESC LIMIT 10;";
		try (Connection conn = DriverManager.getConnection(Leaderboards.URL)) {
			ResultSet res = conn.createStatement().executeQuery(query);
			Component leaderbaord_rows = text("Game Leaderboard\n").color(GOLD).append(text("LIGHTSTRIKE").color(GREEN));
			int i = 0;
			while (res.next()) {
				i++;
				UUID uuid = convertBytesToUUID(res.getBytes("player_uuid"));
				String name = Bukkit.getOfflinePlayer(uuid).getName();
				Component num = get_styles(i);
				leaderbaord_rows = leaderbaord_rows.append(text("\n")).append(num);
				leaderbaord_rows = leaderbaord_rows.append(text("" + name).color(WHITE)).append(text(" - ").color(GRAY));
				leaderbaord_rows = leaderbaord_rows.append(text("" + res.getInt("SUM(was_winner)")).color(GREEN));
			}
			return leaderbaord_rows;
		} catch (SQLException e) {
			Bukkit.getLogger().severe("error opening database: " + e);
			return null;
		}
	}

	Component get_styles(int i) {
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

	public static String convertToSmallCaps(int number) {
		String[] suffixes = {"th. ", "st. ", "nd. ", "rd. "};
		String[] exceptions = {"11", "12", "13"};

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

	public static UUID convertBytesToUUID(byte[] bytes) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		long high = byteBuffer.getLong();
		long low = byteBuffer.getLong();
		return new UUID(high, low);
	}
}
