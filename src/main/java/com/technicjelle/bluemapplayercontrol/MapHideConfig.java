package com.technicjelle.bluemapplayercontrol;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MapHideConfig {
	private static final List<ConfigEntry> ENTRIES = List.of(
			new ConfigEntry("language", "EN", List.of(
					"Controls which translation file is loaded from the Translations folder.",
					"Default: EN.",
					"Format: language suffix only, such as EN or FR. The plugin loads Translations/Locale_<language>.yml.",
					"Changing this while the server is running requires /bmpc reload or a server restart."
			)),
			new ConfigEntry("bmpc-toggle-alias", "/map hide", List.of(
					"1MB Alias Helper: optional command alias for the player self-toggle command.",
					"Default: /map hide.",
					"Expected format: a command with a leading slash, such as /map hide. Set to \"\" to disable the alias.",
					"When a player types this command, the plugin runs the same permission checks and logic as /bmpc toggle.",
					"Changing this while the server is running requires /bmpc reload or a server restart."
			)),
			new ConfigEntry("default-visibility", "show", List.of(
					"Controls the starting BlueMap marker visibility when this plugin applies its join default.",
					"Default: show.",
					"Safe values: show or hide.",
					"show = players appear on BlueMap until they hide themselves. hide = players start hidden until they show themselves.",
					"Changing this affects future joins after /bmpc reload or a server restart; it does not rewrite already-applied player states."
			)),
			new ConfigEntry("apply-default-visibility-on-first-join-only", true, List.of(
					"Controls when default-visibility is applied on player join.",
					"Default: true.",
					"Safe values: true or false.",
					"true = only apply the default to first-time joins. false = apply it on every join and possibly override a player's previous choice.",
					"Changing this affects future joins after /bmpc reload or a server restart."
			)),
			new ConfigEntry("toggle-back-after-seconds", 0, List.of(
					"Automatically toggles a player again after they use /bmpc, /bmpc toggle, or the configured alias.",
					"Default: 0.",
					"Safe values: 0 or any positive whole number of seconds. Negative values are treated as 0 by the plugin.",
					"0 disables the timer. If a player toggles again before the timer ends, the timer is reset.",
					"Changing this takes effect after /bmpc reload or a server restart and applies to new toggles only."
			)),
			new ConfigEntry("forced-permissions.enabled", false, List.of(
					"Enables permission-based forced visibility overrides.",
					"Default: false.",
					"Safe values: true or false.",
					"false = players and admins can change visibility normally. true = hide-node and show-node are enforced.",
					"Changing this requires /bmpc reload or a server restart. Commands and joins check the new value after reload."
			)),
			new ConfigEntry("forced-permissions.hide-node", "maphide.forcehide", List.of(
					"Permission node that forcefully keeps a player hidden from BlueMap when forced-permissions.enabled is true.",
					"Default: maphide.forcehide.",
					"Expected format: a permission node string.",
					"Changing this requires /bmpc reload or a server restart."
			)),
			new ConfigEntry("forced-permissions.show-node", "maphide.forceshow", List.of(
					"Permission node that forcefully keeps a player visible on BlueMap when forced-permissions.enabled is true.",
					"Default: maphide.forceshow.",
					"Expected format: a permission node string.",
					"Changing this requires /bmpc reload or a server restart."
			)),
			new ConfigEntry("forced-permissions.conflict-priority", "hide", List.of(
					"Decides which forced permission wins if a player has both hide-node and show-node.",
					"Default: hide.",
					"Safe values: hide or show.",
					"Changing this requires /bmpc reload or a server restart."
			)),
			new ConfigEntry("forced-permissions.check-interval-seconds", 0, List.of(
					"How often, in seconds, to re-check online players for force permissions while they remain online.",
					"Default: 0.",
					"Safe values: 0 or any positive whole number of seconds. Negative values are treated as 0 by the plugin.",
					"0 disables the repeating check. Join events and commands still check force permissions.",
					"Only set this above 0 if another plugin or workflow keeps undoing forced visibility.",
					"Changing this requires /bmpc reload or a server restart; reload cancels and recreates the repeating task."
			))
	);
	private static final Map<String, List<String>> SECTION_COMMENTS = Map.of(
			"forced-permissions", List.of(
					"Optional permission-based overrides.",
					"Defaults: disabled, hide-node maphide.forcehide, show-node maphide.forceshow, conflict-priority hide, check interval 0.",
					"When enabled, players with the configured permission nodes are forced visible or hidden regardless of /map hide or admin /bmpc commands.",
					"Changing any value in this section requires /bmpc reload or a server restart."
			)
	);
	private static final Set<String> LEGACY_PLUGIN_COMMENTS = Set.of(
			"Translation language to load from the Translations folder.",
			"Default: EN",
			"Example: set this to FR and create Translations/Locale_FR.yml to use French messages.",
			"Reload with /bmpc reload after changing this while the server is running.",
			"1MB Alias Helper",
			"Alias command for the player self-toggle command.",
			"Default: /map hide",
			"When a player types /map hide, it runs the same logic as /bmpc toggle.",
			"This is specific to the 1MoreBlock.com server setup. Set to \"\" to disable.",
			"Starting BlueMap marker visibility for players when this plugin applies its join default.",
			"Valid values: show, hide",
			"Default: show",
			"show = players appear on BlueMap until they run /map hide.",
			"hide = players start hidden until they run /map hide.",
			"Controls when default-visibility is applied on join.",
			"Default: true",
			"true = only applies to players joining for the first time.",
			"false = applies on every join, which can override a player's previous choice.",
			"Automatically toggles the player again after they run /map hide.",
			"Default: 0",
			"0 = disabled.",
			"Any positive number = seconds to wait before toggling again.",
			"If the player runs /map hide before the timer ends, the timer resets.",
			"Optional permission-based overrides.",
			"Default: disabled",
			"When enabled, players with the configured permission nodes are forced visible or hidden,",
			"regardless of /map hide or admin /bmpc commands.",
			"Default: false",
			"false = players control their own marker unless an admin changes it.",
			"true = the hide-node and show-node permissions below are enforced.",
			"Permission that forcefully keeps a player hidden from BlueMap when enabled is true.",
			"Default: maphide.forcehide",
			"Permission that forcefully keeps a player visible on BlueMap when enabled is true.",
			"Default: maphide.forceshow",
			"If a player somehow has both force permissions, this setting decides which wins.",
			"Valid values: hide, show",
			"Default: hide",
			"How often, in seconds, to re-check online players for force permissions.",
			"0 = disables the repeating check. Join events and commands still check force permissions.",
			"Only set this above 0 if another plugin or workflow keeps undoing forced visibility."
	);

	private final JavaPlugin plugin;
	private final File configFile;
	private final Map<String, ConfigEntry> entriesByPath = new LinkedHashMap<>();
	private YamlConfiguration config;

	public MapHideConfig(JavaPlugin plugin) {
		this.plugin = plugin;
		this.configFile = new File(plugin.getDataFolder(), "config.yml");
		for (ConfigEntry entry : ENTRIES) {
			entriesByPath.put(entry.path(), entry);
		}
	}

	public YamlConfiguration reload() {
		YamlConfiguration loaded = new YamlConfiguration();
		loaded.options().parseComments(true);
		try {
			if (!configFile.getParentFile().isDirectory() && !configFile.getParentFile().mkdirs()) {
				throw new IOException("Could not create " + configFile.getParentFile());
			}
			if (configFile.isFile()) {
				loaded.load(configFile);
			}
			applyDefaultsAndComments(loaded);
			loaded.save(configFile);
		} catch (IOException | InvalidConfigurationException exception) {
			plugin.getLogger().severe("Could not load config.yml: " + exception.getMessage());
		}
		config = loaded;
		return config;
	}

	public YamlConfiguration config() {
		if (config == null) {
			return reload();
		}
		return config;
	}

	public boolean setValue(String path, Object value) {
		if (!entriesByPath.containsKey(path)) {
			return false;
		}
		config().set(path, value);
		applyDefaultsAndComments(config());
		try {
			config().save(configFile);
		} catch (IOException exception) {
			plugin.getLogger().severe("Could not save config.yml: " + exception.getMessage());
			return false;
		}
		return true;
	}

	public Map<String, Object> values() {
		Map<String, Object> values = new HashMap<>();
		flattenConfig("", config(), values);
		return values;
	}

	private void applyDefaultsAndComments(YamlConfiguration yaml) {
		SECTION_COMMENTS.forEach((path, comments) -> {
			if (!yaml.isConfigurationSection(path) && !yaml.contains(path)) {
				yaml.createSection(path);
			}
			mergeComments(yaml, path, comments);
		});
		for (ConfigEntry entry : ENTRIES) {
			if (!yaml.contains(entry.path())) {
				yaml.set(entry.path(), entry.defaultValue());
			}
			mergeComments(yaml, entry.path(), entry.comments());
		}
	}

	private void mergeComments(YamlConfiguration yaml, String path, List<String> comments) {
		List<String> existing = yaml.getComments(path);
		if (existing == null || existing.isEmpty()) {
			yaml.setComments(path, comments);
			return;
		}

		List<String> merged = new ArrayList<>(comments);
		for (String comment : existing) {
			if (comment == null || comments.contains(comment) || LEGACY_PLUGIN_COMMENTS.contains(comment)) {
				continue;
			}
			merged.add(comment);
		}
		List<String> deduplicated = new ArrayList<>();
		for (String comment : merged) {
			if (!deduplicated.contains(comment)) {
				deduplicated.add(comment);
			}
		}
		if (!deduplicated.equals(existing)) {
			yaml.setComments(path, deduplicated);
		}
	}

	private void flattenConfig(String prefix, ConfigurationSection section, Map<String, Object> values) {
		for (String key : section.getKeys(false)) {
			String path = prefix.isEmpty() ? key : prefix + "." + key;
			if (section.isConfigurationSection(key)) {
				flattenConfig(path, section.getConfigurationSection(key), values);
			} else {
				values.put(path, section.get(key));
			}
		}
	}

	private record ConfigEntry(String path, Object defaultValue, List<String> comments) {
		private ConfigEntry {
			comments = List.copyOf(new ArrayList<>(comments));
		}
	}
}
