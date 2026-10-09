package com.combatcore.util;

import net.minecraft.client.options.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Small helpers around vanilla key simulation.
 *
 * <p>Feather 1.8.9 exposes exactly what is needed without any mixin:
 * {@link KeyBinding#set(int, boolean)} forces the pressed state of the binding
 * registered for a key code (vanilla reads that state once per tick for
 * movement input and mouse buttons), so a forced state persists until we
 * restore it - which every module does on disable, when a screen opens, and
 * after a fixed number of ticks.
 *
 * <p>Restoring deliberately does <em>not</em> use {@code GameOptions.isPressed}
 * (it reads {@code Keyboard.isKeyDown}, which is not reliable in this
 * environment). Instead the pre-force state of the binding is remembered when
 * forcing starts and written back on restore - unless a real key event
 * overwrote the binding in the meantime, in which case that event already
 * carries the fresh physical state and is left alone. Result: a simulated tap
 * never fights the real keyboard, and a player holding a key keeps moving.
 *
 * <p>Every state change flows through {@link #set}, so callers must pair
 * forcing with {@link #restore}; {@link #press} is shorthand for forcing on.
 */
public final class Keys {

	/** Pre-force pressed state, keyed by identity. */
	private static final Map<KeyBinding, Boolean> PRE_FORCE = new IdentityHashMap<KeyBinding, Boolean>();

	/** The state we last forced each binding into. */
	private static final Map<KeyBinding, Boolean> FORCED = new IdentityHashMap<KeyBinding, Boolean>();

	private Keys() {
	}

	/** Force a binding's pressed state (does not touch physical key state). */
	public static void set(KeyBinding binding, boolean pressed) {
		if (binding == null || binding.getKeyCode() == 0) {
			return;
		}

		if (!PRE_FORCE.containsKey(binding)) {
			// First force of this run: remember what vanilla believed so far
			// (the event-maintained state, i.e. the physical key).
			PRE_FORCE.put(binding, Boolean.valueOf(binding.isPressed()));
		}

		FORCED.put(binding, Boolean.valueOf(pressed));
		KeyBinding.set(binding.getKeyCode(), pressed);
	}

	/** Press a binding for this tick. */
	public static void press(KeyBinding binding) {
		set(binding, true);
	}

	/**
	 * Undo a forced state. If no key event intervened during the forced
	 * window, the pre-force state is written back; otherwise the binding
	 * already holds fresh physical state from that event and is left as is.
	 */
	public static void restore(KeyBinding binding) {
		if (binding == null) {
			return;
		}

		Boolean pre = PRE_FORCE.remove(binding);
		Boolean forcedTo = FORCED.remove(binding);

		if (pre == null || forcedTo == null) {
			// Never forced by us - the binding is already event-accurate.
			return;
		}

		if (Boolean.valueOf(binding.isPressed()).equals(forcedTo)) {
			KeyBinding.set(binding.getKeyCode(), pre.booleanValue());
		}
	}

	/** Human readable name of a raw LWJGL key code, {@code "None"} for 0. */
	public static String getName(int keyCode) {
		return keyCode <= 0 ? "None" : Keyboard.getKeyName(keyCode);
	}
}
