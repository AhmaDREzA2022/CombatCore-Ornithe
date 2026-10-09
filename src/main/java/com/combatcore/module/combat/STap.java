package com.combatcore.module.combat;

import com.combatcore.module.Module;
import com.combatcore.module.setting.NumberSetting;
import com.combatcore.util.KeySequence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.options.KeyBinding;
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
 * (how long the keys are held, in ticks). The delay-hold state machine lives
 * in {@link KeySequence}; sharing the forward key with W-Tap is safe because
 * {@code Keys} reference-counts owners per binding.
 */
public class STap extends Module {

	private final NumberSetting delay = addSetting(new NumberSetting("Delay", 0, 0, 10, "t"));
	private final NumberSetting duration = addSetting(new NumberSetting("Duration", 1, 1, 10, "t"));

	private final KeySequence sequence = new KeySequence();

	public STap() {
		super("S-Tap", "Combat", "Briefly presses S after a hit to reset sprint");
	}

	@Override
	public void onAttack(Entity target) {
		if (this.sequence.isBusy()) {
			return;
		}

		// Only meaningful while sprinting - otherwise it is just a movement
		// stutter.
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.options == null
				|| !client.player.isSprinting()) {
			return;
		}

		this.sequence.queue(this.delay.getValue(), this.duration.getValue(),
				new KeyBinding[]{client.options.backKey},
				new KeyBinding[]{client.options.forwardKey});
	}

	@Override
	public void onTick(Minecraft client) {
		this.sequence.tick(client);
	}

	@Override
	public void restoreKeys() {
		this.sequence.cancel();
	}
}
