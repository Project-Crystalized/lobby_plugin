package gg.crystalized.lobby;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import static net.kyori.adventure.text.Component.text;
import static org.bukkit.entity.EntityType.TEXT_DISPLAY;
import static org.bukkit.entity.TextDisplay.TextAlignment.CENTER;

public class Nametag {
    Player p;
    TextDisplay nametag;
    BukkitTask runnable;
    public Nametag(Player p){
        this.p = p;
        nametag = (TextDisplay) Bukkit.getWorld("world").spawnEntity(new Location(Bukkit.getWorld("world"), 0, 0, 0), TEXT_DISPLAY);
        Component text = Ranks.getRankWithName(p).append(text("\n")).append(Ranks.getColoredName(p)).append(text("\n"));
        nametag.text(text);
        nametag.setAlignment(CENTER);
        nametag.setBillboard(Display.Billboard.CENTER);
        nametag.setPersistent(false);
        nametag.getPersistentDataContainer().set(new NamespacedKey("crystalized", "nametag"), PersistentDataType.STRING, p.getName() + "_nametag");
        Location loc = p.getLocation();
        loc.setY(loc.getY() + 2);
        nametag.teleport(loc);
        nametag.setInterpolationDuration(1);
        nametag.setTeleportDuration(1);
        //p.hideEntity(Lobby_plugin.getInstance(), nametag);
        runnable = new BukkitRunnable(){
            public void run(){
                if(!p.isOnline()){
                    cancel();
                }
                Vector v = p.getVelocity();
                v.multiply(v.length());
                v.add(p.getLocation().toVector());
                Location loc = v.toLocation(p.getWorld());
                loc.setY(loc.getY() + 2);
                nametag.teleport(loc);
            }
        }.runTaskTimer(Lobby_plugin.getInstance(), 1, 1);
    }
}
