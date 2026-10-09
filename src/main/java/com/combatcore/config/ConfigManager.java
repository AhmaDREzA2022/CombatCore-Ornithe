package com.combatcore.config;

import com.combatcore.CombatCoreMod;
import com.combatcore.module.Module;
import com.combatcore.module.setting.NumberSetting;
import com.combatcore.module.setting.Setting;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.options.KeyBinding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON config: {@code <game dir>/config/combatcore.json}, written with
 * Minecraft's bundled Gson (2.2.4-compatible API, no extra dependencies).
 *
 * <p>Stores the ClickGUI key plus, per module, the enabled state, the toggle
 * key binding and all settings:
 *
 * <pre>
 * {
 *   "guiKey": 54,
 *   "modules": {
 *     "W-Tap": { "enabled": false, "key": 0, "settings": { "Delay": 0, "Chance": 100 } },
 *     ...
 *   }
 * }
 * </pre>
 *
 * <p>Key codes ({@code guiKey} and the per-module {@code key}) are applied
 * onto the live vanilla {@link KeyBinding}s once, when the file is first read,
 * so hand-edited values win over {@code options.txt} on the next launch. After
 * that the bindings are authoritative: every tick the live codes are mirrored
 * back here (dirtying the config when they change), which keeps rebinds made
 * in Options - Controls or the ClickGUI persisting across restarts.
 *
 * <p>Saving is dirty-flagged and debounced (at most one write per second of
 * inactivity) so dragging a slider cannot spam the disk; the file is written
 * immediately when the ClickGUI closes and on JVM shutdown. Loading is lazy
 * (first client tick) and tolerant: unknown or malformed entries are simply
 * ignored and defaults kept.
 */
public final class ConfigManager {

	private static final String FILE_NAME = "combatcore.json";
	private static final int SAVE_DELAY_TICKS = 20;

	private static File file;
	private static boolean loaded;
	private static boolean dirty;
	private static int saveIn;

	/** Last codes written to (or read from) the file, for change detection. */
	private static int mirrorGuiKey = -1;
	private static final Map<String, Integer> mirrorKeys = new HashMap<String, Integer>();

	private ConfigManager() {
	}

	/** The GUI key's current code (live vanilla binding). */
	public static int getGuiKey() {
		return CombatCoreMod.GUI_KEY.getKeyCode();
	}

	/** Idempotent; runs on the first client tick when {@code Minecraft} exists. */
	public static void ensureLoaded(Minecraft client) {
		if (loaded) {
			return;
		}
		loaded = true;

		try {
			File dir = new File(client.gameDir, "config");
			if (!dir.isDirectory() && !dir.mkdirs()) {
				CombatCoreMod.LOGGER.warn("Could not create config directory {}", dir);
				return;
			}

			file = new File(dir, FILE_NAME);
			if (file.isFile()) {
				load();
			}
		} catch (Exception e) {
			CombatCoreMod.LOGGER.warn("Could not load config, using defaults", e);
		}
	}

	/** Request a save; flushed by {@link #tick()} after a short idle delay. */
	public static void markDirty() {
		dirty = true;
		saveIn = SAVE_DELAY_TICKS;
	}

	/** Debounced save, called once per client tick. */
	public static void tick() {
		if (!loaded) {
			return;
		}

		// Mirror live binding codes (Controls screen / ClickGUI rebinds).
		if (syncKeys()) {
			markDirty();
		}

		if (!dirty) {
			return;
		}

		if (saveIn > 0) {
			saveIn--;
			return;
		}

		save();
	}

	/** Immediate save if anything changed (ClickGUI close, shutdown hook). */
	public static void saveNow() {
		if (dirty) {
			save();
		}
	}

	private static void load() throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		try {
			JsonElement parsed = new JsonParser().parse(reader);
			if (parsed == null || !parsed.isJsonObject()) {
				return;
			}

			JsonObject root = parsed.getAsJsonObject();

			if (root.has("guiKey") && root.get("guiKey").isJsonPrimitive()) {
				CombatCoreMod.GUI_KEY.setKeyCode(root.get("guiKey").getAsInt());
			}

			if (root.has("modules") && root.get("modules").isJsonObject()) {
				JsonObject modules = root.getAsJsonObject("modules");
				for (Module module : CombatCoreMod.MODULES.getModules()) {
					JsonElement entry = modules.get(module.getName());
					if (entry != null && entry.isJsonObject()) {
						loadModule(module, entry.getAsJsonObject());
					}
				}
			}

			// The file may have moved keys around: re-route key events to the
			// new codes and remember the live values, so this load does not
			// immediately dirty the config again.
			KeyBinding.resetMapping();
			syncKeys();
		} finally {
			reader.close();
		}
	}

	private static void loadModule(Module module, JsonObject entry) {
		if (entry.has("key") && entry.get("key").isJsonPrimitive()) {
			module.setKeyCode(entry.get("key").getAsInt());
		}

		if (entry.has("settings") && entry.get("settings").isJsonObject()) {
			JsonObject settings = entry.getAsJsonObject("settings");
			for (Setting<?> setting : module.getSettings()) {
				JsonElement value = settings.get(setting.getName());
				if (value == null || !value.isJsonPrimitive() || !(setting instanceof NumberSetting)) {
					continue;
				}

				try {
					((NumberSetting) setting).setValue(value.getAsInt());
				} catch (RuntimeException ignored) {
					// Malformed value - keep the current/default one.
				}
			}
		}

		if (entry.has("enabled") && entry.get("enabled").isJsonPrimitive()) {
			module.setEnabled(entry.get("enabled").getAsBoolean());
		}
	}

	private static void save() {
		if (file == null) {
			return;
		}

		syncKeys();
		dirty = false;

		try {
			JsonObject root = new JsonObject();
			root.addProperty("guiKey", CombatCoreMod.GUI_KEY.getKeyCode());

			JsonObject modules = new JsonObject();
			for (Module module : CombatCoreMod.MODULES.getModules()) {
				modules.add(module.getName(), saveModule(module));
			}
			root.add("modules", modules);

			Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
			try {
				new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
			} finally {
				writer.close();
			}
		} catch (Exception e) {
			dirty = true;
			CombatCoreMod.LOGGER.warn("Could not save config to {}", file, e);
		}
	}

	/**
	 * Copies the live {@link KeyBinding} codes into the mirrors. Returns
	 * {@code true} when anything changed (i.e. the config should be saved).
	 */
	private static boolean syncKeys() {
		boolean changed = false;

		int guiKey = CombatCoreMod.GUI_KEY.getKeyCode();
		if (mirrorGuiKey != guiKey) {
			mirrorGuiKey = guiKey;
			changed = true;
		}

		for (Module module : CombatCoreMod.MODULES.getModules()) {
			Integer previous = mirrorKeys.get(module.getName());
			if (previous == null || previous.intValue() != module.getKeyCode()) {
				mirrorKeys.put(module.getName(), Integer.valueOf(module.getKeyCode()));
				changed = true;
			}
		}

		return changed;
	}

	private static JsonObject saveModule(Module module) {
		JsonObject entry = new JsonObject();
		entry.addProperty("enabled", module.isEnabled());
		entry.addProperty("key", module.getKeyCode());

		JsonObject settings = new JsonObject();
		for (Setting<?> setting : module.getSettings()) {
			Object value = setting.getValue();
			if (value instanceof Integer) {
				settings.addProperty(setting.getName(), (Integer) value);
			}
		}
		entry.add("settings", settings);

		return entry;
	}
}
