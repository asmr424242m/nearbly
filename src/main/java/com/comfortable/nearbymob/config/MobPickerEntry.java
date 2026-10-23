package com.comfortable.nearbymob.config;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Cloth entry that looks like a normal setting but opens the full mob picker.
 * The pending list is only written back to the config when Cloth's global Save
 * button is pressed, so Cancel still discards the selection.
 */
public final class MobPickerEntry extends TooltipListEntry<List<String>> {

	private final Button button;
	private final List<String> initial;
	private final Consumer<List<String>> changeListener;
	private List<String> selected;

	public MobPickerEntry(
		Component fieldName,
		List<String> initial,
		List<String> current,
		Consumer<List<String>> changeListener,
		Supplier<Optional<Component[]>> tooltipSupplier
	) {
		super(fieldName, tooltipSupplier);
		this.initial = new ArrayList<>(initial);
		this.selected = new ArrayList<>(current);
		this.changeListener = changeListener;
		this.button = Button.builder(buttonLabel(), button -> this.openPicker())
			.bounds(0, 0, 150, 20)
			.build();
	}

	private Component buttonLabel() {
		return this.selected.isEmpty()
			? Component.translatable("nearbymob.config.tracked_mobs.button.none")
			: Component.translatable("nearbymob.config.tracked_mobs.button", this.selected.size());
	}

	private void openPicker() {
		Screen parent = this.getConfigScreen();
		if (parent == null) {
			parent = Minecraft.getInstance().screen;
		}
		if (parent == null) {
			return;
		}

		Screen finalParent = parent;
		Minecraft.getInstance().setScreen(
			new MobSelectionScreen(finalParent, this.selected, updated -> {
				this.selected = new ArrayList<>(updated);
				this.changeListener.accept(List.copyOf(this.selected));
				this.button.setMessage(this.buttonLabel());
			})
		);
	}

	@Override
	public void render(
		GuiGraphics guiGraphics,
		int index,
		int y,
		int x,
		int entryWidth,
		int entryHeight,
		int mouseX,
		int mouseY,
		boolean isHovered,
		float delta
	) {
		super.render(guiGraphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);

		guiGraphics.drawString(
			Minecraft.getInstance().font,
			this.getDisplayedFieldName(),
			x,
			y + 6,
			this.getPreferredTextColor(),
			false
		);

		this.button.setX(x + entryWidth - 150);
		this.button.setY(y);
		this.button.setWidth(150);
		this.button.setMessage(this.buttonLabel());
		this.button.active = this.isEditable();
		this.button.render(guiGraphics, mouseX, mouseY, delta);
	}

	@Override
	public List<String> getValue() {
		return new ArrayList<>(this.selected);
	}

	@Override
	public Optional<List<String>> getDefaultValue() {
		return Optional.of(NearbyMobConfig.defaultTrackedMobs());
	}

	@Override
	public boolean isEdited() {
		return this.selected.size() != this.initial.size()
			|| !this.selected.containsAll(this.initial);
	}

	@Override
	public int getItemHeight() {
		return 24;
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return List.of(this.button);
	}

	@Override
	public List<? extends NarratableEntry> narratables() {
		return List.of(this.button);
	}

	public MobPickerEntry setSaveConsumer(Consumer<List<String>> saveConsumer) {
		this.saveCallback = saveConsumer;
		return this;
	}
}
