package gg.crystalized.lobby;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public final class Lobby_plugin extends JavaPlugin {

	@Override
	public void onEnable() {
		this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "crystalized:main");
		new Leaderboards();

		Commands dc = new Commands();
		this.getCommand("chess").setExecutor(dc);
		this.getCommand("pig_hunt").setExecutor(dc);
		this.getCommand("give_xp").setExecutor(dc);
		new RankDisplay();

		LobbyDatabase.setup_databases();
	}

	@Override
	public void onDisable() {
	}

	public static Lobby_plugin getInstance() {
		return getPlugin(Lobby_plugin.class);
	}
}
