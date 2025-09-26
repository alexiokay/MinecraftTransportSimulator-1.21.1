package mcinterface1211;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Bridge between NeoForge config and MTS ConfigSystem.
 * This class syncs values from the NeoForge TOML config to the MTS JSON config system.
 */
@EventBusSubscriber(modid = "mts", value = Dist.CLIENT)
public class ConfigBridge {

    /**
     * Called when NeoForge config is loaded or reloaded.
     * Syncs values from NeoForge config to MTS ConfigSystem.
     */
    @SubscribeEvent
    public static void onConfigLoad(final ModConfigEvent event) {
        if (ConfigSystem.settings != null && ConfigSystem.client != null) {
            syncFromNeoForgeToMTS();
        }
    }

    /**
     * Called after MTS ConfigSystem loads to sync initial values.
     */
    public static void initializeConfigBridge() {
        if (ConfigSystem.settings != null && ConfigSystem.client != null) {
            syncFromNeoForgeToMTS();
        }
    }

    /**
     * Syncs values from NeoForge TOML config to MTS JSON config.
     */
    private static void syncFromNeoForgeToMTS() {
        try {
            // General Settings
            if (MTSConfig.DEV_MODE != null && ConfigSystem.settings != null) {
                ConfigSystem.settings.general.devMode.value = MTSConfig.DEV_MODE.get();
            }

            if (MTSConfig.FUEL_SYSTEM != null && ConfigSystem.settings != null) {
                // MTS fuel system is controlled by fuelUsageFactor - 0 disables, 1+ enables
                ConfigSystem.settings.general.fuelUsageFactor.value = MTSConfig.FUEL_SYSTEM.get() ? 1.0 : 0.0;
            }

            if (MTSConfig.DAMAGE_SYSTEM != null && ConfigSystem.settings != null) {
                // Apply damage system setting to all damage-related configs
                boolean damageEnabled = MTSConfig.DAMAGE_SYSTEM.get();
                ConfigSystem.settings.damage.vehicleDestruction.value = damageEnabled;
                ConfigSystem.settings.damage.vehicleExplosions.value = damageEnabled;
                ConfigSystem.settings.damage.vehicleBlockBreaking.value = damageEnabled;
            }

            // Rendering Settings
            if (MTSConfig.FANCY_LIGHTS != null && ConfigSystem.client != null) {
                ConfigSystem.client.renderingSettings.brightLights.value = MTSConfig.FANCY_LIGHTS.get();
                ConfigSystem.client.renderingSettings.blendedLights.value = MTSConfig.FANCY_LIGHTS.get();
            }

            if (MTSConfig.TRANSPARENT_WINDOWS != null && ConfigSystem.client != null) {
                ConfigSystem.client.renderingSettings.renderWindows.value = MTSConfig.TRANSPARENT_WINDOWS.get();
            }

            if (MTSConfig.RENDER_DISTANCE != null && ConfigSystem.client != null) {
                // MTS doesn't have a direct render distance setting, but we can store it for future use
                // This could be used by rendering code that checks the NeoForge config directly
            }

            // Controls Settings
            if (MTSConfig.MOUSE_YOKE != null && ConfigSystem.client != null) {
                ConfigSystem.client.controlSettings.mouseYokeRate.value = MTSConfig.MOUSE_YOKE.get() ? 0.1 : 0.0;
            }

            if (MTSConfig.SIMPLE_THROTTLE != null && ConfigSystem.client != null) {
                ConfigSystem.client.controlSettings.simpleThrottle.value = MTSConfig.SIMPLE_THROTTLE.get();
            }

            if (MTSConfig.AUTO_START_ENGINES != null && ConfigSystem.client != null) {
                ConfigSystem.client.controlSettings.autostartEng.value = MTSConfig.AUTO_START_ENGINES.get();
            }

            if (MTSConfig.AUTO_TURN_SIGNALS != null && ConfigSystem.client != null) {
                ConfigSystem.client.controlSettings.autoTrnSignals.value = MTSConfig.AUTO_TURN_SIGNALS.get();
            }

            if (MTSConfig.SOUND_VOLUME != null && ConfigSystem.client != null) {
                ConfigSystem.client.controlSettings.soundVolume.value = MTSConfig.SOUND_VOLUME.get().floatValue();
            }

            if (MTSConfig.RADIO_VOLUME != null && ConfigSystem.client != null) {
                ConfigSystem.client.controlSettings.radioVolume.value = MTSConfig.RADIO_VOLUME.get().floatValue();
            }

            // Vehicle Physics Settings
            if (MTSConfig.AIRCRAFT_SPEED_FACTOR != null && ConfigSystem.settings != null) {
                ConfigSystem.settings.general.aircraftSpeedFactor.value = MTSConfig.AIRCRAFT_SPEED_FACTOR.get();
            }

            if (MTSConfig.CAR_SPEED_FACTOR != null && ConfigSystem.settings != null) {
                ConfigSystem.settings.general.carSpeedFactor.value = MTSConfig.CAR_SPEED_FACTOR.get();
            }

            if (MTSConfig.GRAVITY_FACTOR != null && ConfigSystem.settings != null) {
                ConfigSystem.settings.general.gravityFactor.value = MTSConfig.GRAVITY_FACTOR.get();
            }

            // HUD Settings
            if (MTSConfig.RENDER_HUD_1P != null && ConfigSystem.client != null) {
                ConfigSystem.client.renderingSettings.renderHUD_1P.value = MTSConfig.RENDER_HUD_1P.get();
            }

            if (MTSConfig.RENDER_HUD_3P != null && ConfigSystem.client != null) {
                ConfigSystem.client.renderingSettings.renderHUD_3P.value = MTSConfig.RENDER_HUD_3P.get();
            }

            if (MTSConfig.FULL_HUD_1P != null && ConfigSystem.client != null) {
                ConfigSystem.client.renderingSettings.fullHUD_1P.value = MTSConfig.FULL_HUD_1P.get();
            }

            if (MTSConfig.FULL_HUD_3P != null && ConfigSystem.client != null) {
                ConfigSystem.client.renderingSettings.fullHUD_3P.value = MTSConfig.FULL_HUD_3P.get();
            }

            // Save the synchronized settings
            ConfigSystem.saveToDisk();
        } catch (IllegalStateException e) {
            // Config values not loaded yet - this is expected during early initialization
            // We'll sync later when the config is actually loaded
            System.out.println("ConfigBridge: Config values not loaded yet, skipping sync during initialization");
        }
    }

    /**
     * Updates NeoForge config values from MTS settings.
     * This would be called when MTS settings are changed through the in-game GUI.
     */
    public static void syncFromMTSToNeoForge() {
        // This would update the NeoForge config values when MTS settings change
        // Implementation depends on when/how this would be triggered

        if (ConfigSystem.settings != null) {
            // Update NeoForge config values
            MTSConfig.DEV_MODE.set(ConfigSystem.settings.general.devMode.value);
            MTSConfig.FUEL_SYSTEM.set(ConfigSystem.settings.general.fuelUsageFactor.value > 0.0);

            // Update damage system based on vehicleDestruction setting
            MTSConfig.DAMAGE_SYSTEM.set(ConfigSystem.settings.damage.vehicleDestruction.value);
        }

        if (ConfigSystem.client != null) {
            MTSConfig.FANCY_LIGHTS.set(ConfigSystem.client.renderingSettings.brightLights.value);
            MTSConfig.TRANSPARENT_WINDOWS.set(ConfigSystem.client.renderingSettings.renderWindows.value);

            // Controls settings
            MTSConfig.MOUSE_YOKE.set(ConfigSystem.client.controlSettings.mouseYokeRate.value > 0.0);
            MTSConfig.SIMPLE_THROTTLE.set(ConfigSystem.client.controlSettings.simpleThrottle.value);
            MTSConfig.AUTO_START_ENGINES.set(ConfigSystem.client.controlSettings.autostartEng.value);
            MTSConfig.AUTO_TURN_SIGNALS.set(ConfigSystem.client.controlSettings.autoTrnSignals.value);
            MTSConfig.SOUND_VOLUME.set((double) ConfigSystem.client.controlSettings.soundVolume.value);
            MTSConfig.RADIO_VOLUME.set((double) ConfigSystem.client.controlSettings.radioVolume.value);

            // HUD settings
            MTSConfig.RENDER_HUD_1P.set(ConfigSystem.client.renderingSettings.renderHUD_1P.value);
            MTSConfig.RENDER_HUD_3P.set(ConfigSystem.client.renderingSettings.renderHUD_3P.value);
            MTSConfig.FULL_HUD_1P.set(ConfigSystem.client.renderingSettings.fullHUD_1P.value);
            MTSConfig.FULL_HUD_3P.set(ConfigSystem.client.renderingSettings.fullHUD_3P.value);
        }

        // Physics settings
        if (ConfigSystem.settings != null) {
            MTSConfig.AIRCRAFT_SPEED_FACTOR.set(ConfigSystem.settings.general.aircraftSpeedFactor.value);
            MTSConfig.CAR_SPEED_FACTOR.set(ConfigSystem.settings.general.carSpeedFactor.value);
            MTSConfig.GRAVITY_FACTOR.set(ConfigSystem.settings.general.gravityFactor.value);
        }
    }

    /**
     * Updates FMOD status in the NeoForge config.
     * Called by InterfaceSound when FMOD initialization completes.
     */
    public static void updateFMODStatus(String status, String audioSystem, int errorCode) {
        try {
            MTSConfig.FMOD_STATUS.set(status);
            MTSConfig.AUDIO_SYSTEM.set(audioSystem);
            MTSConfig.FMOD_ERROR_CODE.set(errorCode);
        } catch (Exception e) {
            // Config not ready yet - this is expected during early initialization
            System.out.println("ConfigBridge: Config not ready for FMOD status update, skipping");
        }
    }

    /**
     * Called when any screen is about to open.
     * Refresh FMOD status if it's a config screen.
     */
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen().getClass().getName().contains("ConfigurationScreen")) {
            // Just get current status and update config
            String status = InterfaceSound.getCurrentFMODStatus();
            String system = InterfaceSound.getCurrentAudioSystem();
            int errorCode = InterfaceSound.getCurrentFMODErrorCode();

            MTSConfig.FMOD_STATUS.set(status);
            MTSConfig.AUDIO_SYSTEM.set(system);
            MTSConfig.FMOD_ERROR_CODE.set(errorCode);
        }
    }
}