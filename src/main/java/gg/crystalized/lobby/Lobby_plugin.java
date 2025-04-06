package gg.crystalized.lobby;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.ArrayList;

public final class Lobby_plugin extends JavaPlugin implements PluginMessageListener {

	@Override
	public void onEnable() {
		this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "crystalized:main");
		this.getServer().getMessenger().registerIncomingPluginChannel(this, "crystalized:main", this);
		new Leaderboards();

		Commands dc = new Commands();
		this.getCommand("chess").setExecutor(dc);
		this.getCommand("pig_hunt").setExecutor(dc);
		this.getCommand("give_xp").setExecutor(dc);
		new RankDisplay();

		LobbyDatabase.setup_databases();
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals("crystalized:main")) {
			return;
		}
		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String messageText = in.readUTF();
		if(messageText.contains("add_xp")){
			LevelManager.giveExperience(player, filterNumbers(messageText));
		}
	}

	public int filterNumbers(String s){
		String number = "";
		for(int i = 0; i < s.length(); i++){
			if(NumberUtils.isCreatable(String.valueOf(s.charAt(i)))){
				number = number.concat(String.valueOf(s.charAt(i)));
			}
		}
		return Integer.parseInt(number);
	}

	@Override
	public void onDisable() {
	}

	public static Lobby_plugin getInstance() {
		return getPlugin(Lobby_plugin.class);
	}
}
