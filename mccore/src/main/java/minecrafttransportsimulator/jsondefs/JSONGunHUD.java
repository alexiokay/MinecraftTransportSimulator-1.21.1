package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;

/**
 * JSON definition for the modern gun HUD overlay system.
 * This is completely optional - if not defined, the legacy text display is used.
 * When defined, provides a Superb Warfare-style visual HUD for guns.
 *
 * Note: Position and layout are handled by the core mod, not by pack makers.
 * Pack makers only define what content to show and colors/textures.
 *
 * @author don_bruce
 */
@JSONDescription("Optional modern HUD overlay for guns. If not defined, the legacy text display 'Gun:Name Loaded:X' is used. When defined, shows a modern icon-based HUD with ammo bars, fire modes, and more. Position is controlled by the core mod settings.")
public class JSONGunHUD {

    @JSONDescription("Enable the modern gun HUD overlay. If false or not set, uses legacy text display.")
    public boolean enabled = true;

    @JSONDescription("Weapon display settings (icon and name).")
    public WeaponDisplay weaponDisplay;

    @JSONDescription("Ammo display settings (count, bar, backup ammo).")
    public AmmoDisplay ammoDisplay;

    @JSONDescription("Fire mode display settings (semi, burst, auto switching).")
    public FireModeDisplay fireModeDisplay;

    @JSONDescription("Ammo type selection display (when multiple ammo types are loaded).")
    public AmmoTypeDisplay ammoTypeDisplay;

    @JSONDescription("Heat/overheat display settings.")
    public HeatDisplay heatDisplay;

    @JSONDescription("Lock-on targeting display settings.")
    public LockOnDisplay lockOnDisplay;

    /**
     * Weapon icon and name display settings.
     */
    public static class WeaponDisplay {
        @JSONDescription("Show weapon icon. Default true.")
        public boolean showIcon = true;

        @JSONDescription("Custom icon texture path (e.g., 'packid:textures/gun_icon/mygun_icon.png'). If not set, uses item texture. Texture must be 256x64 pixels, white silhouette on transparent background (same as Superb Warfare format).")
        public String iconTexture;

        @JSONDescription("Show weapon name. Default true.")
        public boolean showName = true;

        @JSONDescription("Weapon name text color in hex (e.g., 'FFFFFF' for white). Default white.")
        public String nameColor = "FFFFFF";
    }

    /**
     * Ammo count and bar display settings.
     */
    public static class AmmoDisplay {
        @JSONDescription("Show ammo count number. Default true.")
        public boolean showCount = true;

        @JSONDescription("Ammo count color when ammo present (hex). Default white.")
        public String countColor = "FFFFFF";

        @JSONDescription("Ammo count color when empty (hex). Default red.")
        public String countEmptyColor = "FF0000";

        @JSONDescription("Show ammo bar. Default false.")
        public boolean showBar = false;

        @JSONDescription("Custom ammo bar texture. If not set, uses solid color.")
        public String barTexture;

        @JSONDescription("Ammo bar color when full (hex). Default yellow-gold.")
        public String barColor = "FFCC00";

        @JSONDescription("Ammo bar color when empty (hex). Default red.")
        public String barEmptyColor = "FF0000";

        @JSONDescription("Show backup/reserve ammo count (capacity). Default true.")
        public boolean showBackupAmmo = true;

        @JSONDescription("Backup ammo text color (hex). Default cyan.")
        public String backupAmmoColor = "00FFFF";
    }

    /**
     * Fire mode display and switching settings.
     */
    public static class FireModeDisplay {
        @JSONDescription("Enable fire mode display. Default false (single mode guns don't need this).")
        public boolean enabled = false;

        @JSONDescription("Available fire modes for this gun.")
        public List<FireMode> modes;
    }

    /**
     * Individual fire mode definition.
     */
    public static class FireMode {
        @JSONDescription("Internal mode name (used for logic).")
        public String name;

        @JSONDescription("Display text shown on HUD (e.g., 'SEMI', 'AUTO', 'BURST').")
        public String displayText;

        @JSONDescription("Optional icon texture for this mode.")
        public String icon;

        @JSONDescription("For burst mode, number of rounds per burst. Default 3.")
        public int burstCount = 3;

        @JSONDescription("Fire delay override for this mode (ticks). If 0, uses gun default.")
        public float fireDelay = 0;

        @JSONDescription("If true, gun fires only once per trigger pull in this mode.")
        public boolean isSemiAuto = false;
    }

    /**
     * Ammo type selection display (for guns with multiple ammo types).
     */
    public static class AmmoTypeDisplay {
        @JSONDescription("Enable ammo type display. Default false.")
        public boolean enabled = false;

        @JSONDescription("Show name of current ammo type. Default false.")
        public boolean showAmmoName = false;

        @JSONDescription("Ammo name text color (hex). Default white.")
        public String ammoNameColor = "FFFFFF";
    }

    /**
     * Heat/overheat display settings.
     */
    public static class HeatDisplay {
        @JSONDescription("Enable heat display. Default false.")
        public boolean enabled = false;

        @JSONDescription("Custom heat bar texture.")
        public String barTexture;

        @JSONDescription("Heat bar color when cool (hex). Default green.")
        public String coolColor = "00FF00";

        @JSONDescription("Heat bar color when hot (hex). Default red.")
        public String hotColor = "FF0000";

        @JSONDescription("Show overheat warning flash. Default true.")
        public boolean overheatWarning = true;

        @JSONDescription("Heat threshold (0.0-1.0) to start showing warning. Default 0.8.")
        public float warningThreshold = 0.8f;
    }

    /**
     * Lock-on targeting display settings.
     */
    public static class LockOnDisplay {
        @JSONDescription("Enable lock-on display. Default false.")
        public boolean enabled = false;

        @JSONDescription("Texture shown while acquiring lock.")
        public String lockingTexture;

        @JSONDescription("Texture shown when lock acquired.")
        public String lockedTexture;

        @JSONDescription("Show target name when locked. Default true.")
        public boolean showTargetName = true;

        @JSONDescription("Target name text color (hex). Default red.")
        public String targetNameColor = "FF0000";

        @JSONDescription("Show distance to target. Default false.")
        public boolean showDistance = false;
    }
}
