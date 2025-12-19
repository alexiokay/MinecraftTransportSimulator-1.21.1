package mcinterface1211;

import minecrafttransportsimulator.guis.components.AGUIBase;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * Client-side rendering event handler for registering GUI layers.
 * Manually registered on the MOD event bus in InterfaceLoader.
 *
 * @author alexispace
 */
public class ClientRenderingEvents {

    /**
     * Register custom GUI layers/overlays.
     * This is the NeoForge 1.21.1 equivalent of Forge's RegisterGuiOverlaysEvent.
     *
     * GUIs that return true from renderBelowVanilla() are rendered here,
     * below all vanilla HUD elements (health, hunger, hotbar, etc.).
     */
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        // Register overlay that renders below all vanilla HUD elements
        event.registerBelowAll(
            ResourceLocation.fromNamespaceAndPath(InterfaceLoader.MODID, "below_vanilla_overlay"),
            (guiGraphics, partialTick) -> {
                // Set the GuiGraphics context for native rendering
                InterfaceRender.setCurrentGuiGraphics(guiGraphics);

                // Render all GUIs that want below-vanilla timing
                float partialTicks = partialTick.getGameTimeDeltaPartialTick(true);
                int screenWidth = guiGraphics.guiWidth();
                int screenHeight = guiGraphics.guiHeight();
                int mouseX = 0;  // Not needed for HUD overlays
                int mouseY = 0;

                for (AGUIBase gui : AGUIBase.activeGUIs) {
                    if (gui.renderBelowVanilla() && !gui.capturesPlayer()) {
                        // Initialize GUI if needed
                        if (gui.components.isEmpty() || gui.hasScreenSizeChanged(screenWidth, screenHeight)) {
                            gui.setupComponentsInit(screenWidth, screenHeight);
                        }

                        // Render the GUI using InterfaceRender's GUI rendering logic
                        InterfaceRender.renderBelowVanillaGUI(guiGraphics, gui, mouseX, mouseY, screenWidth, screenHeight, partialTicks);
                    }
                }

                // Clear the context after rendering
                InterfaceRender.clearCurrentGuiGraphics();
            }
        );
    }
}
