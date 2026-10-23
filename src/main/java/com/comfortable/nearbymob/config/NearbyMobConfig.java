package com.comfortable.nearbymob.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Plain JSON-backed settings for Nearby Mob HUD.
 *
 * <p>The class is intentionally simple: Cloth Config edits these public fields and
 * calls {@link #save()} when the user presses "Save". Loading is lazy, so the HUD
 * can ask for the instance from the render thread without any extra setup.</p>
 */
public final class NearbyMobConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger("nearbymob/config");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("nearby-mob.json");

	public static final int DEFAULT_RADIUS = 32;
	public static final int DEFAULT_HUD_ANGLE = -15;
	public static final int DEFAULT_SCALE = 48;
	public static final int DEFAULT_ROTATION_SPEED = 30;
	public static final int DEFAULT_HUD_WIDTH = 120;
	public static final int DEFAULT_HUD_HEIGHT = 150;
	public static final int DEFAULT_HUD_X = 100;
	public static final int DEFAULT_HUD_Y = 100;
	public static final int DEFAULT_BACKGROUND_OPACITY = 35;
	public static final int DEFAULT_LABEL_COLOR = 0xFFFFFFFF;
	public static final boolean DEFAULT_SHOW_LABEL = true;
	public static final boolean DEFAULT_SHOW_DISTANCE = false;
	public static final boolean DEFAULT_SHOW_BORDER = true;
	public static final boolean DEFAULT_HUD_ENABLED = true;

	public static final List<String> DEFAULT_TRACKED_MOBS = List.of(
		"minecraft:zombie",
		"minecraft:skeleton",
		"minecraft:creeper",
		"minecraft:spider",
		"minecraft:cow",
		"minecraft:pig",
		"minecraft:sheep"
	);

	private static NearbyMobConfig instance;

	/** Radius in blocks around the player that is scanned for tracked mobs. */
	public int radius = DEFAULT_RADIUS;

	/** Entity type ids ("minecraft:zombie", ...) that the HUD should watch. */
	public List<String> trackedMobs = defaultTrackedMobs();

	/** Vertical tilt of the 3D model, in degrees. */
	public int hudAngle = DEFAULT_HUD_ANGLE;

	/** Base size of the 3D model in GUI pixels. Scales with the panel size. */
	public int hudScale = DEFAULT_SCALE;

	/** How fast the model spins, in degrees per second. */
	public int rotationSpeed = DEFAULT_ROTATION_SPEED;

	/** Master switch for the HUD. */
	public boolean hudEnabled = DEFAULT_HUD_ENABLED;

	/** Width of the HUD panel in GUI pixels. */
	public int hudWidth = DEFAULT_HUD_WIDTH;

	/** Height of the HUD panel in GUI pixels, including the caption. */
	public int hudHeight = DEFAULT_HUD_HEIGHT;

	/** Horizontal panel position, 0 = left edge, 100 = right edge. */
	public int hudX = DEFAULT_HUD_X;

	/** Vertical panel position, 0 = top edge, 100 = bottom edge. */
	public int hudY = DEFAULT_HUD_Y;

	/** Black panel alpha, 0 = fully transparent, 100 = fully opaque. */
	public int backgroundOpacity = DEFAULT_BACKGROUND_OPACITY;

	/** Whether the "Near: ..." caption is drawn. */
	public boolean showLabel = DEFAULT_SHOW_LABEL;

	/** Whether the distance to the mob is appended to the caption. */
	public boolean showDistance = DEFAULT_SHOW_DISTANCE;

	/** Whether a subtle outline is drawn around the panel. */
	public boolean showBorder = DEFAULT_SHOW_BORDER;

	/** ARGB colour of the caption. */
	public int labelColor = DEFAULT_LABEL_COLOR;

	public static NearbyMobConfig get() {
		if (instance == null) {
			load();
		}
		return instance;
	}

	public static void load() {
		NearbyMobConfig config = new NearbyMobConfig();

		if (Files.isRegularFile(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
				NearbyMobConfig loaded = GSON.fromJson(reader, NearbyMobConfig.class);
				if (loaded != null) {
					config = loaded;
				}
			} catch (IOException | JsonParseException e) {
				LOGGER.warn("Could not read {}; using defaults", PATH, e);
			}
		}

		config.sanitize();
		instance = config;
	}

	public void save() {
		sanitize();

		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			LOGGER.error("Could not save {}", PATH, e);
		}
	}

	/**
	 * @return the configured entity types, resolved through the vanilla registry.
	 *         Entries that are blank or unknown are silently ignored.
	 */
	public Set<EntityType<?>> resolveTrackedTypes() {
		Set<EntityType<?>> types = new LinkedHashSet<>();

		for (String raw : trackedMobs) {
			if (raw == null || raw.isBlank()) {
				continue;
			}

			String id = raw.trim().toLowerCase(Locale.ROOT);
			Identifier identifier = Identifier.tryParse(id);
			if (identifier == null) {
				continue;
			}

			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(identifier).orElse(null);
			if (type != null) {
				types.add(type);
			}
		}

		return types;
	}

	private void sanitize() {
		radius = clamp(radius, 4, 128);
		hudAngle = clamp(hudAngle, -60, 60);
		hudScale = clamp(hudScale, 10, 200);
		rotationSpeed = clamp(rotationSpeed, 0, 360);
		hudWidth = clamp(hudWidth, 80, 360);
		hudHeight = clamp(hudHeight, 90, 420);
		hudX = clamp(hudX, 0, 100);
		hudY = clamp(hudY, 0, 100);
		backgroundOpacity = clamp(backgroundOpacity, 0, 100);

		List<String> cleaned = new ArrayList<>();
		if (trackedMobs != null) {
			for (String raw : trackedMobs) {
				if (raw != null && !raw.isBlank()) {
					cleaned.add(raw.trim().toLowerCase(Locale.ROOT));
				}
			}
		}
		trackedMobs = cleaned;
	}

	public static List<String> defaultTrackedMobs() {
		return new ArrayList<>(DEFAULT_TRACKED_MOBS);
	}

	public static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
