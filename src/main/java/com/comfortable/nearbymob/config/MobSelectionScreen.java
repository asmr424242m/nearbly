package com.comfortable.nearbymob.config;

import com.comfortable.nearbymob.hud.MobPreview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Searchable, image-backed mob picker.
 *
 * <p>The screen works both in a world and from the title screen. In a world it
 * renders real 3D mob previews; at the title screen it falls back to spawn egg
 * icons (or a coloured letter tile for mobs without one).</p>
 */
public final class MobSelectionScreen extends Screen {

	private static final int ROW_HEIGHT = 30;
	private static final int BOTTOM_BAR_HEIGHT = 44;

	private final Screen parent;
	private final Consumer<List<String>> onDone;
	private final Set<String> selected = new LinkedHashSet<>();

	private List<EntityType<?>> allTypes = List.of();
	private MobList list;
	private EditBox search;
	private int visibleCount;

	public MobSelectionScreen(Screen parent, Collection<String> initialSelected, Consumer<List<String>> onDone) {
		super(Component.translatable("nearbymob.mob_picker.title"));
		this.parent = parent;
		this.onDone = onDone;

		for (String id : initialSelected) {
			if (id != null && !id.isBlank()) {
				this.selected.add(id.trim().toLowerCase(Locale.ROOT));
			}
		}
	}

	@Override
	protected void init() {
		this.allTypes = MobCatalog.all(this.minecraft);

		int searchWidth = Math.min(360, Math.max(120, this.width - 40));
		this.search = new EditBox(
			this.font,
			(this.width - searchWidth) / 2,
			28,
			searchWidth,
			18,
			Component.translatable("nearbymob.mob_picker.search")
		);
		this.search.setHint(Component.translatable("nearbymob.mob_picker.search.hint"));
		this.search.setMaxLength(64);
		this.search.setResponder(value -> this.refreshList());
		this.addRenderableWidget(this.search);

		int listTop = 52;
		int listHeight = Math.max(40, this.height - listTop - BOTTOM_BAR_HEIGHT);
		this.list = new MobList(this.minecraft, this.width, listHeight, listTop, ROW_HEIGHT);
		this.list.updateSizeAndPosition(this.width, listHeight, 0, listTop);
		this.addRenderableWidget(this.list);

		int buttonY = this.height - 28;
		int doneWidth = 90;
		int cancelWidth = 90;
		int selectWidth = 112;
		int clearWidth = 80;
		int gap = 8;
		int total = doneWidth + cancelWidth + selectWidth + clearWidth + gap * 3;
		int x = (this.width - total) / 2;

		this.addRenderableWidget(
			Button.builder(CommonComponents.GUI_DONE, button -> this.finish())
				.bounds(x, buttonY, doneWidth, 20)
				.build()
		);
		x += doneWidth + gap;

		this.addRenderableWidget(
			Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
				.bounds(x, buttonY, cancelWidth, 20)
				.build()
		);
		x += cancelWidth + gap;

		this.addRenderableWidget(
			Button.builder(Component.translatable("nearbymob.mob_picker.select_all"), button -> this.selectVisible())
				.bounds(x, buttonY, selectWidth, 20)
				.build()
		);
		x += selectWidth + gap;

		this.addRenderableWidget(
			Button.builder(Component.translatable("nearbymob.mob_picker.clear"), button -> this.selected.clear())
				.bounds(x, buttonY, clearWidth, 20)
				.build()
		);

		this.refreshList();
		this.setInitialFocus(this.search);
	}

	private void refreshList() {
		if (this.list == null) {
			return;
		}

		String query = this.search == null ? "" : this.search.getValue().trim().toLowerCase(Locale.ROOT);
		List<MobList.Entry> entries = new ArrayList<>();

		for (EntityType<?> type : this.allTypes) {
			if (query.isEmpty() || MobCatalog.searchText(type).contains(query)) {
				entries.add(new MobList.Entry(this, type));
			}
		}

		this.visibleCount = entries.size();
		this.list.replaceEntries(entries);
		this.list.setScrollAmount(0);
	}

	private void selectVisible() {
		if (this.list == null) {
			return;
		}

		for (MobList.Entry entry : this.list.children()) {
			this.selected.add(MobCatalog.idOf(entry.type));
		}
	}

	private void finish() {
		this.onDone.accept(new ArrayList<>(this.selected));
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		super.render(guiGraphics, mouseX, mouseY, delta);

		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);

		Component count = Component.translatable("nearbymob.mob_picker.selected", this.selected.size());
		guiGraphics.drawString(this.font, count, 16, this.height - 40, 0xFFB0B0B0, false);

		if (this.visibleCount == 0) {
			guiGraphics.drawCenteredString(
				this.font,
				Component.translatable("nearbymob.mob_picker.empty"),
				this.width / 2,
				(this.list.getY() + this.list.getBottom()) / 2,
				0xFFAAAAAA
			);
		}
	}

	private static final class MobList extends ObjectSelectionList<MobList.Entry> {

		private MobList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
			super(minecraft, width, height, y, itemHeight);
		}

		@Override
		public int getRowWidth() {
			return Math.min(520, Math.max(220, this.width - 60));
		}

		@Override
		protected boolean entriesCanBeSelected() {
			return false;
		}

		private static final class Entry extends ObjectSelectionList.Entry<Entry> {

			private final MobSelectionScreen screen;
			private final EntityType<?> type;
			private final String id;
			private final Component name;

			private Entry(MobSelectionScreen screen, EntityType<?> type) {
				this.screen = screen;
				this.type = type;
				this.id = MobCatalog.idOf(type);
				this.name = type.getDescription();
			}

			@Override
			public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float delta) {
				int x = this.getContentX();
				int y = this.getContentY();

				if (hovered) {
					guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x22FFFFFF);
				}

				boolean isSelected = this.screen.selected.contains(this.id);
				int iconX = x + 2;
				int iconY = y + 1;
				int iconSize = 24;

				guiGraphics.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, 0x33000000);

				MobPreview preview = MobPreview.get(this.type);
				if (preview != null) {
					preview.render(guiGraphics, iconX, iconY, iconSize);
				} else {
					this.renderFallbackIcon(guiGraphics, iconX, iconY, iconSize);
				}

				Font font = Minecraft.getInstance().font;
				int textX = iconX + iconSize + 6;
				int boxX = this.getX() + this.getWidth() - 18;
				int maxTextWidth = Math.max(20, boxX - textX - 6);

				String displayName = font.plainSubstrByWidth(this.name.getString(), maxTextWidth);
				String displayId = font.plainSubstrByWidth(this.id, maxTextWidth);
				int nameColor = isSelected ? 0xFFFFFFFF : 0xFFC8C8C8;

				guiGraphics.drawString(font, displayName, textX, this.getY() + 4, nameColor, false);
				guiGraphics.drawString(font, displayId, textX, this.getY() + 16, 0xFF7E7E7E, false);

				int boxY = this.getY() + (this.getHeight() - 10) / 2;
				if (isSelected) {
					guiGraphics.fill(boxX, boxY, boxX + 10, boxY + 10, 0xFF2E7D32);
					guiGraphics.fill(boxX + 2, boxY + 2, boxX + 8, boxY + 8, 0xFFB9FFB9);
				} else {
					guiGraphics.fill(boxX, boxY, boxX + 10, boxY + 10, 0x66000000);
				}
				guiGraphics.renderOutline(boxX, boxY, 10, 10, isSelected ? 0xFF7CFF8A : 0xFF9A9A9A);
			}

			private void renderFallbackIcon(GuiGraphics guiGraphics, int x, int y, int size) {
				SpawnEggItem egg = SpawnEggItem.byId(this.type);
				if (egg != null) {
					guiGraphics.renderFakeItem(new ItemStack(egg), x + (size - 16) / 2, y + (size - 16) / 2);
					return;
				}

				int color = 0xFF000000 | (this.id.hashCode() & 0x00FFFFFF);
				guiGraphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, color);
				guiGraphics.renderOutline(x + 2, y + 2, size - 4, size - 4, 0x99FFFFFF);

				String initial = this.name.getString().isBlank() ? "?" : this.name.getString().substring(0, 1).toUpperCase(Locale.ROOT);
				Font font = Minecraft.getInstance().font;
				guiGraphics.drawString(
					font,
					initial,
					x + (size - font.width(initial)) / 2,
					y + (size - 8) / 2,
					0xFFFFFFFF,
					true
				);
			}

			@Override
			public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
				if (event.button() == 0) {
					if (!this.screen.selected.remove(this.id)) {
						this.screen.selected.add(this.id);
					}
					return true;
				}

				return super.mouseClicked(event, doubleClick);
			}

			@Override
			public Component getNarration() {
				return Component.translatable(
					"nearbymob.mob_picker.narration",
					this.name,
					this.screen.selected.contains(this.id)
						? Component.translatable("nearbymob.mob_picker.narration.selected")
						: Component.translatable("nearbymob.mob_picker.narration.not_selected")
				);
			}
		}
	}
}
