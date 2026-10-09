package com.combatcore;

import com.combatcore.config.ConfigManager;
import com.combatcore.gui.ClickGuiScreen;
import com.combatcore.module.Module;
import com.combatcore.module.ModuleManager;
import com.combatcore.util.Keys;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

/**
 * Combat Core - a small, client-side only PvP helper for Ornithe/Calamus
 * 1.8.9 with four modules: W-Tap, S-Tap, Jump Reset and Block Hit.
 *
 * <p>Wiring:
 *
 * <ul>
 *   <li>{@link com.combatcore.mixin.MinecraftMixin} feeds
 *       {@link #onClientTick(Minecraft)} at the head of every client tick
 *       (module ticks, key bindings, ClickGUI key, config autosave),</li>
 *   <li>{@link com.combatcore.mixin.ClientPlayerInteractionManagerMixin}
 *       feeds {@link #onAttack(Entity)} whenever the client attacks an
 *       entity (the "hit landed" signal),</li>
 *   <li>{@link com.combatcore.mixin.GameOptionsMixin} registers every key
 *       binding before {@code options.txt} is parsed, so keys are rebindable
 *       in Options - Controls and survive restarts,</li>
 *   <li>settings live in {@code config/combatcore.json} (see
 *       {@link ConfigManager}) and the ClickGUI opens with Right Shift.</li>
 * </ul>
 *
 * <p>No reach, aim, velocity or render cheats - this mod only ever simulates
 * input the player could produce themselves, through vanilla key bindings.
 */
public class CombatCoreMod implements ClientModInitializer {

	/** Vanilla controls-screen category for every key binding of this mod. */
	public static final String KEY_CATEGORY = "Combat Core";

	public static final Logger LOGGER = LogManager.getLogger("Combat Core");

	public static final ModuleManager MODULES = new ModuleManager();

	/** Opens the ClickGUI (Right Shift by default, reconfigurable). */
	public static final KeyBinding GUI_KEY =
			new KeyBinding("Open Combat Core ClickGUI", Keyboard.KEY_RSHIFT, KEY_CATEGORY);

	/**
	 * Ignore GUI-key presses for a moment after one fired: key repeat keeps
	 * arriving while the key is physically held and would otherwise reopen
	 * the GUI right after it was closed.
	 */
	private static final int GUI_COOLDOWN_TICKS = 2;

	private static int guiCooldown;

	@Override
	public void onInitializeClient() {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				ConfigManager.saveNow();
			}
		}, "Combat Core config saver"));

		LOGGER.info("Loaded - {} modules, ClickGUI key: {}",
				MODULES.getModules().size(), Keys.getName(GUI_KEY.getKeyCode()));
	}

	/**
	 * Called from {@link com.combatcore.mixin.MinecraftMixin} at the head of
	 * every client tick.
	 */
	public static void onClientTick(Minecraft client) {
		if (client.options == null) {
			return;
		}

		ConfigManager.ensureLoaded(client);

		// ClickGUI key (Right Shift by default): event-driven click counter,
		// always drained, only acted on while no screen is open. Closing is
		// handled inside the screen itself.
		boolean guiClick = GUI_KEY.consumeClick();
		if (guiClick) {
			if (client.screen == null && guiCooldown == 0) {
				client.openScreen(new ClickGuiScreen());
			}
			guiCooldown = GUI_COOLDOWN_TICKS;
		} else if (guiCooldown > 0) {
			guiCooldown--;
		}

		MODULES.tick(client);
		ConfigManager.tick();
	}

	/** Called by the ClickGUI when it closed itself with the GUI key. */
	public static void suppressGuiOpen() {
		guiCooldown = GUI_COOLDOWN_TICKS;
	}

	/**
	 * Appends our key bindings to the controls screen so they can be listed,
	 * rebound and persisted through {@code options.txt}. Called from
	 * {@link com.combatcore.mixin.GameOptionsMixin} at the head of
	 * {@code GameOptions.load()}; guarded against running twice because
	 * options can reload.
	 */
	public static void registerKeys(GameOptions options) {
		KeyBinding[] bindings = options.keyBindings;
		if (bindings == null) {
			return;
		}

		for (KeyBinding key : allKeys()) {
			if (contains(bindings, key)) {
				continue;
			}

			KeyBinding[] updated = new KeyBinding[bindings.length + 1];
			System.arraycopy(bindings, 0, updated, 0, bindings.length);
			updated[bindings.length] = key;
			bindings = updated;
		}

		options.keyBindings = bindings;
	}

	/** The GUI key plus every module's toggle key. */
	public static List<KeyBinding> allKeys() {
		List<KeyBinding> keys = new ArrayList<KeyBinding>();
		keys.add(GUI_KEY);
		for (Module module : MODULES.getModules()) {
			keys.add(module.getKeyBinding());
		}
		return keys;
	}

	private static boolean contains(KeyBinding[] bindings, KeyBinding key) {
		for (KeyBinding binding : bindings) {
			if (binding == key) {
				return true;
			}
		}

		return false;
	}

	/** Called from {@code ClientPlayerInteractionManagerMixin} on attack. */
	public static void onAttack(Entity target) {
		MODULES.onAttack(target);
	}
}
