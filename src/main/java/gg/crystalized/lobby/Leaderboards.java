package gg.crystalized.lobby;

import java.nio.ByteBuffer;
import java.util.*;


import net.kyori.adventure.text.Component;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class Leaderboards {
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
}