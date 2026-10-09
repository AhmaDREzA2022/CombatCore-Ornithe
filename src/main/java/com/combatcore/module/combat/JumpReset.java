package com.combatcore.module.combat;

import com.combatcore.module.Module;
import com.combatcore.module.setting.NumberSetting;
import com.combatcore.util.Keys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.entity.Entity;

import java.util.Random;

/**
 * Jump Reset: jumps on the tick the player takes knockback. Being airborne
 * during the knockback tick changes how much of the horizontal velocity is
 * applied (friction no longer bleeds it on that tick), which noticeably
 * reduces the distance you get pushed.
 *
 * <p>Detection: the local player's hurt timer ({@code LivingEntity
 * .damagedTimer}, Feather's name for MCP {@code hurtTime}) is set to 10 by
 * vanilla whenever health actually drops (see {@code LocalClientPlayerEntity
 * .damageTo}), so a rise of the value while it is being watched is a fresh
 * hit - including combo hits that re-set it before it reached 0. The rise is
 * read at the head of {@code Minecraft.tick()}: incoming packets execute in
 * the frame loop's scheduled-task drain <em>before</em> {@code tick()} runs,
 * so the head injection already observes the freshly set timer (the decrement
 * in {@code baseTick()} happens later, inside the entity tick) - detection and
 * the jump press are therefore on the same tick as the knockback.
 *
 * <p>This module keeps its own tiny press/restore bookkeeping instead of
 * {@link com.combatcore.util.KeySequence}: its trigger is a damage event, not
 * a delayed action, and consecutive hits re-press immediately (double-trigger
 * behaviour is an explicit, documented choice).
 *
 * <p>Settings: chance (%) and minimum hurt time (only trigger when the fresh
 * hurt timer is at least this value; a vanilla full hit sets it to 10).
 * The jump key is only pressed while on the ground, since an airborne press
 * would be a no-op anyway.
 */
public class JumpReset extends Module {

	private final NumberSetting chance = addSetting(new NumberSetting("Chance", 100, 0, 100, "%"));
	private final NumberSetting minHurtTime = addSetting(new NumberSetting("Min hurt time", 1, 1, 10, "t"));

	private final Random random = new Random();

	/** Hurt timer observed on the previous tick, for rise detection. */
	private int lastHurtTime;
	/** Ticks left with the jump key forced pressed. */
	private int jumpTicksLeft;

	public JumpReset() {
		super("Jump Reset", "Combat", "Jumps when taking knockback to reduce KB");
	}

	@Override
	public void onTick(Minecraft client) {
		LocalClientPlayerEntity player = client.player;

		int hurtTime = player.damagedTimer;
		boolean freshHit = hurtTime > 0 && hurtTime > this.lastHurtTime;
		this.lastHurtTime = hurtTime;

		if (this.jumpTicksLeft > 0 && --this.jumpTicksLeft == 0) {
			Keys.restore(this, client.options.jumpKey);
		}

		if (!freshHit || hurtTime < this.minHurtTime.getValue() || !player.onGround) {
			return;
		}

		if (this.random.nextInt(100) >= this.chance.getValue()) {
			return;
		}

		Keys.press(this, client.options.jumpKey);
		this.jumpTicksLeft = 1;
	}

	@Override
	public void restoreKeys() {
		this.jumpTicksLeft = 0;
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.options != null) {
			Keys.restore(this, client.options.jumpKey);
		}
	}
}
