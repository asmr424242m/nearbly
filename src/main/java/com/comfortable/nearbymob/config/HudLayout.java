package com.comfortable.nearbymob.config;

/**
 * Immutable description of the HUD panel: its size and its position in percent.
 *
 * <p>Percent positions are relative to the free space on screen, so 0/0 is the
 * top-left corner, 100/100 is the bottom-right corner, and 50/50 is centered.
 * A small padding keeps the panel from touching the screen edge.</p>
 */
public record HudLayout(int width, int height, int xPercent, int yPercent) {

	public static final int MIN_WIDTH = 80;
	public static final int MAX_WIDTH = 360;
	public static final int MIN_HEIGHT = 90;
	public static final int MAX_HEIGHT = 420;
	public static final int SCREEN_PADDING = 4;

	public static HudLayout defaults() {
		return new HudLayout(
			NearbyMobConfig.DEFAULT_HUD_WIDTH,
			NearbyMobConfig.DEFAULT_HUD_HEIGHT,
			NearbyMobConfig.DEFAULT_HUD_X,
			NearbyMobConfig.DEFAULT_HUD_Y
		);
	}

	public static HudLayout fromConfig(NearbyMobConfig config) {
		return new HudLayout(config.hudWidth, config.hudHeight, config.hudX, config.hudY).sanitized();
	}

	public HudLayout sanitized() {
		return new HudLayout(
			NearbyMobConfig.clamp(width, MIN_WIDTH, MAX_WIDTH),
			NearbyMobConfig.clamp(height, MIN_HEIGHT, MAX_HEIGHT),
			NearbyMobConfig.clamp(xPercent, 0, 100),
			NearbyMobConfig.clamp(yPercent, 0, 100)
		);
	}

	/**
	 * Keeps a user-supplied layout inside a window of the given size. The
	 * percent position is preserved, so a panel pinned to the bottom-right stays
	 * there even when its size has to be reduced.
	 */
	public HudLayout fitToScreen(int screenWidth, int screenHeight) {
		int maxWidth = Math.max(1, screenWidth - SCREEN_PADDING * 2);
		int maxHeight = Math.max(1, screenHeight - SCREEN_PADDING * 2);
		int fittedWidth = Math.min(width, maxWidth);
		int fittedHeight = Math.min(height, maxHeight);
		if (fittedWidth == width && fittedHeight == height) {
			return this;
		}
		return new HudLayout(fittedWidth, fittedHeight, xPercent, yPercent);
	}

	public HudLayout withSize(int newWidth, int newHeight) {
		return new HudLayout(newWidth, newHeight, xPercent, yPercent).sanitized();
	}

	public HudLayout withPosition(int newX, int newY) {
		return new HudLayout(width, height, newX, newY).sanitized();
	}

	public HudLayout withSizeAndPosition(int newWidth, int newHeight, int newX, int newY) {
		return new HudLayout(newWidth, newHeight, newX, newY).sanitized();
	}

	public int left(int screenWidth) {
		int maxLeft = Math.max(0, screenWidth - width);
		int padding = Math.min(SCREEN_PADDING, maxLeft / 2);
		int available = Math.max(0, maxLeft - padding * 2);
		return NearbyMobConfig.clamp(padding + Math.round(available * xPercent / 100.0F), 0, maxLeft);
	}

	public int top(int screenHeight) {
		int maxTop = Math.max(0, screenHeight - height);
		int padding = Math.min(SCREEN_PADDING, maxTop / 2);
		int available = Math.max(0, maxTop - padding * 2);
		return NearbyMobConfig.clamp(padding + Math.round(available * yPercent / 100.0F), 0, maxTop);
	}

	/**
	 * Converts an absolute panel position back into percentages. This is used
	 * while dragging the HUD editor so the panel follows the mouse without
	 * jumping when it gets close to an edge.
	 */
	public static int percentXFromLeft(int left, int panelWidth, int screenWidth) {
		int maxLeft = Math.max(0, screenWidth - panelWidth);
		int padding = Math.min(SCREEN_PADDING, maxLeft / 2);
		int available = Math.max(0, maxLeft - padding * 2);
		if (available <= 0) {
			return 0;
		}
		return NearbyMobConfig.clamp(Math.round((left - padding) * 100.0F / available), 0, 100);
	}

	public static int percentYFromTop(int top, int panelHeight, int screenHeight) {
		int maxTop = Math.max(0, screenHeight - panelHeight);
		int padding = Math.min(SCREEN_PADDING, maxTop / 2);
		int available = Math.max(0, maxTop - padding * 2);
		if (available <= 0) {
			return 0;
		}
		return NearbyMobConfig.clamp(Math.round((top - padding) * 100.0F / available), 0, 100);
	}
}
