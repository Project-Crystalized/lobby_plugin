package gg.crystalized.lobby.parkour;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class ParkourListener implements Listener {

    @EventHandler
    public void onMove(PlayerMoveEvent e){
        ParkourRun run = ParkourRun.getRun(e.getPlayer());
        if(run != null && run.isNextCheckpoint(e.getPlayer().getLocation())){
            run.onCheckpoint();
        }
        if(run == null && Parkour.findParkour(e.getPlayer().getLocation().toBlockLocation()) != null){
            new ParkourRun(e.getPlayer(), Parkour.findParkour(e.getPlayer().getLocation().toBlockLocation()));
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent e){
        ItemStack item = e.getItem();
        Player p = e.getPlayer();
        ParkourRun run = ParkourRun.getRun(p);
        if(run == null) return;

        if(p.getInventory().getItem(6).equals(item)){
            //return to checkpoint
            p.teleport(run.course.checkpoints[run.lastCheckpoint]);
        }

        if(p.getInventory().getItem(7).equals(item)){
            //restart
            p.teleport(run.course.checkpoints[0]);
            run.stop(false);
            new ParkourRun(p, run.course);
        }

        if(p.getInventory().getItem(8).equals(item)){
            run.stop(false);
        }
    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent e){
        ParkourRun run = ParkourRun.getRun(e.getPlayer());
        if(run == null) return;
        run.stop(false);
    }
}
