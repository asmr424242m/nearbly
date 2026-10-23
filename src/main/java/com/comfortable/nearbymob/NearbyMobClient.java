package com.comfortable.nearbymob;

import com.comfortable.nearbymob.hud.NearbyMobHud;
import com.comfortable.nearbymob.tracker.MobTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entrypoint for Nearby Mob HUD.
 */
public class NearbyMobClient implements ClientModInitializer {

	public static final String MOD_ID = "nearbymob";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		NearbyMobKeybinds.register();
		ClientTickEvents.END_CLIENT_TICK.register(MobTracker::tick);

		HudElementRegistry.addLast(
			Identifier.fromNamespaceAndPath(MOD_ID, "nearby_mob"),
			new NearbyMobHud()
		);

		LOGGER.info("[{}] client initialised", MOD_ID);
	}
}
