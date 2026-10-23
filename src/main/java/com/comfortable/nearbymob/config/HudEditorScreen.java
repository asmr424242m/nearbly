package com.comfortable.nearbymob.config;

import com.comfortable.nearbymob.hud.HudRenderer;
import com.comfortable.nearbymob.hud.MobPreview;
import com.comfortable.nearbymob.tracker.MobTracker;
import com.comfortable.nearbymob.tracker.MobTracker.MobGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.function.Consumer;

/**
 * Drag-and-resize editor for the HUD panel.
 *
 * <p>Drag the panel itself to move it, drag the bottom-right handle to resize
 * it, or scroll over it to grow/shrink both dimensions together. Changes are
 * returned to the Cloth entry only when "Done" is pressed, so cancelling the
 * config screen also discards layout changes.</p>
 */
public final class HudEditorScreen extends Screen {

	private static final int HANDLE_SIZE = 12;

	private final Screen parent;
	private final Consumer<HudLayout> onDone;

	private HudLayout layout;
	private List<MobGroup> demoGroups = List.of();

	private boolean dragging;
	private boolean resizing;
	private int dragOffsetX;
	private int dragOffsetY;
	private double resizeStartMouseX;
	private double resizeStartMouseY;
	private int resizeStartWidth;
	private int resizeStartHeight;

	public HudEditorScreen(Screen parent, HudLayout layout, Consumer<HudLayout> onDone) {
		super(Component.translatable("nearbymob.hud_editor.title"));
		this.parent = parent;
		this.layout = layout.sanitized();
		this.onDone = onDone;
	}

	@Override
	protected void init() {
		this.layout = this.layout.fitToScreen(this.width, this.height);
		this.demoGroups = MobTracker.getGroups();
		if (this.demoGroups.isEmpty()) {
			MobPreview preview = MobPreview.get(EntityType.ZOMBIE);
			this.demoGroups = preview == null
				? List.of()
				: List.of(new MobGroup(preview.mob(), 1));
		}

		int buttonWidth = 82;
		int gap = 8;
		int total = buttonWidth * 3 + gap * 2;
		int x = (this.width - total) / 2;
		int y = this.height - 28;

		this.addRenderableWidget(
			Button.builder(Component.translatable("nearbymob.hud_editor.reset"), button -> this.layout = HudLayout.defaults())
				.bounds(x, y, buttonWidth, 20)
				.build()
		);
		x += buttonWidth + gap;

		this.addRenderableWidget(
			Button.builder(CommonComponents.GUI_CANCEL, button -> this.minecraft.setScreen(this.parent))
				.bounds(x, y, buttonWidth, 20)
				.build()
		);
		x += buttonWidth + gap;

		this.addRenderableWidget(
			Button.builder(CommonComponents.GUI_DONE, button -> {
				this.onDone.accept(this.layout);
				this.minecraft.setScreen(this.parent);
			}).bounds(x, y, buttonWidth, 20).build()
		);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		this.layout = this.layout.fitToScreen(this.width, this.height);
		NearbyMobConfig config = NearbyMobConfig.get();
		HudRenderer.render(guiGraphics, config, this.layout, this.demoGroups, 0.0F, 25.0F);

		int left = this.layout.left(this.width);
		int top = this.layout.top(this.height);
		int right = left + this.layout.width();
		int bottom = top + this.layout.height();

		int outlineColor = this.dragging || this.resizing ? 0xFFFFFF66 : 0xFFFFFFFF;
		guiGraphics.renderOutline(left, top, this.layout.width(), this.layout.height(), outlineColor);

		int handleX = right - HANDLE_SIZE;
		int handleY = bottom - HANDLE_SIZE;
		guiGraphics.fill(handleX, handleY, right, bottom, 0xCCFFFFFF);
		guiGraphics.renderOutline(handleX, handleY, HANDLE_SIZE, HANDLE_SIZE, 0xFF000000);

		super.render(guiGraphics, mouseX, mouseY, delta);

		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		guiGraphics.drawCenteredString(
			this.font,
			Component.translatable("nearbymob.hud_editor.hint"),
			this.width / 2,
			26,
			0xFFB0B0B0
		);
		guiGraphics.drawCenteredString(
			this.font,
			Component.translatable(
				"nearbymob.hud_editor.size",
				this.layout.width(),
				this.layout.height(),
				this.layout.xPercent(),
				this.layout.yPercent()
			),
			this.width / 2,
			38,
			0xFF909090
		);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		this.layout = this.layout.fitToScreen(this.width, this.height);
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}

		if (event.button() != 0) {
			return false;
		}

		int left = this.layout.left(this.width);
		int top = this.layout.top(this.height);
		int right = left + this.layout.width();
		int bottom = top + this.layout.height();

		if (event.x() >= right - HANDLE_SIZE && event.x() <= right
			&& event.y() >= bottom - HANDLE_SIZE && event.y() <= bottom) {
			this.resizing = true;
			this.resizeStartMouseX = event.x();
			this.resizeStartMouseY = event.y();
			this.resizeStartWidth = this.layout.width();
			this.resizeStartHeight = this.layout.height();
			return true;
		}

		if (event.x() >= left && event.x() <= right && event.y() >= top && event.y() <= bottom) {
			this.dragging = true;
			this.dragOffsetX = (int) Math.round(event.x()) - left;
			this.dragOffsetY = (int) Math.round(event.y()) - top;
			return true;
		}

		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		this.layout = this.layout.fitToScreen(this.width, this.height);
		if (this.resizing) {
			int left = this.layout.left(this.width);
			int top = this.layout.top(this.height);
			int newWidth = NearbyMobConfig.clamp(
				this.resizeStartWidth + (int) Math.round(event.x() - this.resizeStartMouseX),
				HudLayout.MIN_WIDTH,
				HudLayout.MAX_WIDTH
			);
			int newHeight = NearbyMobConfig.clamp(
				this.resizeStartHeight + (int) Math.round(event.y() - this.resizeStartMouseY),
				HudLayout.MIN_HEIGHT,
				HudLayout.MAX_HEIGHT
			);
			newWidth = Math.min(newWidth, Math.max(HudLayout.MIN_WIDTH, this.width - left));
			newHeight = Math.min(newHeight, Math.max(HudLayout.MIN_HEIGHT, this.height - top));

			int xPercent = HudLayout.percentXFromLeft(left, newWidth, this.width);
			int yPercent = HudLayout.percentYFromTop(top, newHeight, this.height);
			this.layout = this.layout.withSizeAndPosition(newWidth, newHeight, xPercent, yPercent)
				.fitToScreen(this.width, this.height);
			return true;
		}

		if (this.dragging) {
			int newLeft = NearbyMobConfig.clamp(
				(int) Math.round(event.x()) - this.dragOffsetX,
				0,
				Math.max(0, this.width - this.layout.width())
			);
			int newTop = NearbyMobConfig.clamp(
				(int) Math.round(event.y()) - this.dragOffsetY,
				0,
				Math.max(0, this.height - this.layout.height())
			);

			int xPercent = HudLayout.percentXFromLeft(newLeft, this.layout.width(), this.width);
			int yPercent = HudLayout.percentYFromTop(newTop, this.layout.height(), this.height);
			this.layout = this.layout.withPosition(xPercent, yPercent);
			return true;
		}

		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		this.dragging = false;
		this.resizing = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		this.layout = this.layout.fitToScreen(this.width, this.height);
		int left = this.layout.left(this.width);
		int top = this.layout.top(this.height);
		int right = left + this.layout.width();
		int bottom = top + this.layout.height();

		if (mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom) {
			int delta = (int) Math.round(vertical * 10.0);
			if (delta != 0) {
				int maxWidth = Math.max(1, this.width - HudLayout.SCREEN_PADDING * 2);
				int maxHeight = Math.max(1, this.height - HudLayout.SCREEN_PADDING * 2);
				int newWidth = NearbyMobConfig.clamp(
					this.layout.width() + delta,
					Math.min(HudLayout.MIN_WIDTH, maxWidth),
					Math.min(HudLayout.MAX_WIDTH, maxWidth)
				);
				int newHeight = NearbyMobConfig.clamp(
					this.layout.height() + delta,
					Math.min(HudLayout.MIN_HEIGHT, maxHeight),
					Math.min(HudLayout.MAX_HEIGHT, maxHeight)
				);

				int newLeft = NearbyMobConfig.clamp(
					left + (this.layout.width() - newWidth) / 2,
					0,
					Math.max(0, this.width - newWidth)
				);
				int newTop = NearbyMobConfig.clamp(
					top + (this.layout.height() - newHeight) / 2,
					0,
					Math.max(0, this.height - newHeight)
				);

				int xPercent = HudLayout.percentXFromLeft(newLeft, newWidth, this.width);
				int yPercent = HudLayout.percentYFromTop(newTop, newHeight, this.height);
				this.layout = this.layout.withSizeAndPosition(newWidth, newHeight, xPercent, yPercent)
					.fitToScreen(this.width, this.height);
			}
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
