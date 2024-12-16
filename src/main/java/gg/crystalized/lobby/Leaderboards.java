package gg.crystalized.lobby;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.Component.text;


public class Leaderboards {
	public static final String URL = "jdbc:sqlite:/home/nico/databases/litestrike_db.sql";
	private LsWinLeaderboard ls_win_lb;

	public Leaderboards() {
		World w = Bukkit.getWorld("world");
		ls_win_lb = new LsWinLeaderboard(w);
	}
}

class LsWinLeaderboard {
	private TextDisplay display;
	public LsWinLeaderboard(World w) {
		Location loc = new Location(w, -20, -59, -111);
		display = w.spawn(loc, TextDisplay.class);
		display.text(text("loading"));

		new BukkitRunnable() {
			@Override
			public void run() {
				display.text(generate_text());
			}
		}.runTaskTimer(Lobby_plugin.getInstance(), (20 * 5), (20 * 10));
	}


	private Component generate_text() {
		String query = "SELECT player_uuid, SUM(was_winner) FROM LsGamesPlayers GROUP BY player_uuid ORDER BY SUM(was_winner) DESC LIMIT 10;";
		try (Connection conn = DriverManager.getConnection(Leaderboards.URL)) {
			ResultSet res = conn.createStatement().executeQuery(query);
			Component leaderbaord_rows = text("");
			while (res.next()) {
				UUID uuid = convertBytesToUUID(res.getBytes("player_uuid"));
				leaderbaord_rows = leaderbaord_rows.append(text(Bukkit.getOfflinePlayer(uuid).getName() + " wins: " + res.getInt("SUM(was_winner)") + "\n"));
			}
			return leaderbaord_rows;
		} catch (SQLException e) {
			Bukkit.getLogger().severe("" + e);
			for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
				Bukkit.getLogger().severe(ste.toString());
			}
			return null;
		}
	}

	public static UUID convertBytesToUUID(byte[] bytes) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		long high = byteBuffer.getLong();
		long low = byteBuffer.getLong();
		return new UUID(high, low);
	}
}
