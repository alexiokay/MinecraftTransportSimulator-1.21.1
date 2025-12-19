package minecrafttransportsimulator.guis.components;

import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;

/**
 * GUI component that renders a texture cutout with sub-pixel (float) precision.
 * This is essential for modern FPS-style HUDs like Superb Warfare where elements
 * need precise positioning (e.g., y - 20.5f for ammo brackets).
 *
 * Unlike {@link GUIComponentCutout} which uses integer positions, this class
 * stores all positions and sizes as floats for pixel-perfect rendering.
 *
 * The texture is specified independently from any GUI texture, allowing
 * weapon HUD elements to use their own textures (fire mode icons, ammo stack, etc.).
 *
 * @author alexispace
 */
public class GUIComponentPreciseCutout extends AGUIComponent {

    //Float position variables for sub-pixel precision
    public float posX;
    public float posY;
    public float renderWidth;
    public float renderHeight;

    //Texture variables
    private final String texturePath;
    private final float textureWidth;
    private final float textureHeight;
    public float textureU;
    public float textureV;
    public float textureSectionWidth;
    public float textureSectionHeight;

    /**
     * Creates a precise cutout component with float positioning.
     *
     * @param x Screen X position (float for sub-pixel precision)
     * @param y Screen Y position (float for sub-pixel precision)
     * @param width Render width on screen
     * @param height Render height on screen
     * @param texturePath Full texture path (e.g., "mts:textures/overlay/ammo_bar/fire_mode/semi.png")
     * @param textureWidth Total texture file width (for UV calculation)
     * @param textureHeight Total texture file height (for UV calculation)
     */
    public GUIComponentPreciseCutout(float x, float y, float width, float height,
                                      String texturePath, float textureWidth, float textureHeight) {
        this(x, y, width, height, texturePath, textureWidth, textureHeight, 0, 0, width, height);
    }

    /**
     * Creates a precise cutout component with float positioning and custom UV coordinates.
     *
     * @param x Screen X position (float for sub-pixel precision)
     * @param y Screen Y position (float for sub-pixel precision)
     * @param width Render width on screen
     * @param height Render height on screen
     * @param texturePath Full texture path
     * @param textureWidth Total texture file width
     * @param textureHeight Total texture file height
     * @param u Texture U offset (in pixels)
     * @param v Texture V offset (in pixels)
     * @param sectionWidth Width of texture section to sample
     * @param sectionHeight Height of texture section to sample
     */
    public GUIComponentPreciseCutout(float x, float y, float width, float height,
                                      String texturePath, float textureWidth, float textureHeight,
                                      float u, float v, float sectionWidth, float sectionHeight) {
        // Pass int-casted values to parent for basic bounds checking
        super((int) x, (int) y, (int) width, (int) height);

        // Store precise float values
        this.posX = x;
        this.posY = y;
        this.renderWidth = width;
        this.renderHeight = height;

        this.texturePath = texturePath;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.textureU = u;
        this.textureV = v;
        this.textureSectionWidth = sectionWidth;
        this.textureSectionHeight = sectionHeight;

        // Create renderable with our own texture (not gui.getTexture())
        RenderableVertices vertexObject = RenderableVertices.createSprite(1, null, null);
        renderable = new RenderableData(vertexObject, texturePath);
    }

    /**
     * Updates the position with float precision. Call this in setStates() to reposition dynamically.
     */
    public void setPosition(float x, float y) {
        this.posX = x;
        this.posY = y;
        // Update parent position for bounds checking
        this.position.x = x;
        this.position.y = -y;  // GUI uses inverted Y
    }

    /**
     * Updates the texture being rendered. Useful for switching fire mode icons.
     */
    public void setTexture(String newTexturePath) {
        renderable.setTexture(newTexturePath);
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        if (renderable.isTranslucent == blendingEnabled) {
            // Use float-precision sprite properties
            renderable.vertexObject.setSpritePropertiesFloat(
                0,
                0, 0,  // Offset within sprite (we use transform for positioning)
                renderWidth, renderHeight,
                textureU / textureWidth,
                textureV / textureHeight,
                (textureU + textureSectionWidth) / textureWidth,
                (textureV + textureSectionHeight) / textureHeight
            );

            // Position using float precision - note Y is inverted for GUI
            renderable.transform.setTranslation(posX, -posY, position.z);
            renderable.setLightValue(gui.worldLightValue);
            renderable.setLightMode(renderBright || ignoreGUILightingState ? LightingMode.IGNORE_ALL_LIGHTING : LightingMode.IGNORE_ORIENTATION_LIGHTING);
            renderable.render();
        }
    }
}
