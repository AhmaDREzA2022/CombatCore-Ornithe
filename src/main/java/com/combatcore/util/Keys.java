package com.combatcore.util;

import net.minecraft.client.options.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

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
 * <p>Forced state is <em>owner-tagged</em>: every {@link #set} names the
 * module (or {@link KeySequence}) forcing the binding, and the pre-force
 * snapshot is only written back when the last owner calls {@link #restore}.
 * That makes it safe for two modules to force the same binding at overlapping
 * times - e.g. W-Tap and S-Tap both release the forward key - without one
 * module's restore discarding another's intent.
 *
 * <p>Restoring deliberately does <em>not</em> use {@code GameOptions.isPressed}
 * (it reads {@code Keyboard.isKeyDown}, which is not reliable in this
 * environment). Instead the pre-force state of the binding is remembered when
 * the first owner forces it and written back when the last owner releases -
 * unless a real key event overwrote the binding in the meantime, in which
 * case that event already carries the fresh physical state and is left alone.
 * Result: a simulated tap never fights the real keyboard, and a player holding
 * a key keeps moving.
 *
 * <p>Every state change flows through {@link #set}, so callers must pair
 * forcing with {@link #restore}; {@link #press} is shorthand for forcing on.
 */
public final class Keys {

	/** Pre-force pressed state, keyed by identity. */
	private static final Map<KeyBinding, Boolean> PRE_FORCE = new IdentityHashMap<KeyBinding, Boolean>();

	/** The state we last forced each binding into. */
	private static final Map<KeyBinding, Boolean> FORCED = new IdentityHashMap<KeyBinding, Boolean>();

	/** Owners currently forcing each binding (identity-based on the owner). */
	private static final Map<KeyBinding, Set<Object>> OWNERS =
			new IdentityHashMap<KeyBinding, Set<Object>>();

	private Keys() {
	}

	/**
	 * Force a binding's pressed state on behalf of {@code owner} (does not
	 * touch physical key state). The pre-force snapshot is taken only when
	 * the first owner starts forcing this binding.
	 */
	public static void set(Object owner, KeyBinding binding, boolean pressed) {
		if (owner == null || binding == null || binding.getKeyCode() == 0) {
			return;
		}

		Set<Object> owners = OWNERS.get(binding);
		if (owners == null) {
			owners = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
			OWNERS.put(binding, owners);
		}
		if (owners.isEmpty()) {
			// First force of this run: remember what vanilla believed so far
			// (the event-maintained state, i.e. the physical key).
			PRE_FORCE.put(binding, Boolean.valueOf(binding.isPressed()));
		}

		owners.add(owner);
		FORCED.put(binding, Boolean.valueOf(pressed));
		KeyBinding.set(binding.getKeyCode(), pressed);
	}

	/** Press a binding for this tick on behalf of {@code owner}. */
	public static void press(Object owner, KeyBinding binding) {
		set(owner, binding, true);
	}

	/**
	 * Undo {@code owner}'s forced state. If other owners are still forcing the
	 * binding, nothing happens yet; once the last owner releases, the
	 * pre-force state is written back (unless a real key event intervened
	 * meanwhile, in which case the binding already holds fresh physical state
	 * and is left as is).
	 */
	public static void restore(Object owner, KeyBinding binding) {
		if (owner == null || binding == null) {
			return;
		}

		Set<Object> owners = OWNERS.get(binding);
		if (owners == null || !owners.remove(owner)) {
			// Never forced by this owner - the binding is already event-accurate.
			return;
		}

		if (!owners.isEmpty()) {
			// Another module still forces this binding: keep the forced state
			// and the pre-force snapshot until the last owner releases.
			return;
		}

		Boolean pre = PRE_FORCE.remove(binding);
		Boolean forcedTo = FORCED.remove(binding);
		OWNERS.remove(binding);

		if (pre == null || forcedTo == null) {
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
