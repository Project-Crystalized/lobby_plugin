package gg.crystalized.lobby;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

public final class PlayerListener implements Listener {

	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		// NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "Litestrike!");
		// npc.spawn(new Location(Bukkit.getWorld("world"), -10.5, -60, -20.5, -90, 0));
	}

	@EventHandler
	public void onPlayerHit(EntityDamageByEntityEvent e) {
		if (!CitizensAPI.getNPCRegistry().isNPC(e.getEntity())) {
			return;
		}
		NPC npc = CitizensAPI.getNPCRegistry().getNPC(e.getEntity());
		World w = e.getDamager().getWorld();
		if (npc.getName().equals("Litestrike!")) {
			e.getDamager().teleport(new Location(w, -29, -61, -112, -144, 0));
		} else if (npc.getName().equals("Litestrike")) {
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Connect");
			out.writeUTF("litestrike");
			((Player) e.getDamager()).sendPluginMessage(Lobby_plugin.getInstance(), "crystalized:main", out.toByteArray());
		}
	}
}
