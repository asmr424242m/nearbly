package com.comfortable.nearbymob.config;

import com.comfortable.nearbymob.NearbyMobClient;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Builds the Cloth Config screen used both by Mod Menu and (if desired) directly
 * from code. Values are only written to the config object when the user saves.
 */
public final class NearbyMobConfigScreen {

	private NearbyMobConfigScreen() {
	}

	public static Screen create(Screen parent) {
		NearbyMobConfig config = NearbyMobConfig.get();
		Draft draft = new Draft(config);

		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(parent)
			.setTitle(Component.translatable("nearbymob.config.title"))
			.setSavingRunnable(config::save);

		ConfigEntryBuilder entries = builder.entryBuilder();
		ConfigCategory general = builder.getOrCreateCategory(Component.translatable("nearbymob.config.category.general"));
		ConfigCategory appearance = builder.getOrCreateCategory(Component.translatable("nearbymob.config.category.appearance"));

		general.addEntry(entries.startIntSlider(
				Component.translatable("nearbymob.config.radius"),
				config.radius,
				4,
				128
			)
			.setDefaultValue(NearbyMobConfig.DEFAULT_RADIUS)
			.setTextGetter(value -> Component.translatable("nearbymob.config.radius.value", value))
			.setTooltip(Component.translatable("nearbymob.config.radius.tooltip"))
			.setSaveConsumer(value -> config.radius = value)
			.build());

		general.addEntry(new MobPickerEntry(
				Component.translatable("nearbymob.config.tracked_mobs"),
				new ArrayList<>(config.trackedMobs),
				draft.trackedMobs,
				value -> draft.trackedMobs = new ArrayList<>(value),
				tooltip(
					Component.translatable("nearbymob.config.tracked_mobs.tooltip.1"),
					Component.translatable("nearbymob.config.tracked_mobs.tooltip.2")
				)
			)
			.setSaveConsumer(value -> config.trackedMobs = new ArrayList<>(value)));

		general.addEntry(entries.startBooleanToggle(
				Component.translatable("nearbymob.config.hud_enabled"),
				config.hudEnabled
			)
			.setDefaultValue(NearbyMobConfig.DEFAULT_HUD_ENABLED)
			.setTooltip(Component.translatable("nearbymob.config.hud_enabled.tooltip"))
			.setSaveConsumer(value -> config.hudEnabled = value)
			.build());

		general.addEntry(entries.startTextDescription(
				Component.translatable("nearbymob.config.hint", NearbyMobClient.MOD_ID)
			).build());

		appearance.addEntry(new HudLayoutEntry(
				Component.translatable("nearbymob.config.layout"),
				HudLayout.fromConfig(config),
				draft.layout,
				value -> draft.layout = value,
				tooltip(
					Component.translatable("nearbymob.config.layout.tooltip.1"),
					Component.translatable("nearbymob.config.layout.tooltip.2"),
					Component.translatable("nearbymob.config.layout.tooltip.3")
				)
			)
			.setSaveConsumer(layout -> {
				config.hudWidth = layout.width();
				config.hudHeight = layout.height();
				config.hudX = layout.xPercent();
				config.hudY = layout.yPercent();
			}));

		appearance.addEntry(entries.startIntSlider(
				Component.translatable("nearbymob.config.scale"),
				config.hudScale,
				10,
				200
			)
			.setDefaultValue(NearbyMobConfig.DEFAULT_SCALE)
			.setTextGetter(value -> Component.translatable("nearbymob.config.scale.value", value))
			.setTooltip(Component.translatable("nearbymob.config.scale.tooltip"))
			.setSaveConsumer(value -> config.hudScale = value)
			.build());

		appearance.addEntry(entries.startIntSlider(
				Component.translatable("nearbymob.config.hud_angle"),
				config.hudAngle,
				-60,
				60
			)
			.setDefaultValue(NearbyMobConfig.DEFAULT_HUD_ANGLE)
			.setTextGetter(value -> Component.translatable("nearbymob.config.hud_angle.value", value))
			.setTooltip(Component.translatable("nearbymob.config.hud_angle.tooltip"))
			.setSaveConsumer(value -> config.hudAngle = value)
			.build());

		appearance.addEntry(entries.startIntSlider(
				Component.translatable("nearbymob.config.rotation_speed"),
				config.rotationSpeed,
				0,
				360
			)
			.setDefaultValue(NearbyMobConfig.DEFAULT_ROTATION_SPEED)
			.setTextGetter(value -> Component.translatable("nearbymob.config.rotation_speed.value", value))
			.setTooltip(Component.translatable("nearbymob.config.rotation_speed.tooltip"))
			.setSaveConsumer(value -> config.rotationSpeed = value)
			.build());

		appearance.addEntry(entries.startIntSlider(
				Component.translatable("nearbymob.config.background_opacity"),
				config.backgroundOpacity,
				0,
				100
			)
			.setDefaultValue(NearbyMobConfig.DEFAULT_BACKGROUND_OPACITY)
			.setTextGetter(value -> Component.translatable("nearbymob.config.background_opacity.value", value))
			.setTooltip(Component.translatable("nearbymob.config.background_opacity.tooltip"))
			.setSaveConsumer(value -> config.backgroundOpacity = value)
			.build());

		appearance.addEntry(entries.startBooleanToggle(
				Component.translatable("nearbymob.config.show_label"),
				config.showLabel
			)
			.setDefaultValue(NearbyMobConfig.DEFAULT_SHOW_LABEL)
			.setTooltip(Component.translatable("nearbymob.config.show_label.tooltip"))
			.setSaveConsumer(value -> config.showLabel = value)
			.build());

		appearance.addEntry(entries.startBooleanToggle(
				Component.translatable("nearbymob.config.show_distance"),
				config.showDistance
			)
			.setDefaultValue(NearbyMobConfig.DEFAULT_SHOW_DISTANCE)
			.setTooltip(Component.translatable("nearbymob.config.show_distance.tooltip"))
			.setSaveConsumer(value -> config.showDistance = value)
			.build());

		appearance.addEntry(entries.startBooleanToggle(
				Component.translatable("nearbymob.config.show_border"),
				config.showBorder
			)
			.setDefaultValue(NearbyMobConfig.DEFAULT_SHOW_BORDER)
			.setTooltip(Component.translatable("nearbymob.config.show_border.tooltip"))
			.setSaveConsumer(value -> config.showBorder = value)
			.build());

		appearance.addEntry(entries.startAlphaColorField(
				Component.translatable("nearbymob.config.label_color"),
				config.labelColor
			)
			.setDefaultValue(NearbyMobConfig.DEFAULT_LABEL_COLOR)
			.setTooltip(Component.translatable("nearbymob.config.label_color.tooltip"))
			.setSaveConsumer(value -> config.labelColor = value)
			.build());

		appearance.addEntry(entries.startTextDescription(
				Component.translatable("nearbymob.config.appearance.hint")
			).build());

		return builder.build();
	}

	private static Supplier<Optional<Component[]>> tooltip(Component... lines) {
		return () -> Optional.of(lines);
	}

	/**
	 * Pending values for the custom entries. Cloth may rebuild its entries when
	 * tabs are switched, so the entries cannot own the only copy of a selection.
	 * The draft lives for as long as the config screen and is only copied into
	 * the real config when Cloth's Save button is pressed.
	 */
	private static final class Draft {
		private List<String> trackedMobs;
		private HudLayout layout;

		private Draft(NearbyMobConfig config) {
			this.trackedMobs = new ArrayList<>(config.trackedMobs);
			this.layout = HudLayout.fromConfig(config);
		}
	}
}
