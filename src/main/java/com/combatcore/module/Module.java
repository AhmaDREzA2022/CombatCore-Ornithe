package com.combatcore.module;

import com.combatcore.CombatCoreMod;
import com.combatcore.module.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for every Combat Core module.
 *
 * <p>A module is a named, categorised feature with a toggle state, a vanilla
 * {@link KeyBinding} toggle key (LWJGL key code {@code 0} = unbound) and a list
 * of {@link Setting}s. The lifecycle hooks are driven by
 * {@link com.combatcore.module.ModuleManager}, which in turn is fed by the
 * mixins in {@code com.combatcore.mixin}:
 *
 * <ul>
 *   <li>{@link #onAttack(Entity)} - called when the client attacks an entity
 *       ({@code ClientPlayerInteractionManager.attackEntity}),</li>
 *   <li>{@link #onTick(Minecraft)} - called once per client tick
 *       ({@code Minecraft.tick}) while the module is enabled and no screen is
 *       open,</li>
 *   <li>{@link #onEnable()} / {@link #onDisable()} - toggle lifecycle,</li>
 *   <li>{@link #restoreKeys()} - puts any simulated key back to its previous
 *       state (called on disable and when a screen opens).</li>
 * </ul>
 *
 * <p>The toggle key is a real vanilla {@link KeyBinding}: vanilla feeds its
 * click counter from the {@code Keyboard.next()} event queue, which is
 * reliable even for presses shorter than one tick (raw
 * {@code Keyboard.isKeyDown} polling is not, so it is used nowhere).
 * Registering through the binding also means the key shows up under
 * Options - Controls and can be rebound there; the code is stored in the JSON
 * config as well (see {@link com.combatcore.config.ConfigManager}).
 */
public abstract class Module {

	private final String name;
	private final String category;
	private final String description;
	private final List<Setting<?>> settings = new ArrayList<Setting<?>>();
	private final KeyBinding keyBinding;

	private boolean enabled;

	protected Module(String name, String category, String description) {
		this.name = name;
		this.category = category;
		this.description = description;
		// Unbound (0) by default; created here so the binding registers itself
		// in vanilla's key tables before GameOptions.load() parses options.txt.
		this.keyBinding = new KeyBinding("Toggle " + name, 0, CombatCoreMod.KEY_CATEGORY);
	}

	protected <S extends Setting<?>> S addSetting(S setting) {
		this.settings.add(setting);
		return setting;
	}

	public String getName() {
		return this.name;
	}

	public String getCategory() {
		return this.category;
	}

	public String getDescription() {
		return this.description;
	}

	public List<Setting<?>> getSettings() {
		return Collections.unmodifiableList(this.settings);
	}

	public KeyBinding getKeyBinding() {
		return this.keyBinding;
	}

	public int getKeyCode() {
		return this.keyBinding.getKeyCode();
	}

	public void setKeyCode(int keyCode) {
		if (this.keyBinding.getKeyCode() == keyCode) {
			return;
		}

		this.keyBinding.setKeyCode(keyCode);
		// Re-route key events to the new code (vanilla does the same after a
		// rebind in the controls screen).
		KeyBinding.resetMapping();
	}

	/**
	 * Drains one pending press of the bound key. Event-driven: vanilla already
	 * discarded presses that arrived while a screen was open, and callers
	 * should drain unconditionally so no stale click survives.
	 */
	public boolean consumeClick() {
		return this.keyBinding.consumeClick();
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void toggle() {
		this.setEnabled(!this.enabled);
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}

		this.enabled = enabled;

		if (enabled) {
			this.onEnable();
		} else {
			this.onDisable();
		}
	}

	/** Toggle lifecycle, e.g. cancel running actions. */
	public void onEnable() {
	}

	public void onDisable() {
		this.restoreKeys();
	}

	/** Called once per client tick while enabled and no screen is open. */
	public void onTick(Minecraft client) {
	}

	/**
	 * Called when the client attacks an entity (hit about to be registered).
	 * Only called while enabled with no screen open.
	 */
	public void onAttack(Entity target) {
	}

	/** Put every key this module is currently forcing back to its real state. */
	public void restoreKeys() {
	}
}
