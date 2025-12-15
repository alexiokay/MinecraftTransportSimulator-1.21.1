package mcinterface1211.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mcinterface1211.InterfaceLoader;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.packloading.PackParser;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

/**
 * Mixin to directly intercept model loading and provide generated models for pack items.
 */
@Mixin(ModelBakery.class)
public class ModelBakeryMixin {

    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    private void interceptModelLoading(ResourceLocation modelLocation, CallbackInfoReturnable<UnbakedModel> cir) {
        // Only handle MTS namespace models that look like pack items
        if (modelLocation.getNamespace().equals(InterfaceLoader.MODID) &&
            modelLocation.getPath().startsWith("item/") &&
            modelLocation.getPath().contains(".")) {

            String itemName = modelLocation.getPath().substring("item/".length());

            // Check if this is a pack item
            for (AItemPack<?> packItem : PackParser.getAllPackItems()) {
                if (packItem != null && packItem.getRegistrationName().equals(itemName)) {
                    // For now, just don't intercept and let it fail normally
                    // We'll focus on the resource loading instead
                    break;
                }
            }
        }
    }
}