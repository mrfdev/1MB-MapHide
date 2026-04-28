package com.technicjelle.bluemapplayercontrol;

import de.bluecolored.bluemap.api.BlueMapAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public final class MapHideService {
	private static final String DEFAULT_LANGUAGE = "EN";

	private final BlueMapPlayerControl plugin;
	private final MiniMessage miniMessage = MiniMessage.miniMessage();
	private final MapHideConfig mapHideConfig;
	private final Map<UUID, BukkitTask> toggleBackTasks = new HashMap<>();
	private final Map<UUID, Long> toggleBackExpiresAt = new HashMap<>();
	private BukkitTask forcedPermissionTask;
	private YamlConfiguration translations;
	private YamlConfiguration fallbackTranslations;
	private Properties buildInfo = new Properties();
	private String language = DEFAULT_LANGUAGE;

	public MapHideService(BlueMapPlayerControl plugin) {
		this.plugin = plugin;
		this.mapHideConfig = new MapHideConfig(plugin);
	}

	public void reload() {
		mapHideConfig.reload();
		saveBundledResource("Translations/Locale_EN.yml");
		loadBuildInfo();

		language = config().getString("language", DEFAULT_LANGUAGE).toUpperCase(Locale.ROOT);
		File translationFile = new File(plugin.getDataFolder(), "Translations/Locale_" + language + ".yml");
		if (!translationFile.isFile()) {
			language = DEFAULT_LANGUAGE;
			translationFile = new File(plugin.getDataFolder(), "Translations/Locale_EN.yml");
		}
		translations = YamlConfiguration.loadConfiguration(translationFile);
		loadFallbackTranslations();
		startForcedPermissionTask();
	}

	private void saveBundledResource(String path) {
		File file = new File(plugin.getDataFolder(), path);
		if (!file.isFile()) {
			plugin.saveResource(path, false);
		}
	}

	private void loadBuildInfo() {
		buildInfo = new Properties();
		try (var input = plugin.getResource("build-info.properties")) {
			if (input != null) {
				buildInfo.load(input);
			}
		} catch (Exception exception) {
			plugin.getLogger().warning("Could not load build-info.properties: " + exception.getMessage());
		}
	}

	private void loadFallbackTranslations() {
		fallbackTranslations = new YamlConfiguration();
		try (var input = plugin.getResource("Translations/Locale_EN.yml")) {
			if (input != null) {
				fallbackTranslations = YamlConfiguration.loadConfiguration(
						new InputStreamReader(input, StandardCharsets.UTF_8)
				);
			}
		} catch (Exception exception) {
			plugin.getLogger().warning("Could not load fallback translations: " + exception.getMessage());
		}
	}

	public void send(CommandSender sender, String key) {
		send(sender, key, Map.of());
	}

	public void send(CommandSender sender, String key, Map<String, String> placeholders) {
		String rawMessage = translations.getString("messages." + key);
		if (rawMessage == null) {
			rawMessage = fallbackTranslations.getString("messages." + key, "<red>Missing translation: " + key + "</red>");
		}
		String prefix = translations.getString("prefix");
		if (prefix == null) {
			prefix = fallbackTranslations.getString("prefix", "<dark_gray>[<aqua>MapHide</aqua>]</dark_gray>");
		}
		rawMessage = rawMessage.replace("<prefix>", prefix);

		List<TagResolver> resolvers = new ArrayList<>();
		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			resolvers.add(Placeholder.unparsed(entry.getKey(), entry.getValue()));
		}
		sender.sendMessage(miniMessage.deserialize(rawMessage, TagResolver.resolver(resolvers)));
	}

	public Optional<BlueMapAPI> blueMap() {
		return BlueMapAPI.getInstance();
	}

	public boolean toggleSelf(Player player) {
		return blueMap().map(api -> toggleSelf(api, player)).orElseGet(() -> {
			send(player, "bluemap-not-ready");
			return true;
		});
	}

	public boolean toggleSelf(BlueMapAPI api, Player player) {
		ForceMode forceMode = forceMode(player);
		if (forceMode != ForceMode.NONE) {
			applyForcedVisibility(api, player, forceMode);
			send(player, forceMode == ForceMode.HIDE ? "force-hide-active" : "force-show-active");
			return true;
		}

		UUID uuid = player.getUniqueId();
		boolean visible = !api.getWebApp().getPlayerVisibility(uuid);
		api.getWebApp().setPlayerVisibility(uuid, visible);
		send(player, visible ? "self-visible" : "self-hidden");
		scheduleToggleBack(player);
		return true;
	}

	public boolean setSelfVisibility(BlueMapAPI api, Player player, boolean visible) {
		ForceMode forceMode = forceMode(player);
		if (forceMode != ForceMode.NONE) {
			applyForcedVisibility(api, player, forceMode);
			send(player, forceMode == ForceMode.HIDE ? "force-hide-active" : "force-show-active");
			return true;
		}

		api.getWebApp().setPlayerVisibility(player.getUniqueId(), visible);
		cancelToggleBack(player.getUniqueId());
		send(player, visible ? "self-visible" : "self-hidden");
		return true;
	}

	public boolean setTargetVisibility(BlueMapAPI api, CommandSender sender, Player target, VisibilityAction action) {
		ForceMode forceMode = forceMode(target);
		if (forceMode != ForceMode.NONE) {
			applyForcedVisibility(api, target, forceMode);
			send(sender, "target-forced", Map.of(
					"player", target.getName(),
					"mode", forceMode.placeholder()
			));
			return true;
		}

		UUID uuid = target.getUniqueId();
		boolean visible = switch (action) {
			case SHOW -> true;
			case HIDE -> false;
			case TOGGLE -> !api.getWebApp().getPlayerVisibility(uuid);
		};
		api.getWebApp().setPlayerVisibility(uuid, visible);
		cancelToggleBack(target.getUniqueId());
		send(sender, visible ? "target-visible" : "target-hidden", Map.of("player", target.getName()));
		return true;
	}

	public void applyJoinVisibility(Player player) {
		blueMap().ifPresentOrElse(api -> applyJoinVisibility(api, player), () ->
				Bukkit.getScheduler().runTaskLater(plugin, () -> blueMap().ifPresent(api -> applyJoinVisibility(api, player)), 40L)
		);
	}

	private void applyJoinVisibility(BlueMapAPI api, Player player) {
		ForceMode forceMode = forceMode(player);
		if (forceMode != ForceMode.NONE) {
			applyForcedVisibility(api, player, forceMode);
			return;
		}

		boolean firstJoinOnly = config().getBoolean("apply-default-visibility-on-first-join-only", true);
		if (firstJoinOnly && player.hasPlayedBefore()) {
			return;
		}

		api.getWebApp().setPlayerVisibility(player.getUniqueId(), defaultVisible());
	}

	public void applyForcedVisibility(BlueMapAPI api, Player player, ForceMode forceMode) {
		api.getWebApp().setPlayerVisibility(player.getUniqueId(), forceMode == ForceMode.SHOW);
		cancelToggleBack(player.getUniqueId());
	}

	private void startForcedPermissionTask() {
		if (forcedPermissionTask != null) {
			forcedPermissionTask.cancel();
			forcedPermissionTask = null;
		}
		if (!config().getBoolean("forced-permissions.enabled", false)) {
			return;
		}

		int intervalSeconds = Math.max(0, config().getInt("forced-permissions.check-interval-seconds", 0));
		if (intervalSeconds <= 0) {
			return;
		}

		long intervalTicks = intervalSeconds * 20L;
		forcedPermissionTask = Bukkit.getScheduler().runTaskTimer(plugin, () ->
				blueMap().ifPresent(api -> {
					for (Player player : Bukkit.getOnlinePlayers()) {
						ForceMode forceMode = forceMode(player);
						if (forceMode != ForceMode.NONE) {
							applyForcedVisibility(api, player, forceMode);
						}
					}
				}), intervalTicks, intervalTicks);
	}

	public ForceMode forceMode(Player player) {
		if (!config().getBoolean("forced-permissions.enabled", false)) {
			return ForceMode.NONE;
		}

		boolean forceHide = player.hasPermission(forceHidePermission());
		boolean forceShow = player.hasPermission(forceShowPermission());
		if (forceHide && forceShow) {
			return config().getString("forced-permissions.conflict-priority", "hide").equalsIgnoreCase("show")
					? ForceMode.SHOW
					: ForceMode.HIDE;
		}
		if (forceHide) {
			return ForceMode.HIDE;
		}
		if (forceShow) {
			return ForceMode.SHOW;
		}
		return ForceMode.NONE;
	}

	public List<Player> findTargets(CommandSender sender, String targetSelector) {
		List<Player> directMatch = new ArrayList<>();
		Player exactPlayer = Bukkit.getPlayerExact(targetSelector);
		if (exactPlayer != null) {
			directMatch.add(exactPlayer);
			return directMatch;
		}
		if (!targetSelector.startsWith("@")) {
			return directMatch;
		}

		List<Player> players = new ArrayList<>();
		try {
			for (Entity entity : Bukkit.selectEntities(sender, targetSelector)) {
				if (entity instanceof Player player) {
					players.add(player);
				}
			}
		} catch (IllegalArgumentException ignored) {
			return Collections.emptyList();
		}
		return players;
	}

	public boolean setConfigValue(String key, String value) {
		Object parsedValue = parseConfigValue(value);
		if (!mapHideConfig.setValue(key, parsedValue)) {
			return false;
		}
		reload();
		return true;
	}

	private Object parseConfigValue(String value) {
		if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
			return Boolean.parseBoolean(value);
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ignored) {
			return value;
		}
	}

	public Map<String, Object> configValues() {
		return mapHideConfig.values();
	}

	public String placeholder(OfflinePlayer offlinePlayer, String identifier) {
		Player player = offlinePlayer == null ? null : offlinePlayer.getPlayer();
		return switch (identifier.toLowerCase(Locale.ROOT)) {
			case "visible" -> player == null ? "unknown" : Boolean.toString(isVisible(player));
			case "state" -> player == null ? "unknown" : (isVisible(player) ? "visible" : "hidden");
			case "forced" -> player == null ? "false" : Boolean.toString(forceMode(player) != ForceMode.NONE);
			case "force_mode" -> player == null ? "none" : forceMode(player).placeholder();
			case "default_visibility" -> defaultVisible() ? "visible" : "hidden";
			case "toggle_back_seconds" -> Integer.toString(toggleBackSeconds());
			case "toggle_back_remaining" -> player == null ? "0" : Long.toString(toggleBackRemaining(player.getUniqueId()));
			case "language" -> language;
			default -> null;
		};
	}

	private boolean isVisible(Player player) {
		return blueMap()
				.map(api -> api.getWebApp().getPlayerVisibility(player.getUniqueId()))
				.orElse(false);
	}

	public long toggleBackRemaining(UUID uuid) {
		Long expiresAt = toggleBackExpiresAt.get(uuid);
		if (expiresAt == null) {
			return 0L;
		}
		return Math.max(0L, (expiresAt - System.currentTimeMillis() + 999L) / 1000L);
	}

	private void scheduleToggleBack(Player player) {
		int seconds = toggleBackSeconds();
		cancelToggleBack(player.getUniqueId());
		if (seconds <= 0) {
			return;
		}

		UUID uuid = player.getUniqueId();
		toggleBackExpiresAt.put(uuid, System.currentTimeMillis() + seconds * 1000L);
		BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
			toggleBackTasks.remove(uuid);
			toggleBackExpiresAt.remove(uuid);
			Player onlinePlayer = Bukkit.getPlayer(uuid);
			if (onlinePlayer == null) {
				return;
			}
			blueMap().ifPresent(api -> {
				ForceMode forceMode = forceMode(onlinePlayer);
				if (forceMode != ForceMode.NONE) {
					applyForcedVisibility(api, onlinePlayer, forceMode);
					return;
				}
				boolean visible = !api.getWebApp().getPlayerVisibility(uuid);
				api.getWebApp().setPlayerVisibility(uuid, visible);
				send(onlinePlayer, visible ? "timer-visible" : "timer-hidden");
			});
		}, seconds * 20L);
		toggleBackTasks.put(uuid, task);
		send(player, "toggle-back-scheduled", Map.of("seconds", Integer.toString(seconds)));
	}

	private void cancelToggleBack(UUID uuid) {
		BukkitTask task = toggleBackTasks.remove(uuid);
		if (task != null) {
			task.cancel();
		}
		toggleBackExpiresAt.remove(uuid);
	}

	public void cancelTimers() {
		for (BukkitTask task : toggleBackTasks.values()) {
			task.cancel();
		}
		toggleBackTasks.clear();
		toggleBackExpiresAt.clear();
		if (forcedPermissionTask != null) {
			forcedPermissionTask.cancel();
			forcedPermissionTask = null;
		}
	}

	public boolean defaultVisible() {
		return !config().getString("default-visibility", "show").equalsIgnoreCase("hide");
	}

	public int toggleBackSeconds() {
		return Math.max(0, config().getInt("toggle-back-after-seconds", 0));
	}

	public boolean forcedPermissionsEnabled() {
		return config().getBoolean("forced-permissions.enabled", false);
	}

	public String conflictPriority() {
		return config().getString("forced-permissions.conflict-priority", "hide");
	}

	public String language() {
		return language;
	}

	public String toggleAliasLabel() {
		String alias = config().getString("bmpc-toggle-alias", "/map hide");
		return alias == null || alias.isBlank() ? "disabled" : alias.trim();
	}

	public String forceHidePermission() {
		return config().getString("forced-permissions.hide-node", "maphide.forcehide");
	}

	public String forceShowPermission() {
		return config().getString("forced-permissions.show-node", "maphide.forceshow");
	}

	public String buildInfo(String key, String fallback) {
		return buildInfo.getProperty(key, fallback);
	}

	public boolean matchesToggleAlias(String command, boolean slashRequired) {
		String alias = config().getString("bmpc-toggle-alias", "/map hide");
		if (alias == null || alias.isBlank()) {
			return false;
		}

		String normalizedCommand = command.trim();
		if (slashRequired) {
			if (!normalizedCommand.startsWith("/")) {
				return false;
			}
		} else if (!normalizedCommand.startsWith("/")) {
			normalizedCommand = "/" + normalizedCommand;
		}

		return normalizedCommand.equalsIgnoreCase(alias.trim());
	}

	private FileConfiguration config() {
		return mapHideConfig.config();
	}

	public enum VisibilityAction {
		SHOW,
		HIDE,
		TOGGLE
	}

	public enum ForceMode {
		NONE("none"),
		HIDE("hide"),
		SHOW("show");

		private final String placeholder;

		ForceMode(String placeholder) {
			this.placeholder = placeholder;
		}

		public String placeholder() {
			return placeholder;
		}
	}
}
