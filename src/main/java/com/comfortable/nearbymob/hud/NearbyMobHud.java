package com.comfortable.nearbymob.hud;

import com.comfortable.nearbymob.config.HudLayout;
import com.comfortable.nearbymob.config.NearbyMobConfig;
import com.comfortable.nearbymob.tracker.MobTracker;
import com.comfortable.nearbymob.tracker.MobTracker.MobGroup;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * Draws the nearby tracked mobs as real, live 3D entities. The models go
 * through the same GUI picture-in-picture path that the vanilla inventory uses
 * for the player preview, so no external models or custom render types are
 * involved.
 */
public final class NearbyMobHud implements HudElement {

	private static float spinDegrees;
	private static long lastFrameNanos;

	@Override
	public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.options.hideGui || minecraft.level == null) {
			return;
		}

		NearbyMobConfig config = NearbyMobConfig.get();
		if (!config.hudEnabled) {
			return;
		}

		List<MobGroup> groups = MobTracker.getGroups();
		if (groups.isEmpty() || groups.get(0).representative().level() != minecraft.level) {
			return;
		}

		advanceSpin(config.rotationSpeed);

		HudLayout layout = HudLayout.fromConfig(config);
		HudRenderer.render(
			guiGraphics,
			config,
			layout,
			groups,
			deltaTracker.getGameTimeDeltaPartialTick(false),
			spinDegrees
		);
	}

	private static void advanceSpin(int degreesPerSecond) {
		long now = System.nanoTime();

		if (lastFrameNanos != 0L) {
			float deltaSeconds = (now - lastFrameNanos) / 1_000_000_000.0F;
			deltaSeconds = Math.min(deltaSeconds, 0.25F);
			spinDegrees = (spinDegrees + deltaSeconds * degreesPerSecond) % 360.0F;
		}

		lastFrameNanos = now;
	}
}
