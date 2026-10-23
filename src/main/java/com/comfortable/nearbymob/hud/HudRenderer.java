package com.comfortable.nearbymob.hud;

import com.comfortable.nearbymob.config.HudLayout;
import com.comfortable.nearbymob.config.NearbyMobConfig;
import com.comfortable.nearbymob.tracker.MobTracker.MobGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Shared HUD panel renderer. It is used by the real HUD and by the layout
 * editor, so both always display exactly the same result.
 */
public final class HudRenderer {

	private static final int BORDER_COLOR = 0x40FFFFFF;
	private static final int MAX_VISIBLE_GROUPS = 12;

	private HudRenderer() {
	}

	public static void render(
		GuiGraphics guiGraphics,
		NearbyMobConfig config,
		HudLayout layout,
		List<MobGroup> groups,
		float partialTick,
		float spinDegrees
	) {
		HudLayout fitted = layout.fitToScreen(guiGraphics.guiWidth(), guiGraphics.guiHeight());
		render(
			guiGraphics,
			config,
			fitted.left(guiGraphics.guiWidth()),
			fitted.top(guiGraphics.guiHeight()),
			fitted.width(),
			fitted.height(),
			groups,
			partialTick,
			spinDegrees
		);
	}

	public static void render(
		GuiGraphics guiGraphics,
		NearbyMobConfig config,
		int left,
		int top,
		int width,
		int height,
		List<MobGroup> groups,
		float partialTick,
		float spinDegrees
	) {
		int screenWidth = guiGraphics.guiWidth();
		int screenHeight = guiGraphics.guiHeight();
		left = NearbyMobConfig.clamp(left, 0, Math.max(0, screenWidth - 1));
		top = NearbyMobConfig.clamp(top, 0, Math.max(0, screenHeight - 1));
		width = Math.max(1, Math.min(width, screenWidth - left));
		height = Math.max(1, Math.min(height, screenHeight - top));

		int right = left + width;
		int bottom = top + height;
		int alpha = NearbyMobConfig.clamp(config.backgroundOpacity, 0, 100) * 255 / 100;

		if (alpha > 0) {
			guiGraphics.fill(left, top, right, bottom, alpha << 24);
		}

		if (config.showBorder) {
			guiGraphics.renderOutline(left, top, width, height, BORDER_COLOR);
		}

		float panelScale = Math.min(
			width / 100.0F,
			Math.max(0.3F, (height - (config.showLabel ? 12.0F : 0.0F)) / 120.0F)
		);
		float baseScale = Math.max(1.0F, config.hudScale * panelScale);

		if (groups.isEmpty()) {
			renderSingle(guiGraphics, config, left, top, right, bottom, baseScale, null, 0, partialTick, spinDegrees);
		} else if (groups.size() == 1) {
			MobGroup group = groups.get(0);
			renderSingle(
				guiGraphics,
				config,
				left,
				top,
				right,
				bottom,
				baseScale,
				group.representative(),
				group.count(),
				partialTick,
				spinDegrees
			);
		} else {
			renderMultiple(guiGraphics, config, left, top, right, bottom, baseScale, groups, partialTick, spinDegrees);
		}
	}

	private static void renderSingle(
		GuiGraphics guiGraphics,
		NearbyMobConfig config,
		int left,
		int top,
		int right,
		int bottom,
		float baseScale,
		@Nullable Mob mob,
		int count,
		float partialTick,
		float spinDegrees
	) {
		int labelHeight = config.showLabel ? 12 : 0;
		int modelTop = top + 2;
		int modelBottom = Math.max(modelTop + 1, bottom - labelHeight - 2);

		if (mob != null) {
			renderModel(
				guiGraphics,
				config,
				mob,
				partialTick,
				spinDegrees,
				baseScale,
				left + 3,
				modelTop,
				right - 3,
				modelBottom
			);
		} else {
			Font font = Minecraft.getInstance().font;
			Component placeholder = Component.translatable("nearbymob.hud.preview.unavailable");
			drawFittedString(
				guiGraphics,
				font,
				placeholder,
				(left + right) / 2,
				(modelTop + modelBottom) / 2 - 4,
				right - left - 8,
				0xFFAAAAAA
			);
		}

		if (config.showLabel) {
			Font font = Minecraft.getInstance().font;
			Component label = singleLabel(config, mob, count);
			if (label != null) {
				drawFittedString(guiGraphics, font, label, (left + right) / 2, bottom - 11, right - left - 6, config.labelColor);
			}
		}
	}

	private static void renderMultiple(
		GuiGraphics guiGraphics,
		NearbyMobConfig config,
		int left,
		int top,
		int right,
		int bottom,
		float baseScale,
		List<MobGroup> groups,
		float partialTick,
		float spinDegrees
	) {
		int contentTop = top + 2;
		int contentBottom = bottom - 2;
		if (contentBottom <= contentTop || right <= left) {
			return;
		}

		int visibleCount = Math.min(groups.size(), MAX_VISIBLE_GROUPS);
		int contentWidth = right - left - 6;
		int contentHeight = contentBottom - contentTop;
		int rowHeight = Math.max(1, contentHeight / visibleCount);

		for (int index = 0; index < visibleCount; index++) {
			MobGroup group = groups.get(index);
			Mob mob = group.representative();
			int rowTop = contentTop + index * rowHeight;
			int rowBottom = Math.min(contentBottom, rowTop + rowHeight);
			int modelWidth = Math.max(1, Math.min(rowBottom - rowTop - 2, contentWidth / 2));
			int modelLeft = left + 3;
			int modelRight = modelLeft + modelWidth;
			int modelTop = rowTop + 1;
			int modelBottom = rowBottom - 1;

			renderModel(
				guiGraphics,
				config,
				mob,
				partialTick,
				spinDegrees,
				baseScale,
				modelLeft,
				modelTop,
				modelRight,
				modelBottom
			);

			if (config.showLabel) {
				Font font = Minecraft.getInstance().font;
				int labelLeft = modelRight + 2;
				int labelRight = right - 3;
				int labelWidth = Math.max(1, labelRight - labelLeft);
				int labelY = rowTop + (rowHeight - font.lineHeight) / 2;
				drawFittedString(
					guiGraphics,
					font,
					groupLabel(config, group),
					(labelLeft + labelRight) / 2,
					labelY,
					labelWidth,
					config.labelColor
				);
			}
		}
	}

	private static void renderModel(
		GuiGraphics guiGraphics,
		NearbyMobConfig config,
		Mob mob,
		float partialTick,
		float spinDegrees,
		float baseScale,
		int x0,
		int y0,
		int x1,
		int y1
	) {
		Minecraft minecraft = Minecraft.getInstance();
		EntityRenderState renderState = minecraft.getEntityRenderDispatcher().extractEntity(mob, partialTick);
		ModelDimensions dimensions = prepareRenderState(renderState, spinDegrees);

		float availableWidth = Math.max(1.0F, x1 - x0 - 2.0F);
		float availableHeight = Math.max(1.0F, y1 - y0 - 2.0F);

		float fit = Math.min(
			1.0F,
			Math.min(
				availableHeight * 0.90F / Math.max(0.01F, dimensions.height() * baseScale),
				availableWidth * 0.90F / Math.max(0.01F, dimensions.width() * baseScale)
			)
		);
		float modelScale = baseScale * fit;

		Vector3f translation = new Vector3f(0.0F, dimensions.height() / 2.0F + 0.0625F, 0.0F);
		Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
		rotation.mul(new Quaternionf().rotateX((float) Math.toRadians(config.hudAngle)));

		guiGraphics.submitEntityRenderState(
			renderState,
			modelScale,
			translation,
			rotation,
			null,
			x0,
			y0,
			x1,
			y1
		);
	}

	private static @Nullable Component singleLabel(NearbyMobConfig config, @Nullable Mob mob, int count) {
		if (mob == null) {
			return Component.translatable("nearbymob.hud.preview");
		}

		Component name = mob.getDisplayName();
		if (count <= 1) {
			if (!config.showDistance) {
				return Component.translatable("nearbymob.hud.near", name);
			}
			return Component.translatable("nearbymob.hud.near_distance", name, distanceTo(mob));
		}

		if (!config.showDistance) {
			return Component.translatable("nearbymob.hud.near_count", name, count);
		}
		return Component.translatable("nearbymob.hud.near_count_distance", name, count, distanceTo(mob));
	}

	private static Component groupLabel(NearbyMobConfig config, MobGroup group) {
		Mob mob = group.representative();
		Component name = mob.getDisplayName();

		if (!config.showDistance) {
			return Component.translatable("nearbymob.hud.name_count", name, group.count());
		}
		return Component.translatable("nearbymob.hud.name_count_distance", name, group.count(), distanceTo(mob));
	}

	private static int distanceTo(Mob mob) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return 0;
		}
		return Math.max(1, Math.round((float) Math.sqrt(mob.distanceToSqr(minecraft.player))));
	}

	private static void drawFittedString(
		GuiGraphics guiGraphics,
		Font font,
		Component label,
		int centerX,
		int y,
		int maxWidth,
		int color
	) {
		if (maxWidth <= 0) {
			return;
		}

		String text = label.getString();
		String fitted = font.plainSubstrByWidth(text, maxWidth);
		if (!fitted.equals(text)) {
			String ellipsis = "…";
			int ellipsisWidth = font.width(ellipsis);
			fitted = font.plainSubstrByWidth(text, Math.max(0, maxWidth - ellipsisWidth)) + ellipsis;
		}

		guiGraphics.drawString(font, fitted, centerX - font.width(fitted) / 2, y, color, true);
	}

	/**
	 * Resets a render state to the neutral setup used by the inventory player
	 * preview: full brightness, no shadow or outline, and a controllable body
	 * rotation. Large entities are normalised to scale 1 so the caller can fit
	 * them into the panel reliably.
	 */
	public static ModelDimensions prepareRenderState(EntityRenderState renderState, float spinDegrees) {
		renderState.lightCoords = LightTexture.FULL_BRIGHT;
		renderState.shadowPieces.clear();
		renderState.outlineColor = 0;
		renderState.nameTag = null;
		renderState.nameTagAttachment = null;
		renderState.leashStates = null;

		float width = Math.max(0.4F, renderState.boundingBoxWidth);
		float height = Math.max(0.4F, renderState.boundingBoxHeight);

		if (renderState instanceof LivingEntityRenderState living) {
			float entityScale = living.scale <= 0.0001F ? 1.0F : living.scale;
			width = Math.max(0.4F, living.boundingBoxWidth / entityScale);
			height = Math.max(0.4F, living.boundingBoxHeight / entityScale);

			living.boundingBoxWidth = width;
			living.boundingBoxHeight = height;
			living.scale = 1.0F;

			// 180 degrees faces the camera; the extra spin is what makes the model
			// rotate in the corner instead of following the mob's real yaw.
			living.bodyRot = 180.0F + spinDegrees;
			living.yRot = 0.0F;
			living.xRot = 0.0F;

			living.isInvisible = false;
			living.isInvisibleToPlayer = false;
			living.hasRedOverlay = false;
			living.isFullyFrozen = false;
			living.deathTime = 0.0F;
			living.isAutoSpinAttack = false;
		}

		return new ModelDimensions(width, height);
	}

	public record ModelDimensions(float width, float height) {
	}
}
