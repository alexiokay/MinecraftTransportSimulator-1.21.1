package minecrafttransportsimulator.guis.instances;

import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.GUIComponentNativeLabel;
import minecrafttransportsimulator.guis.components.GUIComponentPreciseCutout;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.items.instances.ItemPartGun;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Modern weapon HUD overlay in Superb Warfare style.
 * Uses Minecraft's native font rendering for proper text display.
 *
 * @author alexispace
 */
public class GUIWeaponHUD extends AGUIBase {

    // Texture paths for fire mode icons
    private static final String TEXTURE_SEMI = "mts:textures/overlay/ammo_bar/fire_mode/semi.png";
    private static final String TEXTURE_AUTO = "mts:textures/overlay/ammo_bar/fire_mode/auto.png";
    private static final String TEXTURE_BURST = "mts:textures/overlay/ammo_bar/fire_mode/burst.png";
    private static final String TEXTURE_LINE = "mts:textures/overlay/ammo_bar/fire_mode/line.png";
    private static final String TEXTURE_AMMO_STACK = "mts:textures/overlay/ammo_bar/fire_mode/ammo_stack.png";

    // === Handheld gun HUD components (bottom-right) ===
    private GUIComponentNativeLabel gunNameLabel;
    private GUIComponentNativeLabel ammoTypeLabel;
    private GUIComponentNativeLabel ammoCountLabel;
    private GUIComponentNativeLabel reserveAmmoLabel;
    private GUIComponentNativeLabel keybindLabel;
    private GUIComponentPreciseCutout fireModeIcon;
    private GUIComponentPreciseCutout fireModeLine;
    private GUIComponentPreciseCutout ammoStackBrackets;
    private GUIComponentPreciseCutout gunIcon;
    private GUIComponentPreciseCutout bulletHudIcon;
    private GUIComponentItem bulletItem;
    private GUIComponentItem gunItemFallback;

    // === Vehicle gun HUD components (top-right) ===
    private GUIComponentNativeLabel vehicleGunNameLabel;
    private GUIComponentNativeLabel vehicleGunIndexLabel;
    private GUIComponentNativeLabel vehicleAmmoLabel;

    // State tracking
    private String lastFireMode = "";

    @Override
    public void setupComponents() {
        super.setupComponents();
        background.visible = false;

        int x = screenWidth;
        int y = screenHeight;

        // === HANDHELD GUN HUD COMPONENTS ===
        // Gun name (0.9x scale, white, centered around x-100)
        // NeoForge uses left-aligned text at calculated center position
        addComponent(gunNameLabel = new GUIComponentNativeLabel(
            x - 100f, y - 60f, 0xFFFFFF, "", 0.9f, true, false
        ));
        gunNameLabel.ignoreGUILightingState = true;

        // Ammo type name (0.9x scale, tan color, centered around x-100)
        addComponent(ammoTypeLabel = new GUIComponentNativeLabel(
            x - 100f, y - 51f, 0xC8A679, "", 0.9f, true, false
        ));
        ammoTypeLabel.ignoreGUILightingState = true;

        // Ammo count (1.5x scale, white)
        addComponent(ammoCountLabel = new GUIComponentNativeLabel(
            x - 64f, y - 43f, 0xFFFFFF, "0", 1.5f, true, false
        ));
        ammoCountLabel.ignoreGUILightingState = true;

        // Reserve ammo (infinity symbol, gray)
        addComponent(reserveAmmoLabel = new GUIComponentNativeLabel(
            x - 64f, y - 30f, 0xCCCCCC, "\u221E", 1.0f, true, false
        ));
        reserveAmmoLabel.ignoreGUILightingState = true;

        // Keybind [N] label
        addComponent(keybindLabel = new GUIComponentNativeLabel(
            x - 111.5f, y - 20f, 0xFFFFFF, "[N]", 1.0f, false, false
        ));
        keybindLabel.ignoreGUILightingState = true;

        // Fire mode icon (8x8)
        addComponent(fireModeIcon = new GUIComponentPreciseCutout(
            x - 95f, y - 21f, 8f, 8f, TEXTURE_AUTO, 8f, 8f
        ));
        fireModeIcon.ignoreGUILightingState = true;

        // Fire mode line (8x8)
        addComponent(fireModeLine = new GUIComponentPreciseCutout(
            x - 95f, y - 16f, 8f, 8f, TEXTURE_LINE, 8f, 8f
        ));
        fireModeLine.ignoreGUILightingState = true;

        // Ammo stack brackets (24x8.5)
        addComponent(ammoStackBrackets = new GUIComponentPreciseCutout(
            x - 62f, y - 20.5f, 24f, 8.5f, TEXTURE_AMMO_STACK, 24f, 24f
        ));
        ammoStackBrackets.ignoreGUILightingState = true;

        // Gun icon (64x16)
        addComponent(gunIcon = new GUIComponentPreciseCutout(
            x - 135f, y - 40f, 64f, 16f, "mts:textures/overlay/ammo_bar/fire_mode/dir.png", 64f, 16f
        ));
        gunIcon.ignoreGUILightingState = true;
        gunIcon.visible = false;

        // Bullet HUD icon (12x12)
        addComponent(bulletHudIcon = new GUIComponentPreciseCutout(
            x - 54f, y - 22f, 12f, 12f, "mts:textures/overlay/ammo_bar/fire_mode/dir.png", 12f, 12f
        ));
        bulletHudIcon.ignoreGUILightingState = true;
        bulletHudIcon.visible = false;

        // Bullet item (0.75x scale)
        addComponent(bulletItem = new GUIComponentItem((int)(x - 54), (int)(y - 20), 0.75f));

        // Gun item fallback
        addComponent(gunItemFallback = new GUIComponentItem((int)(x - 135 + 24), (int)(y - 40), 1.0f));
        gunItemFallback.visible = false;

        // === VEHICLE GUN HUD COMPONENTS (top-right) ===
        addComponent(vehicleGunNameLabel = new GUIComponentNativeLabel(
            x - 10f, 10f, 0xFFFFFF, "", 0.9f, true, true
        ));
        vehicleGunNameLabel.ignoreGUILightingState = true;

        addComponent(vehicleGunIndexLabel = new GUIComponentNativeLabel(
            x - 10f, 22f, 0xAAAAAA, "", 1.0f, true, true
        ));
        vehicleGunIndexLabel.ignoreGUILightingState = true;

        addComponent(vehicleAmmoLabel = new GUIComponentNativeLabel(
            x - 10f, 32f, 0xFFFF00, "", 1.2f, true, true
        ));
        vehicleAmmoLabel.ignoreGUILightingState = true;
    }

    @Override
    public void setStates() {
        super.setStates();
        background.visible = false;

        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
        if (player == null || InterfaceManager.clientInterface.isChatOpen()) {
            hideAllComponents();
            return;
        }

        updateHandheldGunHUD(player);
        updateVehicleGunHUD(player);
    }

    private void updateHandheldGunHUD(IWrapperPlayer player) {
        ItemPartGun heldGunItem = null;
        IWrapperNBT heldGunData = null;

        AItemBase heldItem = player.getHeldItem();
        if (heldItem instanceof ItemPartGun) {
            ItemPartGun gunItem = (ItemPartGun) heldItem;
            if (gunItem.definition.gun.handHeld && gunItem.definition.gunHUD != null && gunItem.definition.gunHUD.enabled) {
                heldGunItem = gunItem;
                IWrapperItemStack heldStack = player.getHeldStack();
                if (heldStack != null) {
                    heldGunData = heldStack.getData();
                }
            }
        }

        if (heldGunItem == null) {
            hideHandheldComponents();
            return;
        }

        showHandheldComponents();

        float x = screenWidth;
        float y = screenHeight;

        // Gun name - use native text width for positioning
        // NeoForge formula: x/0.9 - (100 + font.width(gunName)/2) / 0.9
        // Which simplifies to: (x - 100 - width/2) / 0.9, then scaled by 0.9 = x - 100 - width/2
        String gunName = heldGunItem.getItemName();
        gunNameLabel.text = gunName;
        float gunNameWidth = InterfaceManager.renderingInterface.getNativeTextWidth(gunName);
        gunNameLabel.setPosition(x - 100 - gunNameWidth / 2f, y - 60);

        // Read ammo data
        int ammoCount = 0;
        ItemBullet loadedBullet = null;

        if (heldGunData != null) {
            int loadedBulletsSize = heldGunData.getInteger("loadedBulletsSize");
            for (int i = 0; i < loadedBulletsSize; i++) {
                if (heldGunData.hasKey("loadedBullet" + i)) {
                    IWrapperNBT bulletData = heldGunData.getData("loadedBullet" + i);
                    if (bulletData != null) {
                        ammoCount += bulletData.getInteger("count");
                        if (i == 0 && loadedBullet == null) {
                            AItemBase item = bulletData.getPackItem();
                            if (item instanceof ItemBullet) {
                                loadedBullet = (ItemBullet) item;
                            }
                        }
                    }
                }
            }
            if (loadedBullet == null && heldGunData.hasKey("lastLoadedBullet")) {
                IWrapperNBT lastBulletData = heldGunData.getData("lastLoadedBullet");
                if (lastBulletData != null) {
                    AItemBase item = lastBulletData.getPackItem();
                    if (item instanceof ItemBullet) {
                        loadedBullet = (ItemBullet) item;
                    }
                }
            }
        }

        // Ammo count
        ammoCountLabel.text = String.valueOf(ammoCount);
        ammoCountLabel.setPosition(x - 64, y - 43);

        // Reserve ammo
        reserveAmmoLabel.setPosition(x - 64f, y - 30f);

        // Ammo type name and bullet display
        if (loadedBullet != null) {
            String ammoName = loadedBullet.getItemName();
            ammoTypeLabel.text = ammoName;
            float ammoNameWidth = InterfaceManager.renderingInterface.getNativeTextWidth(ammoName);
            ammoTypeLabel.setPosition(x - 100 - ammoNameWidth / 2f, y - 51);

            boolean hasAmmoCapacity = heldGunItem.definition.gun.capacity > 0;
            ammoStackBrackets.visible = hasAmmoCapacity;
            ammoStackBrackets.setPosition(x - 62f, y - 20.5f);

            String bulletHudIconPath = loadedBullet.definition.bullet != null ? loadedBullet.definition.bullet.hudIcon : null;
            if (bulletHudIconPath != null && !bulletHudIconPath.isEmpty() && hasAmmoCapacity) {
                bulletHudIcon.setTexture(formatTexturePath(bulletHudIconPath));
                bulletHudIcon.setPosition(x - 54f, y - 22f);
                bulletHudIcon.visible = true;
                bulletItem.visible = false;
                bulletItem.stack = null;
            } else if (hasAmmoCapacity) {
                bulletItem.stack = loadedBullet.getNewStack(null);
                bulletItem.position.x = (int)(x - 54);
                bulletItem.position.y = -(int)(y - 20);
                bulletItem.visible = true;
                bulletHudIcon.visible = false;
            } else {
                bulletItem.stack = null;
                bulletItem.visible = false;
                bulletHudIcon.visible = false;
            }
        } else {
            ammoTypeLabel.text = "";
            bulletItem.stack = null;
            bulletItem.visible = false;
            bulletHudIcon.visible = false;
            ammoStackBrackets.visible = false;
        }

        // Keybind and fire mode positions
        keybindLabel.setPosition(x - 111.5f, y - 20f);
        fireModeIcon.setPosition(x - 95f, y - 21f);
        fireModeLine.setPosition(x - 95f, y - 16f);

        // Gun icon
        gunIcon.setPosition(x - 135f, y - 40f);
        if (heldGunItem.definition.gunHUD.weaponDisplay != null &&
            heldGunItem.definition.gunHUD.weaponDisplay.iconTexture != null &&
            !heldGunItem.definition.gunHUD.weaponDisplay.iconTexture.isEmpty()) {
            gunIcon.setTexture(formatTexturePath(heldGunItem.definition.gunHUD.weaponDisplay.iconTexture));
            gunIcon.visible = true;
            gunItemFallback.visible = false;
            gunItemFallback.stack = null;
        } else {
            gunIcon.visible = false;
            gunItemFallback.stack = heldGunItem.getNewStack(null);
            gunItemFallback.position.x = (int)(x - 135 + 24);
            gunItemFallback.position.y = -(int)(y - 40);
            gunItemFallback.visible = true;
        }

        // Fire mode
        String currentFireMode = getFireMode(heldGunItem, heldGunData);
        updateFireModeIcon(currentFireMode);
    }

    private void updateVehicleGunHUD(IWrapperPlayer player) {
        if (!(player.getEntityRiding() instanceof PartSeat)) {
            hideVehicleComponents();
            return;
        }

        PartSeat seat = (PartSeat) player.getEntityRiding();
        if (!seat.canControlGuns || seat.activeGunItem == null || !seat.gunGroups.containsKey(seat.activeGunItem)) {
            hideVehicleComponents();
            return;
        }

        java.util.List<PartGun> guns = seat.gunGroups.get(seat.activeGunItem);
        if (guns == null || guns.isEmpty() || seat.gunIndex >= guns.size()) {
            hideVehicleComponents();
            return;
        }

        PartGun vehicleGun = guns.get(seat.gunIndex);
        if (vehicleGun.definition.gunHUD == null || !vehicleGun.definition.gunHUD.enabled) {
            hideVehicleComponents();
            return;
        }

        showVehicleComponents();

        float x = screenWidth;
        float topY = 10f;

        String gunName = vehicleGun.cachedItem.getItemName();
        vehicleGunNameLabel.text = gunName;
        float gunNameWidth = InterfaceManager.renderingInterface.getNativeTextWidth(gunName);
        vehicleGunNameLabel.setPosition(x - 10 - gunNameWidth, topY);

        String ammoText = vehicleGun.getBulletText();
        vehicleAmmoLabel.text = ammoText;
        float ammoWidth = InterfaceManager.renderingInterface.getNativeTextWidth(ammoText);
        vehicleAmmoLabel.setPosition(x - 10 - ammoWidth, topY + 22);

        if (vehicleGun.cachedItem.definition.gun.fireSolo) {
            String indexText = "[" + (seat.gunIndex + 1) + "]";
            vehicleGunIndexLabel.text = indexText;
            float indexWidth = InterfaceManager.renderingInterface.getNativeTextWidth(indexText);
            vehicleGunIndexLabel.setPosition(x - 10 - indexWidth, topY + 12);
            vehicleGunIndexLabel.visible = true;
        } else {
            vehicleGunIndexLabel.visible = false;
        }
    }

    private String getFireMode(ItemPartGun gunItem, IWrapperNBT gunData) {
        if (gunData != null && gunData.hasKey("currentFireModeIndex")) {
            int fireModeIndex = gunData.getInteger("currentFireModeIndex");
            if (gunItem.definition.gun.fireModes != null && !gunItem.definition.gun.fireModes.isEmpty()) {
                if (fireModeIndex >= 0 && fireModeIndex < gunItem.definition.gun.fireModes.size()) {
                    return gunItem.definition.gun.fireModes.get(fireModeIndex);
                }
                return gunItem.definition.gun.fireModes.get(0);
            }
            return gunItem.definition.gun.isSemiAuto ? "semi" : "auto";
        }
        if (gunItem.definition.gun.fireModes != null && !gunItem.definition.gun.fireModes.isEmpty()) {
            return gunItem.definition.gun.fireModes.get(0);
        }
        return gunItem.definition.gun.isSemiAuto ? "semi" : "auto";
    }

    private String formatTexturePath(String path) {
        if (path == null || path.isEmpty()) {
            return "mts:textures/overlay/ammo_bar/fire_mode/dir.png";
        }
        if (!path.endsWith(".png")) {
            path = path + ".png";
        }
        if (!path.contains(":")) {
            path = "mts:" + path;
        }
        return path;
    }

    private void updateFireModeIcon(String fireMode) {
        if (!fireMode.equals(lastFireMode)) {
            switch (fireMode.toLowerCase()) {
                case "semi": fireModeIcon.setTexture(TEXTURE_SEMI); break;
                case "burst": fireModeIcon.setTexture(TEXTURE_BURST); break;
                default: fireModeIcon.setTexture(TEXTURE_AUTO); break;
            }
            lastFireMode = fireMode;
        }
    }

    private void hideAllComponents() {
        hideHandheldComponents();
        hideVehicleComponents();
    }

    private void hideHandheldComponents() {
        gunNameLabel.visible = false;
        ammoTypeLabel.visible = false;
        ammoCountLabel.visible = false;
        reserveAmmoLabel.visible = false;
        keybindLabel.visible = false;
        fireModeIcon.visible = false;
        fireModeLine.visible = false;
        ammoStackBrackets.visible = false;
        gunIcon.visible = false;
        bulletHudIcon.visible = false;
        bulletItem.visible = false;
        gunItemFallback.visible = false;
    }

    private void showHandheldComponents() {
        gunNameLabel.visible = true;
        ammoTypeLabel.visible = true;
        ammoCountLabel.visible = true;
        reserveAmmoLabel.visible = true;
        keybindLabel.visible = true;
        fireModeIcon.visible = true;
        fireModeLine.visible = true;
    }

    private void hideVehicleComponents() {
        vehicleGunNameLabel.visible = false;
        vehicleGunIndexLabel.visible = false;
        vehicleAmmoLabel.visible = false;
    }

    private void showVehicleComponents() {
        vehicleGunNameLabel.visible = true;
        vehicleAmmoLabel.visible = true;
    }

    @Override protected boolean renderBackground() { return false; }
    @Override protected boolean canStayOpen() { return true; }
    @Override public boolean capturesPlayer() { return false; }
    @Override public int getWidth() { return screenWidth; }
    @Override public int getHeight() { return screenHeight; }
    @Override public boolean renderFlushBottom() { return true; }
    @Override public boolean renderTranslucent() { return true; }
    @Override public boolean renderBelowVanilla() { return true; }
    @Override protected String getTexture() { return null; }
}
