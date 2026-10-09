package com.combatcore.module.combat;

import com.combatcore.module.Module;
import com.combatcore.module.setting.NumberSetting;
import com.combatcore.util.KeySequence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.options.KeyBinding;
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
 * a forced release would interrupt eating, drinking or drawing a bow (this is
 * the {@link KeySequence} start veto). Only runs while a sword is the
 * selected item; releasing restores the physical right-click state, so
 * holding right click yourself is never interrupted.
 *
 * <p>Settings: delay (ticks after the hit, 0 = same-tick press, applied on
 * the next client tick) and hold (how many ticks use stays pressed, 1-4;
 * a meaningful blockhit holds for roughly two to four ticks).
 */
public class BlockHit extends Module {

	private final NumberSetting delay = addSetting(new NumberSetting("Delay", 0, 0, 10, "t"));
	private final NumberSetting hold = addSetting(new NumberSetting("Hold", 2, 1, 4, "t"));

	private final KeySequence sequence = new KeySequence(this::pressAllowed);

	public BlockHit() {
		super("Block Hit", "Combat", "Right-clicks with the sword shortly after a hit");
	}

	@Override
	public void onAttack(Entity target) {
		if (this.sequence.isBusy()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.options == null) {
			return;
		}

		// The null check is not dead code in this build: an empty hotbar slot
		// really is null (PlayerInventory allocates a plain ItemStack array
		// and PlayerInventory.tick() skips null slots).
		ItemStack stack = client.player.inventory.getSelectedItem();
		if (stack == null || !(stack.getItem() instanceof SwordItem)) {
			return;
		}

		this.sequence.queue(this.delay.getValue(), this.hold.getValue(),
				new KeyBinding[]{client.options.useKey}, null);
	}

	@Override
	public void onTick(Minecraft client) {
		this.sequence.tick(client);
	}

	@Override
	public void restoreKeys() {
		this.sequence.cancel();
	}

	/** Start veto: never press while the player is already using an item. */
	private boolean pressAllowed() {
		Minecraft client = Minecraft.getInstance();
		return client == null || client.player == null || !client.player.hasItemInUse();
	}
}
