package com.combatcore.mixin;

import com.combatcore.CombatCoreMod;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Entry point for everything that happens per client tick: key bindings,
 * module timers, the ClickGUI key and config autosaving.
 *
 * <p>Feather name: {@code Minecraft.runTick()} -&gt; {@code tick()}. Injected
 * at the head so simulated key state is set before vanilla samples the
 * movement input and mouse buttons later in the same tick.
 *
 * <p>Handler names carry the {@code combatcore$} prefix so they can never
 * collide with other mods' mixins on the same target (Polyfrost / OneClient
 * compatibility).
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

	@Inject(method = "tick()V", at = @At("HEAD"))
	private void combatcore$onTick(CallbackInfo ci) {
		CombatCoreMod.onClientTick((Minecraft) (Object) this);
	}
}
