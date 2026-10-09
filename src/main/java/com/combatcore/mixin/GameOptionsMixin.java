package com.combatcore.mixin;

import com.combatcore.CombatCoreMod;
import net.minecraft.client.options.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code GameOptions} builds its {@code keyBindings} array and then immediately
 * reads {@code options.txt} from its constructor. Registering Combat Core's
 * bindings at the head of that read means they are already in the array while
 * the saved key codes are parsed, so rebindings made in the controls screen
 * (or written into the JSON config) survive a restart. {@code GameOptions.load}
 * finishes with {@code KeyBinding.resetMapping()}, which then also routes key
 * events to our bindings.
 *
 * <p>Guarded the same way as SafeWalk's port: the append itself is
 * idempotent (see {@link CombatCoreMod#registerKeys(GameOptions)}), because
 * options can reload more than once.
 */
@Mixin(GameOptions.class)
public class GameOptionsMixin {

	@Inject(method = "load()V", at = @At("HEAD"))
	private void combatcore$registerKeys(CallbackInfo ci) {
		CombatCoreMod.registerKeys((GameOptions) (Object) this);
	}
}
