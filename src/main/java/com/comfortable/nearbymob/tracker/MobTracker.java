package com.comfortable.nearbymob.tracker;

import com.comfortable.nearbymob.config.NearbyMobConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scans the world every {@value #SCAN_INTERVAL} client ticks for tracked mobs
 * around the player.
 *
 * <p>The list is sorted by distance, so the first element is always the nearest
 * mob. The HUD uses the whole list to show every nearby mob and the total count.
 * References are revalidated on access and dropped as soon as the world or a
 * mob disappears.</p>
 */
public final class MobTracker {

	public static final int SCAN_INTERVAL = 10;

	private static int tickCounter;
	private static @Nullable ClientLevel trackedLevel;
	private static List<Mob> targets = List.of();

	private MobTracker() {
	}

	public static void tick(Minecraft minecraft) {
		if (++tickCounter < SCAN_INTERVAL) {
			return;
		}

		tickCounter = 0;
		scan(minecraft);
	}

	private static void scan(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		Player player = minecraft.player;

		if (level == null || player == null || level != trackedLevel) {
			clear();
			trackedLevel = level;
		}

		if (level == null || player == null) {
			return;
		}

		NearbyMobConfig config = NearbyMobConfig.get();
		Set<EntityType<?>> trackedTypes = config.resolveTrackedTypes();

		if (trackedTypes.isEmpty()) {
			targets = List.of();
			return;
		}

		double radius = config.radius;
		double radiusSq = radius * radius;
		AABB searchArea = player.getBoundingBox().inflate(radius);

		List<Mob> candidates = level.getEntitiesOfClass(Mob.class, searchArea,
			mob -> mob.isAlive()
				&& !mob.isRemoved()
				&& !mob.isSpectator()
				&& trackedTypes.contains(mob.getType())
				&& mob.distanceToSqr(player) <= radiusSq
		);

		candidates.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)));
		targets = List.copyOf(candidates);
	}

	/**
	 * @return all currently valid tracked mobs within the configured radius,
	 *         sorted from nearest to farthest.
	 */
	public static List<Mob> getTargets() {
		List<Mob> current = targets;
		if (current.isEmpty()) {
			return List.of();
		}

		ArrayList<Mob> valid = new ArrayList<>(current.size());
		for (Mob mob : current) {
			if (!mob.isRemoved() && mob.isAlive()) {
				valid.add(mob);
			}
		}

		if (valid.size() != current.size()) {
			targets = List.copyOf(valid);
		}

		return targets;
	}

	public static @Nullable Mob getTarget() {
		List<Mob> current = getTargets();
		return current.isEmpty() ? null : current.get(0);
	}

	/**
	 * Groups the nearby mobs by entity type. The first mob of each group is the
	 * nearest one, and the count is the total number of that type in range.
	 */
	public static List<MobGroup> getGroups() {
		List<Mob> current = getTargets();
		if (current.isEmpty()) {
			return List.of();
		}

		Map<EntityType<?>, Mob> representatives = new LinkedHashMap<>();
		Map<EntityType<?>, Integer> counts = new LinkedHashMap<>();

		for (Mob mob : current) {
			EntityType<?> type = mob.getType();
			representatives.putIfAbsent(type, mob);
			counts.merge(type, 1, Integer::sum);
		}

		List<MobGroup> groups = new ArrayList<>(representatives.size());
		for (Map.Entry<EntityType<?>, Mob> entry : representatives.entrySet()) {
			groups.add(new MobGroup(entry.getValue(), counts.get(entry.getKey())));
		}
		return List.copyOf(groups);
	}

	public static void clear() {
		targets = List.of();
	}

	public record MobGroup(Mob representative, int count) {
	}
}
