package gg.crystalized.lobby.parkour;

import gg.crystalized.lobby.Lobby_plugin;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

public class Timer{
    BukkitTask task;
    BossBar bar;
    int i = 0;
    int tenth = 0;
    int seconds = 0;
    int minutes = 0;
    int hours = 0;
    public Timer(Player p){
        bar = BossBar.bossBar(Component.text(buildTimer(0, 0,0, 0)).color(YELLOW), 0, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        p.showBossBar(bar);
        task = new BukkitRunnable(){
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

                bar.name(Component.text(buildTimer(tenth, seconds, minutes, hours)).color(YELLOW));

        }}.runTaskTimer(Lobby_plugin.getInstance(), 0, 1);
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

