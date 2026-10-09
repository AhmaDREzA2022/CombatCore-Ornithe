package com.combatcore.module.combat;

import com.combatcore.module.Module;
import com.combatcore.module.setting.NumberSetting;
import com.combatcore.util.Keys;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

/**
 * S-Tap: briefly presses S (backwards) after landing a hit. Forward input
 * drops to the sprint threshold for the duration, which cancels sprinting -
 * same sprint-reset idea as W-Tap, but without releasing W, so momentum is
 * kept while sprint is reset.
 *
 * <p>Settings: delay (ticks after the hit, 0 = next tick) and duration
 * (how long S is held, in ticks).
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
			}
			return;
		}

		if (this.actionTimer < 0) {
			return;
		}

		if (this.actionTimer-- > 0) {
			return;
		}

		// Delay elapsed: hold S for the configured duration.
		this.actionTimer = -1;
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
		}
	}
}
