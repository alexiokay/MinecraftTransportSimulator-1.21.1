package minecrafttransportsimulator.mcinterface;

import java.io.InputStream;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.rendering.GIFParser.ParsedGIF;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableVertices;

/**
 * Interface for the various MC rendering engines.  This class has functions for
 * binding textures, changing lightmap statuses, etc.
 *
 * @author don_bruce
 */
public interface IInterfaceRender {

    /**
     * Returns a 4-float array for the block break texture at the passed-in position in the passed-in world.
     */
    float[] getBlockBreakTexture(AWrapperWorld world, Point3D position);

    /**
     * Returns a 4-float array for the default block texture.  This doesn't take into account world-state.
     */
    float[] getDefaultBlockTexture(String name);

    /**
     * Returns the default font texture base-location folder.  This changes between game versions since new MC versions
     * don't have the texture files and we pack them in ourselves.
     */
    String getDefaultFontTextureFolder();

    /**
     * Returns a stream of the texture specified.  This can vary depending on what texture packs are loaded!
     */
    InputStream getTextureStream(String name);

    /**
     * Renders the item model for the passed-in component.  Only
     * renders the item model: does not render text for counts.
     */
    void renderItemModel(GUIComponentItem component);

    /**
     * Renders according to the set data.
     * If the object is ever deleted, and {@link RenderableVertices#cacheVertices} is true,
     * then {@link #deleteVertices(RenderableData)} should be called to free up the 
     * respective GPU memory.  Calling this is not required if no caching is performed.
     * If the state of the data has changed since the last render, pass in true for the boolean.
     * This allows the rendering system to perform any re-caching as required.
     */
    void renderVertices(RenderableData data, boolean changedSinceLastRender);

    /**
     * Deletes the cached vertices associated with the specified {@link RenderableData}.
     */
    void deleteVertices(RenderableData data);

    /**
     * Binds a URL texture to a stream containing an image.  Pass in a null stream to bind the missing texture to this URL.
     * Returns true if the texture was bound, false if it couldn't be.
     */
    boolean bindURLTexture(String textureURL, InputStream strea);

    /**
     * Binds a URL GIF that was downloaded.
     * Returns true if the texture was bound, false if it couldn't be.
     */
    boolean bindURLGIF(String textureURL, ParsedGIF gif);

    /**
     * Returns an integer that represents the lighting state at the position.
     * This value is version-dependent, and should be stored in {@link RenderableData#worldLightValue}
     */
    int getLightingAtPosition(Point3D position);

    /**
     * Returns true if bounding boxes should be rendered.
     */
    boolean shouldRenderBoundingBoxes();

    /**
     * Returns true if beam rendering should be disabled for shader compatibility.
     * This is used when Iris/Oculus shader mods are detected.
     */
    boolean shouldDisableBeamsForShaderCompatibility();

    /**
     * Renders text using Minecraft's native font system for HUD overlays.
     * This provides the standard Minecraft font appearance that players expect.
     * Used by weapon HUD and other overlays that need to match vanilla style.
     *
     * @param text The text to render
     * @param x Screen X position
     * @param y Screen Y position
     * @param color Text color in 0xRRGGBB format
     * @param scale Text scale (1.0 = normal, 1.5 = 150%, etc.)
     * @param shadow Whether to render with drop shadow
     * @param rightAligned If true, text is right-aligned to the x position
     */
    void renderNativeText(String text, float x, float y, int color, float scale, boolean shadow, boolean rightAligned);

    /**
     * Returns the width of text using Minecraft's native font system.
     * @param text The text to measure
     * @return Width in pixels
     */
    float getNativeTextWidth(String text);

    /**
     * Renders an item at the specified screen position for HUD overlays.
     * Uses Minecraft's native item rendering with proper transforms.
     *
     * @param stack The item stack to render
     * @param x Screen X position
     * @param y Screen Y position
     * @param scale Render scale (1.0 = 16x16 pixels)
     */
    void renderNativeItem(IWrapperItemStack stack, float x, float y, float scale);
}
