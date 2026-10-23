package com.comfortable.nearbymob.config;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Cloth entry that opens the drag-and-resize HUD editor. Position and size are
 * kept as pending values until the global Save button is pressed.
 */
public final class HudLayoutEntry extends TooltipListEntry<HudLayout> {

	private final Button button;
	private final HudLayout initial;
	private final Consumer<HudLayout> changeListener;
	private HudLayout layout;

	public HudLayoutEntry(
		Component fieldName,
		HudLayout initial,
		HudLayout current,
		Consumer<HudLayout> changeListener,
		Supplier<Optional<Component[]>> tooltipSupplier
	) {
		super(fieldName, tooltipSupplier);
		this.initial = initial.sanitized();
		this.layout = current.sanitized();
		this.changeListener = changeListener;
		this.button = Button.builder(Component.translatable("nearbymob.config.layout.button"), button -> this.openEditor())
			.bounds(0, 0, 150, 20)
			.build();
	}

	private void openEditor() {
		Screen parent = this.getConfigScreen();
		if (parent == null) {
			parent = Minecraft.getInstance().screen;
		}
		if (parent == null) {
			return;
		}

		Screen finalParent = parent;
		Minecraft.getInstance().setScreen(
			new HudEditorScreen(finalParent, this.layout, updated -> {
				this.layout = updated;
				this.changeListener.accept(updated);
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
		this.button.active = this.isEditable();
		this.button.render(guiGraphics, mouseX, mouseY, delta);
	}

	@Override
	public HudLayout getValue() {
		return this.layout;
	}

	@Override
	public Optional<HudLayout> getDefaultValue() {
		return Optional.of(HudLayout.defaults());
	}

	@Override
	public boolean isEdited() {
		return !this.layout.equals(this.initial);
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

	public HudLayoutEntry setSaveConsumer(Consumer<HudLayout> saveConsumer) {
		this.saveCallback = saveConsumer;
		return this;
	}
}
