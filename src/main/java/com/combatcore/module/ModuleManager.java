package com.combatcore.module;

import com.combatcore.config.ConfigManager;
import com.combatcore.module.combat.BlockHit;
import com.combatcore.module.combat.JumpReset;
import com.combatcore.module.combat.STap;
import com.combatcore.module.combat.WTap;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.text.LiteralText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry and per-tick dispatcher for all modules.
 *
 * <p>Called from {@link com.combatcore.mixin.MinecraftMixin} at the head of
 * every client tick and from {@link com.combatcore.mixin
 * .ClientPlayerInteractionManagerMixin} on every client-side attack.
 * Dispatch rules (kept deliberately strict so nothing runs in the wrong
 * context):
 *
 * <ul>
 *   <li>key bindings only toggle while no screen is open (the click counters
 *       are always drained, so typing in chat never toggles anything and no
 *       stale press survives a screen),</li>
 *   <li>{@code onTick} only runs for enabled modules with a player in a
 *       world and no screen open,</li>
 *   <li>when a screen opens, every enabled module has its simulated keys
 *       restored once, so no tap can leak into a menu,</li>
 *   <li>{@code onAttack} only runs for enabled modules.</li>
 * </ul>
 */
public final class ModuleManager {

	private final List<Module> modules = new ArrayList<Module>();
	private boolean screenWasOpen;

	public ModuleManager() {
		this.modules.add(new WTap());
		this.modules.add(new STap());
		this.modules.add(new JumpReset());
		this.modules.add(new BlockHit());
	}

	public List<Module> getModules() {
		return Collections.unmodifiableList(this.modules);
	}

	public Module get(String name) {
		for (Module module : this.modules) {
			if (module.getName().equalsIgnoreCase(name)) {
				return module;
			}
		}

		return null;
	}

	/** Per-tick dispatch, invoked at the head of {@code Minecraft.tick()}. */
	public void tick(Minecraft client) {
		if (client.options == null) {
			return;
		}

		boolean screenOpen = client.screen != null;

		if (screenOpen && !this.screenWasOpen) {
			// A screen just opened: never keep a simulated key pressed inside it.
			this.restoreEnabled(client);
		}
		this.screenWasOpen = screenOpen;

		boolean inGame = client.player != null && client.world != null;

		for (Module module : this.modules) {
			// Always drain the event-driven click counter so presses made while
			// a screen was open never produce a stale toggle later.
			boolean pressed = module.consumeClick();

			if (pressed && !screenOpen) {
				module.toggle();
				ConfigManager.markDirty();
				this.sendToggleMessage(client, module);
			}

			if (module.isEnabled() && !screenOpen && inGame) {
				module.onTick(client);
			}
		}
	}

	/** Client-side attack hook; only enabled modules are notified. */
	public void onAttack(Entity target) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.screen != null || client.player == null) {
			return;
		}

		for (Module module : this.modules) {
			if (module.isEnabled()) {
				module.onAttack(target);
			}
		}
	}

	/** Force every enabled module to release its simulated keys. */
	public void restoreEnabled(Minecraft client) {
		for (Module module : this.modules) {
			if (module.isEnabled()) {
				module.restoreKeys();
			}
		}
	}

	private void sendToggleMessage(Minecraft client, Module module) {
		if (client.player == null) {
			return;
		}

		client.player.sendMessage(new LiteralText("§8[§7Combat Core§8] §r" + module.getName()
				+ ": " + (module.isEnabled() ? "§aON" : "§cOFF")));
	}
}
