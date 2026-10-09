package com.combatcore.module.combat;

import com.combatcore.module.Module;
import com.combatcore.module.setting.NumberSetting;
import com.combatcore.util.KeySequence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.entity.Entity;

import java.util.Random;

/**
 * W-Tap: shortly after landing a hit, releases the forward key for one tick
 * and then presses it again. The one-tick release makes vanilla cancel
 * sprinting (movement input drops below the sprint threshold in
 * {@code LocalClientPlayerEntity}'s per-tick update, which sends a stop
 * sprinting packet), and the re-press re-engages sprint afterwards - so your
 * next hit is a fresh sprint hit again (sprint reset / full knockback).
 *
 * <p>Settings: delay (ticks after the hit, 0 = next tick) and chance (%).
 * The delay-hold state machine lives in {@link KeySequence}.
 */
public class WTap extends Module {

	/** How long W stays released, in ticks. One tick is the classic tap. */
	private static final int RELEASE_TICKS = 1;

	private final NumberSetting delay = addSetting(new NumberSetting("Delay", 0, 0, 10, "t"));
	private final NumberSetting chance = addSetting(new NumberSetting("Chance", 100, 0, 100, "%"));

	private final Random random = new Random();
	private final KeySequence sequence = new KeySequence();

	public WTap() {
		super("W-Tap", "Combat", "Releases and re-presses W after a hit to reset sprint");
	}

	@Override
	public void onAttack(Entity target) {
		if (this.sequence.isBusy()) {
			return; // a tap is already queued or running
		}

		// Only meaningful while sprinting - otherwise it is just a movement
		// stutter. Checked before the chance roll so nothing is consumed.
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.options == null
				|| !client.player.isSprinting()) {
			return;
		}

		if (this.random.nextInt(100) >= this.chance.getValue()) {
			return;
		}

		this.sequence.queue(this.delay.getValue(), RELEASE_TICKS,
				null, new KeyBinding[]{client.options.forwardKey});
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
