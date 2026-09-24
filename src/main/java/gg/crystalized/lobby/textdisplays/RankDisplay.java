package gg.crystalized.lobby.textdisplays;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;

import gg.crystalized.lobby.Leaderboards;
import gg.crystalized.lobby.LobbyConfig;
import gg.crystalized.lobby.Lobby_plugin;
import gg.crystalized.lobby.Ranks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.Component.text;

public class RankDisplay {

	private static Location lb_loc = LobbyConfig.Locations.get("ls-ranked-leaderboard");

	private static final String RANKED_TYPE = "ls-ranked";

	static class RankedSnapshot {
		Component sharedText;
		HashMap<UUID, PlayerRankedData> ranked;

		RankedSnapshot(Component sharedText, HashMap<UUID, PlayerRankedData> ranked) {
			this.sharedText = sharedText;
			this.ranked = ranked;
		}
	}

	public RankDisplay() {
		Bukkit.getLogger().info("creating a Rank Display!");
		new BukkitRunnable() {
			@Override
			public void run() {
				RankedSnapshot snap = computeSnapshot();
				for (Player p : Bukkit.getOnlinePlayers()) {
					User user = PacketEvents.getAPI().getPlayerManager().getUser(p);
					if (user == null) continue;

					WinLeaderboard.leaderboards.computeIfAbsent(p, k -> new HashMap<>());
					if (!WinLeaderboard.leaderboards.get(p).containsKey(RANKED_TYPE)) {
						WinLeaderboard.createDisplay(user, p, lb_loc, RANKED_TYPE);
					}

					Component text = buildText(p, snap);

					int entityId = WinLeaderboard.leaderboards.get(p).get(RANKED_TYPE);
					user.sendPacket(WinLeaderboard.displayMetadata(entityId, text));
				}
			}
		}.runTaskTimer(Lobby_plugin.getInstance(), 20, (20 * 10));
	}

	static RankedSnapshot computeSnapshot() {
		try (Connection conn = DriverManager.getConnection(Leaderboards.LS_URL)) {
			ResultSet rs = conn.createStatement().executeQuery("SELECT player_uuid, rank, rp FROM LsRanks ORDER BY rp DESC;");
			Component sharedText = text("RANK Leaderboard\n").color(GOLD).append(text("LITESTRIKE\n---------------------").color(GREEN));
			HashMap<UUID, PlayerRankedData> ranked = new HashMap<>();
			int row = 1;
			int i = 0;
			while (rs.next()) {
				UUID uuid = Leaderboards.convertBytesToUUID(rs.getBytes("player_uuid"));
				int rank = rs.getInt("rank");
				int rp = rs.getInt("rp");
				ranked.put(uuid, new PlayerRankedData(uuid, rank, rp, row));
				row++;
				if (i < 10) {
					i++;
					Component num = Leaderboards.get_styles(i);
					sharedText = sharedText.append(text("\n")).append(num);
					sharedText = sharedText
							.append(Ranks.getName(Bukkit.getOfflinePlayer(uuid)))
							.append(text(" "));
					sharedText = sharedText.append(get_rank_symbol(rank).append(text(" " + rp + "rp\n")).color(WHITE));
				}
			}
			return new RankedSnapshot(sharedText, ranked);
		} catch (SQLException e) {
			Bukkit.getLogger().severe("sqlerror in Rank Leaderboard: " + e);
			return new RankedSnapshot(Component.text("sqlerror: " + e), new HashMap<>());
		}
	}

	static Component buildText(Player p, RankedSnapshot snap) {
		PlayerRankedData prd = snap.ranked.get(p.getUniqueId());
		if (prd == null) {
			return snap.sharedText;
		}
		Component text = snap.sharedText.append(text("\n")).append(text("-----------------").color(GRAY));
		text = text.append(text("\n")).append(Ranks.getName(p)).append(get_rank(prd.rank)).append(Component.translatable("crystalized.game.litestrike.ranked.with_rp", List.of(Component.text(prd.rp))));
		return text.append(Component.translatable("crystalized.game.litestrike.ranked.number", List.of(Component.text(prd.row_nr))));
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

	static class PlayerRankedData {
		public int rank;
		public int rp;
		public UUID uuid;
		public int row_nr;

		public PlayerRankedData(UUID uuid, int rank, int rp, int row_nr) {
			this.uuid = uuid;
			this.rank = rank;
			this.rp = rp;
			this.row_nr = row_nr;
		}
	}
}
