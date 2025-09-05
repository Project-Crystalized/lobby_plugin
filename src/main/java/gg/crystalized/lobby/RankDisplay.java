package gg.crystalized.lobby;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

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

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.Component.text;

public class RankDisplay {

	public static Location display_loc = LobbyConfig.Locations.get("ls-ranked-display");
	public static Location lb_loc = LobbyConfig.Locations.get("ls-ranked-leaderboard");

	public RankDisplay() {
		spawn_leaderboard();
	}

	private void spawn_leaderboard() {
		new BukkitRunnable() {
			@Override
			public void run() {
				World w = Bukkit.getWorld("world");
				lb_loc.getNearbyEntitiesByType(TextDisplay.class, 2.0).forEach(entity -> entity.remove());
				TextDisplay display = (TextDisplay) w.spawnEntity(lb_loc, EntityType.TEXT_DISPLAY);
				display.setShadowed(true);
				display.setBillboard(Billboard.VERTICAL);
				display.setBackgroundColor(Color.fromARGB(80, 50, 50, 50));
				display.text(get_leaderboard_text());

				update_display();
			}
		}.runTaskTimer(Lobby_plugin.getInstance(), 15, (20 * 10));
	}

	private static Component get_leaderboard_text() {
		String query = "SELECT * FROM LsRanks ORDER BY rp DESC LIMIT 10;";
		try (Connection conn = DriverManager.getConnection(Leaderboards.LS_URL)) {
			ResultSet res = conn.createStatement().executeQuery(query);

			Component leaderbaord_rows = text("RANK Leaderboard\n").color(GOLD).append(text("LITESTRIKE\n---------------------").color(GREEN));
			int i = 0;
			while (res.next()) {
				i++;
				String name = Bukkit.getOfflinePlayer(Leaderboards.convertBytesToUUID(res.getBytes("player_uuid"))).getName();
				Component num = Leaderboards.get_styles(i);
				int rp = res.getInt("rp");
				Component rank = get_rank_symbol(res.getInt("rank"));
				leaderbaord_rows = leaderbaord_rows.append(text("\n")).append(num);
				leaderbaord_rows = leaderbaord_rows.append(text("" + name + " ").color(WHITE));
				leaderbaord_rows = leaderbaord_rows.append(rank.append(text(" " + rp + "rp\n")).color(WHITE));
			}
			return leaderbaord_rows;
		} catch (SQLException e) {
			//Bukkit.getLogger().severe("sqlerror in Rank Leaderboard: "+e);
			return Component.text("sqlerror: "+e);
		}
	}

	public static void update_display() {
		World w = Bukkit.getWorld("world");
		try (Connection conn = DriverManager.getConnection(Leaderboards.LS_URL)) {
			String query = "SELECT * FROM LsRanks WHERE player_uuid = ?;";
			String query2 = "SELECT row_nr FROM (SELECT ROW_NUMBER() OVER (ORDER BY rp DESC) AS row_nr, player_uuid FROM LsRanks) sub WHERE sub.player_uuid = ?;";
			PreparedStatement ps = conn.prepareStatement(query);
			PreparedStatement ps2 = conn.prepareStatement(query2);
			display_loc.getNearbyEntitiesByType(TextDisplay.class, 2.0).forEach(entity -> entity.remove());
			for (Player p : Bukkit.getOnlinePlayers()) {
				TextDisplay display = (TextDisplay) w.spawnEntity(display_loc, EntityType.TEXT_DISPLAY);
				display.setShadowed(true);
				display.setBillboard(Billboard.VERTICAL);
				display.setBackgroundColor(Color.fromARGB(80, 50, 50, 50));

				ps.setBytes(1, uuid_to_bytes(p.getUniqueId()));
				ps2.setBytes(1, uuid_to_bytes(p.getUniqueId()));
				ResultSet rs = ps.executeQuery();
				ResultSet rs2 = ps2.executeQuery();
				PlayerRankedData prd = new PlayerRankedData(rs, p.getUniqueId());

				Component text = Component.text(p.getName() + " is\n\n").append(get_rank(prd.rank)).append(Component.text("\n\nwith " + prd.rp + " rank points."));
				text = text.append(Component.text("\nYou are number "+ rs2.getInt("row_nr") + " in rp."));
				display.text(text);
				for (Player player : Bukkit.getOnlinePlayers()) {
					player.hideEntity(Lobby_plugin.getInstance(), display);
				}
				p.showEntity(Lobby_plugin.getInstance(), display);
			}
		} catch (SQLException e) {
			//Bukkit.getLogger().severe("sqlerror in Rank Display: "+e);
		}
	}

	private static byte[] uuid_to_bytes(UUID uuid) {
		ByteBuffer bb = ByteBuffer.allocate(16);
		bb.putLong(uuid.getMostSignificantBits());
		bb.putLong(uuid.getLeastSignificantBits());
		return bb.array();
	}

	private static Component get_rank(int rank) {
		switch (rank) {
			case 1:
				return Component.text("\uE140 Unranked \uE140");
			case 2:
				return Component.text("\uE141 Stone \uE141");
			case 3:
				return Component.text("\uE142 Coal \uE142");
			case 4:
				return Component.text("\uE143 Copper \uE143");
			case 5:
				return Component.text("\uE144 Iron \uE144");
			case 6:
				return Component.text("\uE145 Gold \uE145");
			case 7:
				return Component.text("\uE146 Lapis \uE146");
			case 8:
				return Component.text("\uE147 Emerald \uE147");
			case 9:
				return Component.text("\uE148 Diamond \uE148");
			case 10:
				return Component.text("\uE149 Netherite \uE149");
			default:
				return Component.text("error");
		}
	}

	private static Component get_rank_symbol(int rank) {
		switch (rank) {
			case 1:
				return Component.text("\uE140");
			case 2:
				return Component.text("\uE141");
			case 3:
				return Component.text("\uE142");
			case 4:
				return Component.text("\uE143");
			case 5:
				return Component.text("\uE144");
			case 6:
				return Component.text("\uE145");
			case 7:
				return Component.text("\uE146");
			case 8:
				return Component.text("\uE147");
			case 9:
				return Component.text("\uE148");
			case 10:
				return Component.text("\uE149");
			default:
				return Component.text("error");
		}
	}
}

class PlayerRankedData {
	public int rank;
	public int rp;
	public UUID uuid;

	public PlayerRankedData(ResultSet rs, UUID uuid) throws SQLException {
		this.uuid = uuid;
		rs.next();
		this.rank = rs.getInt("rank");
		this.rp = rs.getInt("rp");
	}

	public PlayerRankedData(ResultSet rs) throws SQLException {
		rs.next();
		this.uuid = Leaderboards.convertBytesToUUID(rs.getBytes("player_uuid"));
		this.rank = rs.getInt("rank");
		this.rp = rs.getInt("rp");
	}
}
