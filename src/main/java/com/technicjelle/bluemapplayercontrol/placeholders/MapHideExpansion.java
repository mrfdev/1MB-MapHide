package com.technicjelle.bluemapplayercontrol.placeholders;

import com.technicjelle.bluemapplayercontrol.BlueMapPlayerControl;
import com.technicjelle.bluemapplayercontrol.MapHideService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public final class MapHideExpansion extends PlaceholderExpansion {
	private final BlueMapPlayerControl plugin;
	private final MapHideService service;

	public MapHideExpansion(BlueMapPlayerControl plugin, MapHideService service) {
		this.plugin = plugin;
		this.service = service;
	}

	@Override
	public String getIdentifier() {
		return "maphide";
	}

	@Override
	public String getAuthor() {
		return "mrfloris, OpenAI";
	}

	@Override
	public String getVersion() {
		return plugin.getPluginMeta().getVersion();
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public String onRequest(OfflinePlayer player, String params) {
		return service.placeholder(player, params);
	}
}
