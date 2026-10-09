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
 * <p>Implemented as a short press of vanilla's use key (Hold ticks, default
 * 2): vanilla's own tick loop sees the pressed state and calls
 * {@code Minecraft.doUse()}, so the block respects every vanilla rule
 * (reach, cooldown, "already using item", not while a screen is open, ...).
 * The press is skipped entirely while the player is already using an item -
 * a forced release would interrupt eating, drinking or drawing a bow. Only
 * runs while a sword is the selected item; releasing restores the physical
 * right-click state, so holding right click yourself is never interrupted.
 *
 * <p>Settings: delay (ticks after the hit, 0 = same-tick press, applied on
 * the next client tick) and hold (how many ticks use stays pressed, 1-4;
 * a meaningful blockhit holds for roughly two to four ticks).
 */
public class BlockHit extends Module {

	private final NumberSetting delay = addSetting(new NumberSetting("Delay", 0, 0, 10, "t"));
	private final NumberSetting hold = addSetting(new NumberSetting("Hold", 2, 1, 4, "t"));

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

		// The null check is not dead code in this build: an empty hotbar slot
		// really is null (PlayerInventory allocates a plain ItemStack array
		// and PlayerInventory.tick() skips null slots).
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

		// Delay elapsed: press use for Hold ticks, vanilla does the rest.
		this.actionTimer = -1;
		if (client.player != null && client.player.hasItemInUse()) {
			// Already using an item (eating, drinking, drawing a bow, an
			// in-progress block): our forced release would interrupt it, so
			// skip this blockhit instead.
			return;
		}
		Keys.press(client.options.useKey);
		this.pressTicksLeft = this.hold.getValue();
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
