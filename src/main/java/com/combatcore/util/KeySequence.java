package com.combatcore.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.options.KeyBinding;

import java.util.function.BooleanSupplier;

/**
 * Shared delay-plus-hold key sequence used by the combat modules.
 *
 * <p>Workflow: call {@link #queue(int, int, KeyBinding[], KeyBinding[])} when
 * an action should run (usually from {@code onAttack}), {@link #tick(Minecraft)}
 * once per client tick from {@code onTick}, and {@link #cancel()} from
 * {@code restoreKeys}. The sequence counts down the delay, then forces every
 * binding in the press list to pressed and every binding in the release list
 * to released for {@code holdTicks} ticks, and finally restores all of them
 * through {@link Keys}.
 *
 * <p>The delay-hold state machine lives here so modules only pick bindings
 * and read their settings - W-Tap is "release forward", S-Tap is "release
 * forward, press back", Block Hit is "press use". Key ownership also
 * centralises here: every forced binding is owned by this sequence, which is
 * what lets W-Tap and S-Tap safely share the forward key ({@link Keys}
 * reference-counts owners per binding).
 *
 * <p>An optional start veto can be supplied at construction; it is consulted
 * once, when the delay elapses. When it rejects, the sequence goes idle
 * without touching any key (Block Hit uses this to never interrupt an
 * in-progress use of another item).
 */
public final class KeySequence {

	private static final KeyBinding[] NOTHING = new KeyBinding[0];

	/** Optional veto, consulted once when the hold is about to start. */
	private final BooleanSupplier startCheck;

	/** Ticks until the hold starts, {@code -1} = idle. */
	private int delay = -1;
	/** Hold length for the current/next hold, in ticks. */
	private int holdTicks;
	/** Ticks left with the keys held, 0 = not holding. */
	private int holdTicksLeft;

	private KeyBinding[] toPress = NOTHING;
	private KeyBinding[] toRelease = NOTHING;

	public KeySequence() {
		this(null);
	}

	/** @param startCheck veto consulted before the hold starts, may be null */
	public KeySequence(BooleanSupplier startCheck) {
		this.startCheck = startCheck;
	}

	/** True while a hold is queued or running. */
	public boolean isBusy() {
		return this.delay >= 0 || this.holdTicksLeft > 0;
	}

	/**
	 * Schedule the hold. Bindings may be {@code null} for "none"; {@code holdTicks}
	 * is clamped to at least 1 and {@code delayTicks} to at least 0. Callers
	 * should only queue while {@link #isBusy()} is false.
	 */
	public void queue(int delayTicks, int holdTicks, KeyBinding[] press, KeyBinding[] release) {
		this.delay = Math.max(0, delayTicks);
		this.holdTicks = Math.max(1, holdTicks);
		this.toPress = press == null ? NOTHING : press;
		this.toRelease = release == null ? NOTHING : release;
	}

	/** Advance one client tick: countdown, start hold, or end hold. */
	public void tick(Minecraft client) {
		if (this.holdTicksLeft > 0) {
			if (--this.holdTicksLeft == 0) {
				restoreAll();
			}
			return;
		}

		if (this.delay < 0) {
			return;
		}
		// Counts down even when the hold starts this tick (delay 0 -> -1).
		if (this.delay-- > 0) {
			return;
		}

		if (this.startCheck != null && !this.startCheck.getAsBoolean()) {
			return; // vetoed: drop the action, keys stay untouched
		}

		for (int i = 0; i < this.toPress.length; i++) {
			Keys.press(this, this.toPress[i]);
		}
		for (int i = 0; i < this.toRelease.length; i++) {
			Keys.set(this, this.toRelease[i], false);
		}
		this.holdTicksLeft = this.holdTicks;
	}

	/** Abort any queued/running hold and restore the keys immediately. */
	public void cancel() {
		this.delay = -1;
		if (this.holdTicksLeft > 0) {
			this.holdTicksLeft = 0;
			restoreAll();
		}
	}

	private void restoreAll() {
		for (int i = 0; i < this.toPress.length; i++) {
			Keys.restore(this, this.toPress[i]);
		}
		for (int i = 0; i < this.toRelease.length; i++) {
			Keys.restore(this, this.toRelease[i]);
		}
	}
}
