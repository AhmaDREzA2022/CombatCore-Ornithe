package com.combatcore.module.combat;

import com.combatcore.module.Module;
import com.combatcore.module.setting.NumberSetting;
import com.combatcore.util.Keys;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;

/**
 * Block Hit: right-clicks (blocks with the sword) a short moment after
 * landing a hit, then releases again - the classic 1.8.9 "blockhit" cadence.
 *
 * <p>Implemented as a one-tick press of vanilla's use key: vanilla's own tick
 * loop sees the pressed state and calls {@code Minecraft.doUse()}, so the
 * block respects every vanilla rule (reach, cooldown, "already using item",
 * not while a screen is open, ...). Only runs while a sword is the selected
 * item; releasing restores the physical right-click state, so holding right
 * click yourself is never interrupted.
 *
 * <p>Setting: delay (ticks after the hit, 0 = same-tick press, applied on the
 * next client tick).
 */
public class BlockHit extends Module {

	private final NumberSetting delay = addSetting(new NumberSetting("Delay", 0, 0, 10, "t"));

	/** Ticks until use is pressed, {@code -1} = idle. */
	private int actionTimer = -1;
	/** Ticks left with use forced pressed. */
	private int pressTicksLeft;

	public BlockHit() {
		super("Block Hit", "Combat", "Right-clicks with the sword shortly after a hit");
	}

	@Override
	public void onAttack(Entity target) {
		if (this.actionTimer >= 0 || this.pressTicksLeft > 0) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return;
		}

		ItemStack stack = client.player.inventory.getSelectedItem();
		if (stack == null || !(stack.getItem() instanceof SwordItem)) {
			return;
		}

		this.actionTimer = this.delay.getValue();
	}

	@Override
	public void onTick(Minecraft client) {
		if (this.pressTicksLeft > 0) {
			if (--this.pressTicksLeft == 0) {
				Keys.restore(client.options.useKey);
			}
			return;
		}

		if (this.actionTimer < 0) {
			return;
		}

		if (this.actionTimer-- > 0) {
			return;
		}

		// Delay elapsed: one-tick right click, vanilla does the rest.
		this.actionTimer = -1;
		Keys.press(client.options.useKey);
		this.pressTicksLeft = 1;
	}

	@Override
	public void restoreKeys() {
		this.actionTimer = -1;
		this.pressTicksLeft = 0;
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.options != null) {
			Keys.restore(client.options.useKey);
		}
	}
}
