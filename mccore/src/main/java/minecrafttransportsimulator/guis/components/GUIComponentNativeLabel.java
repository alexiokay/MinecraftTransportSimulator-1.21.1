package minecrafttransportsimulator.guis.components;

import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Label component that uses Minecraft's native font rendering.
 * This provides the standard Minecraft font appearance that players expect,
 * unlike {@link GUIComponentLabel} which uses MTS's custom bitmap font.
 *
 * Essential for HUD overlays like weapon HUD where text must match
 * the vanilla Minecraft style (like Superb Warfare's ammo display).
 *
 * @author alexispace
 */
public class GUIComponentNativeLabel extends AGUIComponent {
    public int color;
    public final float scale;
    public final boolean shadow;
    public final boolean rightAligned;

    // Float position for sub-pixel precision
    public float posX;
    public float posY;

    /**
     * Creates a native label with Minecraft font rendering.
     *
     * @param x Screen X position
     * @param y Screen Y position
     * @param color Text color in 0xRRGGBB format (e.g., 0xFFFFFF for white)
     * @param text Text to display
     * @param scale Text scale (1.0 = normal, 1.5 = 150%, etc.)
     * @param shadow Whether to render with drop shadow
     * @param rightAligned If true, text is right-aligned to the x position
     */
    public GUIComponentNativeLabel(float x, float y, int color, String text,
                                    float scale, boolean shadow, boolean rightAligned) {
        super((int) x, (int) y, 0, 0);
        this.posX = x;
        this.posY = y;
        this.color = color;
        this.text = text;
        this.scale = scale;
        this.shadow = shadow;
        this.rightAligned = rightAligned;
    }

    /**
     * Updates the position with float precision.
     * Note: Y is inverted internally to match MTS coordinate system.
     */
    public void setPosition(float x, float y) {
        this.posX = x;
        this.posY = y;
        // Update textPosition to match MTS coordinate system (inverted Y)
        this.textPosition.x = x;
        this.textPosition.y = -y;
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        // Native labels render text directly, not in the standard render pass
    }

    @Override
    public void renderText(boolean renderTextLit, int worldLightValue) {
        if (text != null && !text.isEmpty()) {
            // Use textPosition which has inverted Y, matching MTS coordinate system
            InterfaceManager.renderingInterface.renderNativeText(
                text, (float) textPosition.x, (float) textPosition.y, color, scale, shadow, rightAligned
            );
        }
    }
}
