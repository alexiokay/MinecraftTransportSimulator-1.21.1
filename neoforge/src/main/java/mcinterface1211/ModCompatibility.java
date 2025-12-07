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
    private static Boolean hasSodium = null;
    private static Boolean hasEmbeddium = null;

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
     * Check if shaders are actively enabled AND if compatibility mode is enabled in config.
     * This checks if a shader pack is actually loaded and running, and if the user wants
     * to use vanilla shaders for compatibility with shader packs.
     * Priority: Client config > NeoForge config
     */
    public static boolean areShadersEnabled() {
        // Check client config first (in-game menu setting)
        if (minecrafttransportsimulator.systems.ConfigSystem.client != null &&
            minecrafttransportsimulator.systems.ConfigSystem.client.renderingSettings.shaderCompat != null) {
            if (!minecrafttransportsimulator.systems.ConfigSystem.client.renderingSettings.shaderCompat.value) {
                return false;
            }
        } else if (!MTSConfig.IRIS_SHADER_COMPATIBILITY.get()) {
            // Fallback to NeoForge config
            return false;
        }

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
     * Check if Sodium is installed (incompatible with custom shaders).
     */
    public static boolean hasSodium() {
        if (hasSodium == null) {
            hasSodium = ModList.get().isLoaded("sodium");
        }
        return hasSodium;
    }

    /**
     * Check if Embeddium is installed (compatible with custom shaders).
     */
    public static boolean hasEmbeddium() {
        if (hasEmbeddium == null) {
            hasEmbeddium = ModList.get().isLoaded("embeddium") || ModList.get().isLoaded("rubidium");
        }
        return hasEmbeddium;
    }

    /**
     * Check if we're running with Iris + Sodium (known to have rendering issues with custom shaders).
     */
    public static boolean hasIrisAndSodium() {
        return ModList.get().isLoaded("iris") && hasSodium();
    }

    /**
     * Check if we're running with Oculus + Embeddium (more compatible with custom shaders).
     */
    public static boolean hasOculusAndEmbeddium() {
        return ModList.get().isLoaded("oculus") && hasEmbeddium();
    }

    /**
     * Reset cached values (useful for development/testing).
     */
    public static void resetCache() {
        hasShaderMod = null;
        hasSodium = null;
        hasEmbeddium = null;
    }

    /**
     * Get debug information about detected mod compatibility.
     */
    public static String getDebugInfo() {
        String rendering = "UNKNOWN";
        if (hasIrisAndSodium()) {
            rendering = "Iris + Sodium (Custom shaders may have issues)";
        } else if (hasOculusAndEmbeddium()) {
            rendering = "Oculus + Embeddium (Custom shaders compatible)";
        } else if (ModList.get().isLoaded("iris")) {
            rendering = "Iris (no Sodium)";
        } else if (ModList.get().isLoaded("oculus")) {
            rendering = "Oculus (no Embeddium)";
        } else if (hasSodium()) {
            rendering = "Sodium only";
        } else if (hasEmbeddium()) {
            rendering = "Embeddium only";
        } else {
            rendering = "Vanilla";
        }

        return "Rendering: " + rendering +
               ", Shader Compatibility Mode: " + (MTSConfig.IRIS_SHADER_COMPATIBILITY.get() ? "ENABLED" : "DISABLED");
    }
}