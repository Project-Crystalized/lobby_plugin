package gg.crystalized.lobby.textdisplays;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;

import gg.crystalized.lobby.LobbyConfig;
import gg.crystalized.lobby.Lobby_plugin;
import gg.crystalized.lobby.Ranks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

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
		new BukkitRunnable() {
			@Override
			public void run() {
				RankedSnapshot snap = computeSnapshot();
				for (Player p : Bukkit.getOnlinePlayers()) {
					User user = PacketEvents.getAPI().getPlayerManager().getUser(p);
					if (user == null) continue;

					Leaderboards.leaderboards.computeIfAbsent(p, k -> new HashMap<>());
					if (!Leaderboards.leaderboards.get(p).containsKey(RANKED_TYPE)) {
						Leaderboards.createDisplay(user, p, lb_loc, RANKED_TYPE);
					}

					Component text = buildText(p, snap);

					int entityId = Leaderboards.leaderboards.get(p).get(RANKED_TYPE);
					user.sendPacket(Leaderboards.displayMetadata(entityId, text, Leaderboards.BILLBOARD_FIXED));
				}
			}
		}.runTaskTimer(Lobby_plugin.getInstance(), 2, (20 * 10));
	}

	static RankedSnapshot computeSnapshot() {
		try (Connection conn = DriverManager.getConnection(Leaderboards.LS_URL)) {
			ResultSet rs = conn.createStatement().executeQuery("SELECT player_uuid, rank, rp FROM LsRanks ORDER BY rp DESC;");
			Component sharedText = text("").append(text("RANK Leaderboard\n").color(GOLD)).append(text("LITESTRIKE\n").color(GREEN).decoration(BOLD, true));
			HashMap<UUID, PlayerRankedData> ranked = new HashMap<>();
			ArrayList<TextComponent> topKey = new ArrayList<>();
			HashMap<TextComponent, int[]> top = new HashMap<>();
			String longest = "";
			String longestValue = "";
			int row = 1;
			while (rs.next()) {
				UUID uuid = Leaderboards.convertBytesToUUID(rs.getBytes("player_uuid"));
				int rank = rs.getInt("rank");
				int rp = rs.getInt("rp");
				ranked.put(uuid, new PlayerRankedData(uuid, rank, rp, row));
				row++;
				TextComponent name = (TextComponent) Ranks.getName(Bukkit.getOfflinePlayer(uuid));
				String name_str = PlainTextComponentSerializer.plainText().serialize(name);
				if (top.containsKey(name)) {
					continue;
				}
				String value_str = PlainTextComponentSerializer.plainText()
						.serialize(get_rank_symbol(rank).append(text(" " + rp + "rp")));
				if (Leaderboards.balance(name_str) > Leaderboards.balance(longest)) {
					longest = name_str;
				}
				if (Leaderboards.balance(value_str) > Leaderboards.balance(longestValue)) {
					longestValue = value_str;
				}
				if (topKey.size() < 10) {
					topKey.add(name);
					top.put(name, new int[] { rank, rp });
				}
			}

			int minDots = 6;
			int total = Leaderboards.balance(longest + "........" + longestValue) + minDots;
			for (int j = 0; j <= topKey.size() - 1; j++) {
				String top_str = PlainTextComponentSerializer.plainText().serialize(topKey.get(j));
				Component num = Leaderboards.get_styles(j + 1);
				String num_str = PlainTextComponentSerializer.plainText().serialize(num);
				String value_str = PlainTextComponentSerializer.plainText().serialize(
						get_rank_symbol(top.get(topKey.get(j))[0])
								.append(text(" " + top.get(topKey.get(j))[1] + "rp")));
				int numWidth = j + 1 == 1 ? Leaderboards.balanceBold(num_str) : Leaderboards.balance(num_str);
				int padding = total - (numWidth + Leaderboards.balance(top_str) + Leaderboards.balance(value_str));
				String dots = ".".repeat(Math.max(0, padding));
				sharedText = sharedText.append(text("\n")).append(num);
				sharedText = sharedText.append(topKey.get(j)).append(text(dots).color(GRAY));
				sharedText = sharedText.append(get_rank_symbol(top.get(topKey.get(j))[0])
						.append(text(" " + top.get(topKey.get(j))[1] + "rp\n")).color(WHITE));
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
		Component ownLine = text("\n").append(Ranks.getName(p)).append(get_rank(prd.rank)).append(Component.translatable("crystalized.game.litestrike.ranked.with_rp", List.of(Component.text(prd.rp))));
		ownLine = ownLine.append(Component.translatable("crystalized.game.litestrike.ranked.number", List.of(Component.text(prd.row_nr))));
		return snap.sharedText.append(text("\n")).append(text("-----------------").color(GRAY)).append(ownLine.color(WHITE));
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
