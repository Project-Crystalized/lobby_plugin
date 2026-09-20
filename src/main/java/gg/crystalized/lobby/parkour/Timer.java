package gg.crystalized.lobby.parkour;

import gg.crystalized.lobby.Lobby_plugin;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

public class Timer{
    BukkitTask task;
    BossBar bar;
    int millis = 0;
    int seconds = 0;
    int minutes = 0;
    int hours = 0;
    public Timer(Player p){
        bar = BossBar.bossBar(Component.text(buildTimer(0, 0,0, 0)).color(YELLOW), 0, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        p.showBossBar(bar);
        task = new BukkitRunnable(){
            public void run(){
                millis = millis + 50;
                if(millis >= 999){
                    seconds++;
                    millis = 0;
                }
                if(seconds >= 60){
                    minutes++;
                    seconds = 0;
                }
                if(minutes >= 60){
                    hours++;
                    minutes = 0;
                }
                bar.name(Component.text(buildTimer(millis, seconds, minutes, hours)).color(YELLOW));

        }}.runTaskTimer(Lobby_plugin.getInstance(), 0, 1);
    }

    public static String buildTimer(int millis, int seconds, int minutes, int hours){
        String mil = "" + millis;
        if(mil.length() < 3){
            mil = "0".repeat(3-mil.length()) + mil;
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
        return h + ":" + min + ":" + sec + "." + mil;
    }
}

