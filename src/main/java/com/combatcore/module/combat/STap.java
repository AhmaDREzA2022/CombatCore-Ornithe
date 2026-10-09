package com.combatcore.module.combat;

import com.combatcore.module.Module;
import com.combatcore.module.setting.NumberSetting;
import com.combatcore.util.Keys;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

/**
 * S-Tap: after landing a hit, releases W and briefly presses S (backwards)
 * for the configured duration.
 *
 * <p>Releasing forward is what actually cancels sprint in 1.8.9 - the game
 * only drops sprint when there is no forward/strafe input, so pressing S
 * alone while W is held does nothing to sprint state. The S press on top of
 * the release is what distinguishes this from a W-Tap: it gives the classic
 * camera-forward feel and a short reversal while sprint resets.
 *
 * <p>Settings: delay (ticks after the hit, 0 = next tick) and duration
 * (how long the keys are held, in ticks).
 */
public class STap extends Module {

	private final NumberSetting delay = addSetting(new NumberSetting("Delay", 0, 0, 10, "t"));
	private final NumberSetting duration = addSetting(new NumberSetting("Duration", 1, 1, 10, "t"));

	/** Ticks until S is pressed, {@code -1} = idle. */
	private int actionTimer = -1;
	/** Ticks left with S forced pressed. */
	private int pressTicksLeft;

	public STap() {
		super("S-Tap", "Combat", "Briefly presses S after a hit to reset sprint");
	}

	@Override
	public void onAttack(Entity target) {
		if (this.actionTimer >= 0 || this.pressTicksLeft > 0) {
			return;
		}

		this.actionTimer = this.delay.getValue();
	}

	@Override
	public void onTick(Minecraft client) {
		if (this.pressTicksLeft > 0) {
			if (--this.pressTicksLeft == 0) {
				Keys.restore(client.options.backKey);
				Keys.restore(client.options.forwardKey);
			}
			return;
		}

		if (this.actionTimer < 0) {
			return;
		}

		if (this.actionTimer-- > 0) {
			return;
		}

		// Delay elapsed: release W (this is what cancels sprint) and hold S
		// for the configured duration.
		this.actionTimer = -1;
		Keys.set(client.options.forwardKey, false);
		Keys.press(client.options.backKey);
		this.pressTicksLeft = this.duration.getValue();
	}

	@Override
	public void restoreKeys() {
		this.actionTimer = -1;
		this.pressTicksLeft = 0;
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.options != null) {
			Keys.restore(client.options.backKey);
			Keys.restore(client.options.forwardKey);
		}
	}
}
