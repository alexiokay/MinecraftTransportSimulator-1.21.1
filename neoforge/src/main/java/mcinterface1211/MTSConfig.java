package mcinterface1211;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = "mts")
public class MTSConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Audio Configuration
    public static final ModConfigSpec.ConfigValue<String> FMOD_STATUS;
    public static final ModConfigSpec.BooleanValue FMOD_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> AUDIO_SYSTEM;
    public static final ModConfigSpec.IntValue FMOD_ERROR_CODE;

    // General Settings
    public static final ModConfigSpec.BooleanValue DEV_MODE;
    public static final ModConfigSpec.BooleanValue FUEL_SYSTEM;
    public static final ModConfigSpec.BooleanValue DAMAGE_SYSTEM;

    // Rendering Settings
    public static final ModConfigSpec.BooleanValue FANCY_LIGHTS;
    public static final ModConfigSpec.BooleanValue TRANSPARENT_WINDOWS;
    public static final ModConfigSpec.IntValue RENDER_DISTANCE;

    // Controls Settings
    public static final ModConfigSpec.BooleanValue MOUSE_YOKE;
    public static final ModConfigSpec.BooleanValue JOYSTICK_ENABLED;
    public static final ModConfigSpec.BooleanValue SIMPLE_THROTTLE;
    public static final ModConfigSpec.BooleanValue AUTO_START_ENGINES;
    public static final ModConfigSpec.BooleanValue AUTO_TURN_SIGNALS;
    public static final ModConfigSpec.DoubleValue SOUND_VOLUME;
    public static final ModConfigSpec.DoubleValue RADIO_VOLUME;

    // Vehicle Physics Settings
    public static final ModConfigSpec.DoubleValue AIRCRAFT_SPEED_FACTOR;
    public static final ModConfigSpec.DoubleValue CAR_SPEED_FACTOR;
    public static final ModConfigSpec.DoubleValue GRAVITY_FACTOR;

    // HUD Settings
    public static final ModConfigSpec.BooleanValue RENDER_HUD_1P;
    public static final ModConfigSpec.BooleanValue RENDER_HUD_3P;
    public static final ModConfigSpec.BooleanValue FULL_HUD_1P;
    public static final ModConfigSpec.BooleanValue FULL_HUD_3P;

    static {
        BUILDER.push("Audio");
        FMOD_STATUS = BUILDER
            .comment("Current FMOD initialization status")
            .translation("config.audio.fmod_status")
            .define("fmodStatus", "Not initialized");
        FMOD_ENABLED = BUILDER
            .comment("Whether FMOD audio system is enabled")
            .translation("config.audio.fmod_enabled")
            .define("fmodEnabled", true);
        AUDIO_SYSTEM = BUILDER
            .comment("Current active audio system (FMOD/OpenAL)")
            .translation("config.audio.system")
            .define("audioSystem", "Pending");
        FMOD_ERROR_CODE = BUILDER
            .comment("Last FMOD error code (0 = success, 20 = hardware conflict)")
            .translation("config.audio.error_code")
            .defineInRange("fmodErrorCode", -1, -1, 999);
        BUILDER.pop();

        BUILDER.push("General");
        DEV_MODE = BUILDER
            .comment("Enable developer mode features")
            .translation("config.general.dev_mode")
            .define("devMode", false);
        FUEL_SYSTEM = BUILDER
            .comment("Enable fuel consumption system")
            .translation("config.general.fuel_system")
            .define("fuelSystem", true);
        DAMAGE_SYSTEM = BUILDER
            .comment("Enable damage system")
            .translation("config.general.damage_system")
            .define("damageSystem", true);
        BUILDER.pop();

        BUILDER.push("Rendering");
        FANCY_LIGHTS = BUILDER
            .comment("Enable fancy lighting effects")
            .translation("config.rendering.fancy_lights")
            .define("fancyLights", true);
        TRANSPARENT_WINDOWS = BUILDER
            .comment("Enable transparent windows")
            .translation("config.rendering.transparent_windows")
            .define("transparentWindows", true);
        RENDER_DISTANCE = BUILDER
            .comment("Maximum render distance for vehicles")
            .translation("config.rendering.render_distance")
            .defineInRange("renderDistance", 16, 1, 64);
        BUILDER.pop();

        BUILDER.push("Controls");
        MOUSE_YOKE = BUILDER
            .comment("Use mouse as yoke for aircraft")
            .translation("config.controls.mouse_yoke")
            .define("mouseYoke", false);
        JOYSTICK_ENABLED = BUILDER
            .comment("Enable joystick support")
            .translation("config.controls.joystick_enabled")
            .define("joystickEnabled", true);
        SIMPLE_THROTTLE = BUILDER
            .comment("Vehicles automatically go into reverse after stopping with brake")
            .translation("config.controls.simple_throttle")
            .define("simpleThrottle", true);
        AUTO_START_ENGINES = BUILDER
            .comment("Engines automatically start when entering vehicles")
            .translation("config.controls.auto_start_engines")
            .define("autoStartEngines", true);
        AUTO_TURN_SIGNALS = BUILDER
            .comment("Turn signals activate automatically when turning")
            .translation("config.controls.auto_turn_signals")
            .define("autoTurnSignals", true);
        SOUND_VOLUME = BUILDER
            .comment("Volume for all mod sounds (0.0 to 1.0)")
            .translation("config.controls.sound_volume")
            .defineInRange("soundVolume", 1.0, 0.0, 1.0);
        RADIO_VOLUME = BUILDER
            .comment("Volume for vehicle radios (0.0 to 1.0)")
            .translation("config.controls.radio_volume")
            .defineInRange("radioVolume", 1.0, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("Physics");
        AIRCRAFT_SPEED_FACTOR = BUILDER
            .comment("Factor to apply to aircraft movement (1.0 = realistic, 0.35 = default)")
            .translation("config.physics.aircraft_speed_factor")
            .defineInRange("aircraftSpeedFactor", 0.35, 0.1, 2.0);
        CAR_SPEED_FACTOR = BUILDER
            .comment("Factor to apply to car movement (1.0 = realistic, 0.35 = default)")
            .translation("config.physics.car_speed_factor")
            .defineInRange("carSpeedFactor", 0.35, 0.1, 2.0);
        GRAVITY_FACTOR = BUILDER
            .comment("Factor for gravitational forces applied to vehicles")
            .translation("config.physics.gravity_factor")
            .defineInRange("gravityFactor", 1.0, 0.1, 3.0);
        BUILDER.pop();

        BUILDER.push("HUD");
        RENDER_HUD_1P = BUILDER
            .comment("Render HUD in first-person view")
            .translation("config.hud.render_hud_1p")
            .define("renderHud1P", true);
        RENDER_HUD_3P = BUILDER
            .comment("Render HUD in third-person view")
            .translation("config.hud.render_hud_3p")
            .define("renderHud3P", true);
        FULL_HUD_1P = BUILDER
            .comment("Use full-size HUD in first-person")
            .translation("config.hud.full_hud_1p")
            .define("fullHud1P", false);
        FULL_HUD_3P = BUILDER
            .comment("Use full-size HUD in third-person")
            .translation("config.hud.full_hud_3p")
            .define("fullHud3P", false);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Config loaded
    }
}