package com.combatcore.mixin;

import com.combatcore.CombatCoreMod;
import net.minecraft.client.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hit detection: {@code ClientPlayerInteractionManager.attackEntity(...)} is
 * the single place the client dispatches an attack on an entity (called from
 * {@code Minecraft.doAttack()} once the crosshair points at an entity), so
 * this fires exactly when the player lands a hit - never when breaking a
 * block or interacting. Injected at the tail, i.e. after the attack packet
 * has been sent and the swing played.
 *
 * <p>Handler names carry the {@code combatcore$} prefix so they can never
 * collide with other mods' mixins on the same target (Polyfrost / OneClient
 * compatibility).
 */
@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

	@Inject(
			method = "attackEntity(Lnet/minecraft/entity/living/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V",
			at = @At("TAIL")
	)
	private void combatcore$onAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
		CombatCoreMod.onAttack(target);
	}
}
