package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;

/**
 * JSON definition for the modern gun HUD overlay system.
 * This is completely optional - if not defined, the legacy text display is used.
 * When defined, provides a Superb Warfare-style visual HUD for guns.
 *
 * Simplified schema: if enabled, always shows icon, name, ammo count.
 * Only customization options (colors, textures) need to be specified.
 *
 * @author don_bruce
 */
@JSONDescription("Optional modern HUD overlay for guns. If not defined, the legacy text display 'Gun:Name Loaded:X' is used. When enabled, always shows weapon icon, name, and ammo count. Only specify customizations like colors and textures.")
public class JSONGunHUD {

    @JSONDescription("Enable the modern gun HUD overlay. If true, shows icon, name, and ammo count. If false or not set, uses legacy text display.")
    public boolean enabled;

    @JSONDescription("Weapon display customization (icon texture, name color).")
    public WeaponDisplay weaponDisplay;

    @JSONDescription("Ammo display customization (colors).")
    public AmmoDisplay ammoDisplay;

    /**
     * Weapon icon and name display customization.
     * Icon and name are ALWAYS shown when HUD is enabled.
     */
    public static class WeaponDisplay {
        @JSONDescription("Custom icon texture path (e.g., 'packid:textures/gun_icon/mygun_icon.png'). If not set, uses item texture.")
        public String iconTexture;

        @JSONDescription("Weapon name text color in hex (e.g., 'FFFFFF' for white). Default white.")
        public String nameColor;
    }

    /**
     * Ammo count display customization.
     * Ammo count is ALWAYS shown when HUD is enabled.
     */
    public static class AmmoDisplay {
        @JSONDescription("Ammo count color when ammo present (hex). Default white.")
        public String countColor;

        @JSONDescription("Ammo count color when empty (hex). Default red.")
        public String countEmptyColor;

        @JSONDescription("Backup ammo text color (hex). Default cyan.")
        public String backupAmmoColor;
    }
}
