package gg.crystalized.lobby.textdisplays;

import java.nio.ByteBuffer;
import java.util.*;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

import gg.crystalized.lobby.BitmapGlyphInfo;
import gg.crystalized.lobby.LobbyDatabase;
import gg.crystalized.lobby.Nametag;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class Leaderboards {
	public static HashMap<Player, HashMap<String, Integer>> leaderboards = new HashMap<>();

	static void createDisplay(User user, Player p, Location loc, String type){
		int id = Nametag.EntityId;
		leaderboards.get(p).put(type, id);
		Nametag.EntityId++;
		WrapperPlayServerSpawnEntity entity = new WrapperPlayServerSpawnEntity(id, UUID.randomUUID(), EntityTypes.TEXT_DISPLAY, new com.github.retrooper.packetevents.protocol.world.Location
				(loc.getX(), loc.getY(), loc.getZ(), 0, 0), 0, 0, new Vector3d());
		user.sendPacket(entity);
	}

	static final int BILLBOARD_FIXED = 0;
	static final int BILLBOARD_VERTICAL = 1;

	static WrapperPlayServerEntityMetadata displayMetadata(int entityId, Component text, int billboardMode) {
		List<EntityData<?>> metadataEntries = List.of(new EntityData<>(15, EntityDataTypes.BYTE, (byte) billboardMode),
				new EntityData<Component>(23, EntityDataTypes.ADV_COMPONENT, text),
				new EntityData<Integer>(24, EntityDataTypes.INT, 240),
				new EntityData<Integer>(25, EntityDataTypes.INT, 1345466930),
				new EntityData<Byte>(27, EntityDataTypes.BYTE, (byte) 1));
		return new WrapperPlayServerEntityMetadata(entityId, metadataEntries);
	}

	public static final String LS_URL = "jdbc:sqlite:" + LobbyDatabase.dbDir() + "/litestrike_db.sql";
	public static final String KO_URL = "jdbc:sqlite:" + LobbyDatabase.dbDir() + "/knockoff_db.sql";
	public static final String CB_URL = "jdbc:sqlite:" + LobbyDatabase.dbDir() + "/crystalblitz_db.sql";
	public static final String LOBBY_URL = "jdbc:sqlite:" + LobbyDatabase.dbDir() + "/lobby_db.sql";

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

	public static int balance(String name) {
		char[] chars = name.toCharArray();
		int sum = 0;
		for (char c : chars) {
			sum += BitmapGlyphInfo.getBitmapGlyphInfo(c).width;
		}
		sum += name.length() - 1;
		return sum / 2;
	}

	public static int balanceBold(String name) {
		char[] chars = name.toCharArray();
		int sum = 0;
		for (char c : chars) {
			sum += BitmapGlyphInfo.getBitmapGlyphInfo(c).width + 1;
		}
		sum += name.length() - 1;
		return sum / 2;
	}
}
