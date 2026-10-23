package com.comfortable.nearbymob.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Cached, display-only mob instances used for the little 3D icons in the mob
 * picker. The entity is never added to the level; it exists only so the vanilla
 * entity renderer can extract a render state from it.
 */
public final class MobPreview {

	private static final Map<EntityType<?>, MobPreview> CACHE = new HashMap<>();
	private static @Nullable ClientLevel cachedLevel;

	private final Mob mob;
	private final EntityRenderState state;
	private final float width;
	private final float height;

	private MobPreview(Mob mob, EntityRenderState state, float width, float height) {
		this.mob = mob;
		this.state = state;
		this.width = width;
		this.height = height;
	}

	public static @Nullable MobPreview get(EntityType<?> type) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;

		if (level == null) {
			return null;
		}

		if (level != cachedLevel) {
			CACHE.clear();
			cachedLevel = level;
		}

		MobPreview cached = CACHE.get(type);
		if (cached != null) {
			return cached;
		}

		try {
			Entity entity = type.create(level, EntitySpawnReason.COMMAND);
			if (!(entity instanceof Mob mob)) {
				return null;
			}

			mob.setPos(0.0, 0.0, 0.0);
			mob.setYRot(0.0F);
			mob.setXRot(0.0F);
			mob.yBodyRot = 0.0F;
			mob.yBodyRotO = 0.0F;
			mob.yHeadRot = 0.0F;
			mob.yHeadRotO = 0.0F;
			mob.setOldPosAndRot();
			mob.tickCount = 20;
			mob.hurtTime = 0;
			mob.deathTime = 0;
			mob.setInvisible(false);

			EntityRenderState state = minecraft.getEntityRenderDispatcher().extractEntity(mob, 0.0F);
			prepare(state, 210.0F);

			float width = Math.max(0.4F, state.boundingBoxWidth);
			float height = Math.max(0.4F, state.boundingBoxHeight);

			if (state instanceof LivingEntityRenderState living) {
				float entityScale = living.scale <= 0.0001F ? 1.0F : living.scale;
				width = Math.max(0.4F, living.boundingBoxWidth / entityScale);
				height = Math.max(0.4F, living.boundingBoxHeight / entityScale);
			}

			MobPreview preview = new MobPreview(mob, state, width, height);
			CACHE.put(type, preview);
			return preview;
		} catch (RuntimeException ignored) {
			// A modded mob renderer or constructor can be picky. A missing icon is
			// not worth crashing the settings screen over.
			return null;
		}
	}

	private static void prepare(EntityRenderState state, float bodyRot) {
		state.lightCoords = LightTexture.FULL_BRIGHT;
		state.shadowPieces.clear();
		state.outlineColor = 0;
		state.nameTag = null;
		state.nameTagAttachment = null;
		state.leashStates = null;

		if (state instanceof LivingEntityRenderState living) {
			float entityScale = living.scale <= 0.0001F ? 1.0F : living.scale;
			float width = Math.max(0.4F, living.boundingBoxWidth / entityScale);
			float height = Math.max(0.4F, living.boundingBoxHeight / entityScale);

			living.boundingBoxWidth = width;
			living.boundingBoxHeight = height;
			living.scale = 1.0F;

			living.bodyRot = bodyRot;
			living.yRot = 0.0F;
			living.xRot = 0.0F;

			living.isInvisible = false;
			living.isInvisibleToPlayer = false;
			living.hasRedOverlay = false;
			living.isFullyFrozen = false;
			living.deathTime = 0.0F;
			living.isAutoSpinAttack = false;
		}
	}

	public void render(GuiGraphics guiGraphics, int x, int y, int size) {
		float largestSide = Math.max(0.5F, Math.max(width, height));
		float scale = (size * 0.82F) / largestSide;

		Vector3f translation = new Vector3f(0.0F, height / 2.0F + 0.0625F, 0.0F);
		Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
		rotation.mul(new Quaternionf().rotateX((float) Math.toRadians(-12.0F)));

		guiGraphics.submitEntityRenderState(
			state,
			scale,
			translation,
			rotation,
			null,
			x,
			y,
			x + size,
			y + size
		);
	}

	public Mob mob() {
		return mob;
	}

	public EntityRenderState state() {
		return state;
	}

	public float width() {
		return width;
	}

	public float height() {
		return height;
	}
}
