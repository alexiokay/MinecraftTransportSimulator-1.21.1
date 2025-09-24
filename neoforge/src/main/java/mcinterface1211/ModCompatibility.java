package mcinterface1211;

import net.neoforged.fml.ModList;

/**
 * Centralized mod compatibility detection and management.
 * This class handles all mod presence checks and compatibility logic
 * to avoid scattered mod checks throughout the codebase.
 */
public class ModCompatibility {

    // Cached mod presence checks for performance
    private static Boolean hasShaderMod = null;

    /**
     * Check if Iris/Oculus shader mod is installed (not necessarily active).
     */
    public static boolean hasShaderMod() {
        if (hasShaderMod == null) {
            hasShaderMod = ModList.get().isLoaded("iris") || ModList.get().isLoaded("oculus");
        }
        return hasShaderMod;
    }

    /**
     * Check if shaders are actively enabled (not just the mod present).
     * This checks if a shader pack is actually loaded and running.
     */
    public static boolean areShadersEnabled() {
        if (!hasShaderMod()) {
            return false;
        }

        try {
            // Try Iris API first
            if (ModList.get().isLoaded("iris")) {
                Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Object apiInstance = irisApi.getMethod("getInstance").invoke(null);
                Object result = irisApi.getMethod("isShaderPackInUse").invoke(apiInstance);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
            }

            // Try Oculus API
            if (ModList.get().isLoaded("oculus")) {
                try {
                    Class<?> oculusApi = Class.forName("net.coderbot.iris.api.v0.IrisApi");
                    Object result = oculusApi.getMethod("isShaderPackInUse").invoke(null);
                    if (result instanceof Boolean) {
                        return (Boolean) result;
                    }
                } catch (Exception e) {
                    // Oculus API failed, assume disabled to allow beams
                    return false;
                }
            }
        } catch (Exception e) {
            // API not available, assume disabled to allow beams
        }

        // If we can't detect, assume disabled to allow beams to work
        return false;
    }

    /**
     * Reset cached values (useful for development/testing).
     */
    public static void resetCache() {
        hasShaderMod = null;
    }

    /**
     * Get debug information about detected mod compatibility.
     */
    public static String getDebugInfo() {
        return "Shader Mod (Iris/Oculus): " + (hasShaderMod() ? "DETECTED" : "not found");
    }
}