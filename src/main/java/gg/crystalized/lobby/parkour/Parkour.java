package gg.crystalized.lobby.parkour;

import gg.crystalized.lobby.Lobby_plugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;

import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

public class Parkour {
    public static ArrayList<Parkour> parkours = new ArrayList<>();
    TextColor color;
    String name;
    Location[] checkpoints;
    Location leaderboard;

    public Parkour(TextColor color, String name, Location[] checkpoints, Location leaderboard) {
        this.color = color;
        this.name = name;
        this.checkpoints = checkpoints;
        this.leaderboard = leaderboard;
        parkours.add(this);
    }

    public static Parkour findParkour(Location start){
        for(Parkour p : parkours){
            if(p.checkpoints[0].equals(start)) return p;
        }
        return null;
    }

    public static void showParkourStart(){

    }
}

class ParkourRun{
    public static ArrayList<ParkourRun> running = new ArrayList<>();
    Player p;
    Parkour course;
    BukkitTask timer;
    int lastCheckpoint;

    public ParkourRun(Player p, Parkour course) {
        this.p = p;
        this.course = course;
        this.lastCheckpoint = 0;
        Timer.doTimer(this);
        running.add(this);
    }
}

class Timer{
    public static void doTimer(ParkourRun run){
        run.timer = new BukkitRunnable(){
            int i = 0;
            int tenth = 0;
            int seconds = 0;
            int minutes = 0;
            int hours = 0;
            public void run(){
                if(i == 2){
                    tenth++;
                    i = 0;
                }
                if(tenth == 10){
                    seconds++;
                    tenth = 0;
                }
                if(seconds == 60){
                    minutes++;
                    seconds = 0;
                }
                if(minutes == 60){
                    hours++;
                    minutes = 0;
                }
                i++;

                for(Player p : Bukkit.getOnlinePlayers()){
                    p.sendActionBar(Component.text(buildTimer(tenth, seconds, minutes, hours)).color(YELLOW));
                }
            }
        }.runTaskTimer(Lobby_plugin.getInstance(), 0, 1);
    }

    public static String buildTimer(int tenth, int seconds, int minutes, int hours){
        String ten = "" + tenth;
        if(ten.length() < 2){
            ten = "0" + ten;
        }
        String sec = "" + seconds;
        if(sec.length() < 2){
            sec = "0" + sec;
        }
        String min = "" + minutes;
        if(min.length() < 2){
            min = "0" + min;
        }

        String h = "" + hours;
        if(h.length() < 2){
            h = "0" + h;
        }

        return h + ":" + min + ":" + sec + ":" + ten;
    }
}

