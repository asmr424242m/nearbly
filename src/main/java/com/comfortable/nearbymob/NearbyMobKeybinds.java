package com.comfortable.nearbymob;

import com.comfortable.nearbymob.config.NearbyMobConfigScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Vanilla key binding entry for the mod's own settings screen.
 *
 * <p>The category is registered with Fabric's key binding API, so the binding
 * appears in Options → Controls → Key Binds without Mod Menu or any other mod.</p>
 */
public final class NearbyMobKeybinds {

	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
		Identifier.fromNamespaceAndPath(NearbyMobClient.MOD_ID, "main")
	);

	private static KeyMapping openSettings;

	private NearbyMobKeybinds() {
	}

	public static void register() {
		openSettings = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.nearbymob.open_settings",
			com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_O,
			CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openSettings.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(NearbyMobConfigScreen.create(null));
				}
			}
		});
	}
}
