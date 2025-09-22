package mcinterface1211.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface1211.BuilderItem;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * Mixin to intercept GUI item rendering and provide simple placeholders for pack items.
 */
@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {

    @Inject(method = "renderItem(Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"), cancellable = true)
    private void interceptGUIItemRendering(ItemStack stack, int x, int y, CallbackInfo ci) {
        // Check if this is a pack item
        if (stack.getItem() instanceof BuilderItem) {
            BuilderItem builderItem = (BuilderItem) stack.getItem();
            if (builderItem.getWrappedItem() instanceof AItemPack) {
                InterfaceManager.coreInterface.logError("GUI ITEM RENDERER INTERCEPT: Intercepted GUI render for pack item: " + builderItem.getWrappedItem().getRegistrationName() + " at position (" + x + ", " + y + ")");

                // Render a simple placeholder - a colored rectangle for now
                GuiGraphics guiGraphics = (GuiGraphics) (Object) this;

                // Fill with a semi-transparent red color to indicate this is a placeholder
                guiGraphics.fill(x, y, x + 16, y + 16, 0x80FF0000); // Semi-transparent red

                // Add a small border
                guiGraphics.fill(x, y, x + 16, y + 1, 0xFFFFFFFF); // White top border
                guiGraphics.fill(x, y, x + 1, y + 16, 0xFFFFFFFF); // White left border
                guiGraphics.fill(x + 15, y, x + 16, y + 16, 0xFF000000); // Black right border
                guiGraphics.fill(x, y + 15, x + 16, y + 16, 0xFF000000); // Black bottom border

                // Cancel the original rendering
                ci.cancel();
            }
        }
    }
}