package mcinterface1211;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * Client-side rendering event handler for registering GUI layers.
 * Manually registered on the MOD event bus in InterfaceLoader.
 *
 * Following the same pattern as Superb Warfare's ClientRenderHandler (1.21.1 version).
 *
 * @author alexispace
 */
public class ClientRenderingEvents {

    // Resource location for our weapon HUD layer
    public static final ResourceLocation WEAPON_HUD_LAYER = ResourceLocation.fromNamespaceAndPath(InterfaceLoader.MODID, "weapon_hud");

    /**
     * Register custom GUI layers/overlays.
     * This is the NeoForge 1.21.1 equivalent of Forge's RegisterGuiOverlaysEvent.
     * By registering as a proper GUI layer, we get a clean GuiGraphics context
     * without MTS's coordinate transforms.
     *
     * Follows the exact same pattern as Superb Warfare 1.21.1's ClientRenderHandler.registerOverlays()
     */
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        // Register our weapon HUD overlay below all (so it renders early, like Superb Warfare does)
        // Superb Warfare uses registerBelowAll for most of their overlays
        event.registerBelowAll(
            WEAPON_HUD_LAYER,
            (guiGraphics, deltaTracker) -> {
                // Get screen dimensions exactly like Superb Warfare's RenderContext
                int screenWidth = guiGraphics.guiWidth();
                int screenHeight = guiGraphics.guiHeight();

                // Render the weapon HUD with clean context (exactly like Superb Warfare)
                WeaponHUDOverlay.render(guiGraphics, screenWidth, screenHeight);
            }
        );
    }
}
