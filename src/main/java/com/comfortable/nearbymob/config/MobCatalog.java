package com.comfortable.nearbymob.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Builds the list of mob types shown in the picker.
 *
 * <p>Most mobs use a non-{@link MobCategory#MISC} spawn category, but a few
 * vanilla mobs (villagers, golems) are registered as MISC. In a loaded world
 * the most reliable test is to ask the entity type to create an instance and
 * check whether it is a {@link Mob}; at the title screen we fall back to the
 * category plus a small allow-list for the vanilla MISC mobs.</p>
 */
public final class MobCatalog {

	private static final Set<EntityType<?>> KNOWN_MISC_MOBS = Set.of(
		EntityType.IRON_GOLEM,
		EntityType.SNOW_GOLEM,
		EntityType.VILLAGER,
		EntityType.COPPER_GOLEM
	);

	private MobCatalog() {
	}

	public static List<EntityType<?>> all(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		List<EntityType<?>> types = new ArrayList<>();

		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			if (type == EntityType.PLAYER) {
				continue;
			}

			if (isMobType(type, level)) {
				types.add(type);
			}
		}

		types.sort(
			Comparator.comparing((EntityType<?> type) -> type.getDescription().getString(), String.CASE_INSENSITIVE_ORDER)
				.thenComparing(MobCatalog::idOf)
		);

		return types;
	}

	private static boolean isMobType(EntityType<?> type, ClientLevel level) {
		if (type.getCategory() != MobCategory.MISC) {
			return true;
		}

		if (KNOWN_MISC_MOBS.contains(type)) {
			return true;
		}

		if (level == null) {
			return false;
		}

		try {
			Entity entity = type.create(level, EntitySpawnReason.COMMAND);
			return entity instanceof Mob;
		} catch (RuntimeException ignored) {
			// A few modded entity constructors assume a fully initialised level.
			return false;
		}
	}

	public static String idOf(EntityType<?> type) {
		Identifier identifier = BuiltInRegistries.ENTITY_TYPE.getKey(type);
		return identifier == null ? "minecraft:unknown" : identifier.toString();
	}

	public static String searchText(EntityType<?> type) {
		return (type.getDescription().getString() + " " + idOf(type)).toLowerCase(java.util.Locale.ROOT);
	}
}
