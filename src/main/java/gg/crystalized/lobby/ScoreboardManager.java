package gg.crystalized.lobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.geysermc.floodgate.api.FloodgateApi;

import static net.kyori.adventure.text.Component.text;

public class ScoreboardManager {
    public static void SetScoreboard(Player p) {
        FloodgateApi floodgate = FloodgateApi.getInstance();
        Scoreboard s = Bukkit.getScoreboardManager().getNewScoreboard();

        Component title = text("Crystalized").color(NamedTextColor.LIGHT_PURPLE);
        Objective obj = s.registerNewObjective("main", Criteria.DUMMY, title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        obj.getScore("7").setScore(7);
        obj.getScore("7").customName(text("   "));

        obj.getScore("6").setScore(6);
        obj.getScore("6").customName(text("Rank: ").color(NamedTextColor.AQUA).append(text("TODO"))); //TODO get rank name and append it to here

        obj.getScore("5").setScore(5);
        obj.getScore("5").customName(text("Currency: "));

        obj.getScore("4").setScore(4);
        obj.getScore("4").customName(text("  "));

        obj.getScore("3").setScore(3);
        obj.getScore("3").customName(text("Lobby: "));

        obj.getScore("2").setScore(2);
        obj.getScore("2").customName(text("Players: ").color(NamedTextColor.GREEN));

        obj.getScore("1").setScore(1);
        obj.getScore("1").customName(text(" "));

        obj.getScore("0").setScore(0);
        obj.getScore("0").customName(text("crystalized.cc ").color(TextColor.color(0xc4b50a)).append(text("(lobby)").color(NamedTextColor.GRAY)));

        Team CurrencyCounter = s.registerNewTeam("CurrencyCounter");
        CurrencyCounter.addEntry("5");
        obj.getScore("5").setScore(5);

        Team PlayerCounter = s.registerNewTeam("PlayerCounter");
        PlayerCounter.addEntry("2");
        obj.getScore("2").setScore(2);

        Team LobbyNumber = s.registerNewTeam("LobbyNumber");
        LobbyNumber.addEntry("3");
        obj.getScore("3").setScore(3);

        p.setScoreboard(s);

        /*
        This is where we update values
        I've included the floodgate api since Java and Bedrock scoreboards need to be done differently
        Also in the rare case that the floodgate api fucks up and identifies a Bedrock player as a Java player
        for a split second, It should automatically correct itself by being in a BukkitRunnable

        //TODOs
        Somehow get the lobby number if there's multiple lobby instances connected to Velocity

        Add Currency in a database and get the value to here

        If possible, for the players counter we should somehow get the Player count off Velocity instead of the local Paper server, so it counts everyone online
        and not just in that specific lobby instance

         */

        new BukkitRunnable() {
            @Override
            public void run() {
                if (floodgate.isFloodgatePlayer(p.getUniqueId())) {
                    //Bedrock
                    obj.getScore("3").customName(text("Lobby: ")
                            .append(text("1"))
                    );

                    obj.getScore("2").customName(text("Players: ").color(NamedTextColor.GREEN)
                            .append(text("" + Bukkit.getOnlinePlayers().size()).color(NamedTextColor.WHITE))
                    );

                    obj.getScore("5").customName(text("Currency: ")
                            .append(text("0"))
                    );
                } else {
                    //Java
                    LobbyNumber.suffix(text("1"));
                    PlayerCounter.suffix(text("" + Bukkit.getOnlinePlayers().size()));
                    CurrencyCounter.suffix(text("0"));
                }
            }
        }.runTaskTimer(Lobby_plugin.getInstance(), 2, 5);
    }
}
