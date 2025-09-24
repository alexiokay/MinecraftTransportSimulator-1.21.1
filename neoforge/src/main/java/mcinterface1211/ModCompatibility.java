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
     * Check if Iris/Oculus shader mod is active.
     * When true, MTS should use standard Minecraft shaders instead of custom MTS shaders.
     */
    public static boolean hasShaderMod() {
        if (hasShaderMod == null) {
            hasShaderMod = ModList.get().isLoaded("iris") || ModList.get().isLoaded("oculus");
        }
        return hasShaderMod;
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