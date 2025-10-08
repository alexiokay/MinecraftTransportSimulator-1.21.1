package mcinterface1211.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface1211.InterfaceEventsEntityRendering;
import minecrafttransportsimulator.baseclasses.Point3D;
import net.minecraft.client.Camera;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(double pX, double pY, double pZ);

    // Intercept the setup() method AFTER it completes to re-apply our custom position
    // This ensures vanilla's third-person distance doesn't override our zoom
    @Inject(method = "setup", at = @At("RETURN"))
    private void onSetupComplete(CallbackInfo ci) {
        // If we adjusted the camera, re-apply the position AFTER vanilla's setup
        if (InterfaceEventsEntityRendering.adjustedCamera) {
            Point3D pos = InterfaceEventsEntityRendering.cameraAdjustedPosition;
            setPosition(pos.x, pos.y, pos.z);
        }
    }
}