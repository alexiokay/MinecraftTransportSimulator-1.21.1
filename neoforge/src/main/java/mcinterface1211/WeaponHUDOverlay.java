package mcinterface1211;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.items.instances.ItemPartGun;
import minecrafttransportsimulator.jsondefs.JSONGunHUD;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

/**
 * Direct weapon HUD overlay renderer that bypasses MTS's GUI system.
 * This uses Minecraft's native GuiGraphics.blit() method exactly like Superb Warfare.
 *
 * @author alexispace
 */
public class WeaponHUDOverlay {

    // Fire mode texture locations
    private static final ResourceLocation TEXTURE_SEMI = ResourceLocation.fromNamespaceAndPath("mts", "textures/overlay/ammo_bar/fire_mode/semi.png");
    private static final ResourceLocation TEXTURE_AUTO = ResourceLocation.fromNamespaceAndPath("mts", "textures/overlay/ammo_bar/fire_mode/auto.png");
    private static final ResourceLocation TEXTURE_BURST = ResourceLocation.fromNamespaceAndPath("mts", "textures/overlay/ammo_bar/fire_mode/burst.png");
    private static final ResourceLocation TEXTURE_LINE = ResourceLocation.fromNamespaceAndPath("mts", "textures/overlay/ammo_bar/fire_mode/line.png");
    private static final ResourceLocation TEXTURE_AMMO_STACK = ResourceLocation.fromNamespaceAndPath("mts", "textures/overlay/ammo_bar/fire_mode/ammo_stack.png");

    /**
     * Precise blit that accepts float coordinates - exactly like Superb Warfare's RenderHelper.preciseBlit().
     * This allows sub-pixel positioning for better alignment.
     */
    private static void preciseBlit(GuiGraphics gui, ResourceLocation texture,
            float x, float y, float uOffset, float vOffset,
            float width, float height, float textureWidth, float textureHeight) {
        float x1 = x;
        float x2 = x + width;
        float y1 = y;
        float y2 = y + height;
        float minU = uOffset / textureWidth;
        float maxU = (uOffset + width) / textureWidth;
        float minV = vOffset / textureHeight;
        float maxV = (vOffset + height) / textureHeight;

        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = gui.pose().last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.addVertex(matrix4f, x1, y1, 0).setUv(minU, minV);
        bufferbuilder.addVertex(matrix4f, x1, y2, 0).setUv(minU, maxV);
        bufferbuilder.addVertex(matrix4f, x2, y2, 0).setUv(maxU, maxV);
        bufferbuilder.addVertex(matrix4f, x2, y1, 0).setUv(maxU, minV);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
    }

    /**
     * Renders the weapon HUD overlay directly using Minecraft's native blit.
     * Called as a proper NeoForge GUI layer (like Superb Warfare's AmmoBarOverlay).
     * Uses guiGraphics.blit() exactly like Superb Warfare does.
     *
     * INSTANT DISPLAY: Like Superb Warfare, we check the held item directly
     * instead of waiting for EntityPlayerGun to spawn. This makes the HUD
     * appear instantly when switching items.
     *
     * DUAL HUD SUPPORT: Can render both handheld gun HUD (bottom-right) and
     * vehicle-mounted gun HUD (top-right) simultaneously.
     */
    public static void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        // Check if we should render (player has active gun with HUD enabled)
        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
        if (player == null || InterfaceManager.clientInterface.isChatOpen()) {
            return;
        }

        // === HANDHELD GUN HUD (bottom-right) ===
        ItemPartGun heldGunItem = null;
        IWrapperNBT heldGunData = null;

        // INSTANT CHECK: Check held item directly (like Superb Warfare's player.mainHandItem)
        // This makes HUD appear instantly without waiting for EntityPlayerGun entity to spawn
        AItemBase heldItem = player.getHeldItem();
        if (heldItem instanceof ItemPartGun) {
            ItemPartGun gunItem = (ItemPartGun) heldItem;
            // Check if it's a handheld gun with HUD enabled
            if (gunItem.definition.gun.handHeld && gunItem.definition.gunHUD != null && gunItem.definition.gunHUD.enabled) {
                heldGunItem = gunItem;
                // Get NBT data from held stack for ammo count and lastLoadedBullet
                IWrapperItemStack heldStack = player.getHeldStack();
                if (heldStack != null) {
                    heldGunData = heldStack.getData();
                }
            }
        }

        // Render handheld gun HUD at bottom-right
        if (heldGunItem != null) {
            renderHandheldGunHUD(guiGraphics, screenWidth, screenHeight, heldGunItem, heldGunData);
        }

        // === VEHICLE GUN HUD (top-right) ===
        // Check for vehicle gun - renders independently of handheld gun
        if (player.getEntityRiding() instanceof PartSeat) {
            PartSeat seat = (PartSeat) player.getEntityRiding();
            if (seat.canControlGuns && seat.activeGunItem != null && seat.gunGroups.containsKey(seat.activeGunItem)) {
                java.util.List<PartGun> guns = seat.gunGroups.get(seat.activeGunItem);
                if (guns != null && !guns.isEmpty() && seat.gunIndex < guns.size()) {
                    PartGun vehicleGun = guns.get(seat.gunIndex);
                    // Vehicle guns use legacy text HUD for now (rendered by GUIOverlay)
                    // TODO: Add modern HUD for vehicle guns at top-right position
                    renderVehicleGunHUD(guiGraphics, screenWidth, screenHeight, vehicleGun, seat);
                }
            }
        }
    }

    /**
     * Renders the handheld gun HUD at the bottom-right of the screen (Superb Warfare style).
     */
    private static void renderHandheldGunHUD(GuiGraphics guiGraphics, int screenWidth, int screenHeight,
                                              ItemPartGun heldGunItem, IWrapperNBT heldGunData) {
        JSONGunHUD hudDef = heldGunItem.definition.gunHUD;
        if (hudDef == null || !hudDef.enabled) {
            return;
        }

        int x = screenWidth;
        int y = screenHeight;
        Font font = Minecraft.getInstance().font;
        var poseStack = guiGraphics.pose();

        ItemStack gunItemStack = ((WrapperItemStack) heldGunItem.getNewStack(null)).stack;
        String gunName = heldGunItem.getItemName();
        int ammoCount = 0;
        ItemBullet loadedBullet = null;

        // Read ammo count and bullet from ItemStack NBT
        if (heldGunData != null) {
            int loadedBulletsSize = heldGunData.getInteger("loadedBulletsSize");
            for (int i = 0; i < loadedBulletsSize; i++) {
                if (heldGunData.hasKey("loadedBullet" + i)) {
                    IWrapperNBT bulletData = heldGunData.getData("loadedBullet" + i);
                    if (bulletData != null) {
                        ammoCount += bulletData.getInteger("count");
                        if (i == 0 && loadedBullet == null) {
                            AItemBase bulletItem = bulletData.getPackItem();
                            if (bulletItem instanceof ItemBullet) {
                                loadedBullet = (ItemBullet) bulletItem;
                            }
                        }
                    }
                }
            }
            // If no loaded bullets, try lastLoadedBullet
            if (loadedBullet == null && heldGunData.hasKey("lastLoadedBullet")) {
                IWrapperNBT lastBulletData = heldGunData.getData("lastLoadedBullet");
                if (lastBulletData != null) {
                    AItemBase bulletItem = lastBulletData.getPackItem();
                    if (bulletItem instanceof ItemBullet) {
                        loadedBullet = (ItemBullet) bulletItem;
                    }
                }
            }
        }

        // === GUN ICON - exactly like Superb Warfare ===
        // Superb Warfare: guiGraphics.blit(item.getGunIcon(data), x - 135, y - 40, 0f, 0f, 64, 16, 64, 16)
        // Check if custom icon texture is defined in gunHUD.weaponDisplay.iconTexture
        boolean hasCustomIcon = hudDef.weaponDisplay != null && hudDef.weaponDisplay.iconTexture != null && !hudDef.weaponDisplay.iconTexture.isEmpty();

        if (hasCustomIcon) {
            // Use custom icon texture
            String iconPath = hudDef.weaponDisplay.iconTexture;

            // Parse "packid:path" format into ResourceLocation
            ResourceLocation iconTexture;
            if (iconPath.contains(":")) {
                String[] parts = iconPath.split(":", 2);
                iconTexture = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
            } else {
                iconTexture = ResourceLocation.fromNamespaceAndPath("mts", iconPath);
            }
            // Render icon texture using preciseBlit for consistent rendering
            // Uses 64x16 render size matching a typical gun icon
            preciseBlit(
                guiGraphics,
                iconTexture,
                x - 135f,
                y - 40f,
                0f, 0f,
                64f, 16f,    // render size
                64f, 16f     // texture size (assuming 64x16 icon texture)
            );
        } else if (gunItemStack != null) {
            // Fallback: render item texture
            poseStack.pushPose();
            poseStack.translate(x - 135, y - 40, 0);
            guiGraphics.renderFakeItem(gunItemStack, 24, 0);  // Center the 16x16 item in the 64x16 space
            poseStack.popPose();
        }

        // === AMMO COUNT (large 1.5x scale, white with shadow) - exactly like Superb Warfare ===
        // Superb Warfare: poseStack.scale(1.5f), then drawString at (x / 1.5f - 64 / 1.5f, gunAmmoY / 1.5f)
        // Superb Warfare gunAmmoY = y + 5 - 48 = y - 43
        String ammoString = String.valueOf(ammoCount);
        float gunAmmoY = y + 5 - 48;  // y - 43, exactly like Superb Warfare

        poseStack.pushPose();
        poseStack.scale(1.5f, 1.5f, 1f);
        // Use float coordinates like Superb Warfare (no int cast)
        guiGraphics.drawString(font, ammoString, x / 1.5f - 64 / 1.5f, gunAmmoY / 1.5f, 0xFFFFFF, true);
        poseStack.popPose();

        // === BACKUP/RESERVE AMMO (normal scale, gray with shadow) ===
        // Position: x - 64, y - 30
        guiGraphics.drawString(font, "\u221E", x - 64, y - 30, 0xCCCCCC, true);

        // === GUN NAME and AMMO TYPE NAME (0.9x scale, centered) - exactly like Superb Warfare ===
        String ammoName = loadedBullet != null ? loadedBullet.getItemName() : "";

        poseStack.pushPose();
        poseStack.scale(0.9f, 0.9f, 1f);

        // Gun name: centered around x-100, y-60
        // Superb Warfare formula: x / 0.9f - (100 + font.width(gunName) / 2f) / 0.9f
        float gunNameX = x / 0.9f - (100 + font.width(gunName) / 2f) / 0.9f;
        float gunNameY = y / 0.9f - 60 / 0.9f;
        guiGraphics.drawString(font, gunName, gunNameX, gunNameY, 0xFFFFFF, true);

        // Ammo type name: centered around x-100, y-51, tan color 0xC8A679
        if (!ammoName.isEmpty()) {
            float ammoNameX = x / 0.9f - (100 + font.width(ammoName) / 2f) / 0.9f;
            float ammoNameY = y / 0.9f - 51 / 0.9f;
            guiGraphics.drawString(font, ammoName, ammoNameX, ammoNameY, 0xC8A679, true);
        }

        poseStack.popPose();

        // === AMMO TYPE INDICATOR (like Superb Warfare) ===
        // Shows: [ bullet_item ] with count
        // Superb Warfare uses preciseBlit with float position: (x - 62, y - 20.5f)

        // Display the loaded bullet (from PartGun or NBT data)
        // Show for ALL guns with capacity > 0 - clipless guns still have loaded bullets,
        // they just load directly without magazines (e.g., flare gun, confetti gun)
        boolean hasAmmoCapacity = heldGunItem.definition.gun.capacity > 0;
        if (loadedBullet != null && hasAmmoCapacity) {
            // Render ammo_stack texture (the brackets) using preciseBlit for sub-pixel accuracy
            // Superb Warfare: RenderHelper.preciseBlit(gui, AMMO_STACK, (x - 62).toFloat(), y - 20.5f, 0f, 0f, 24f, 8.5f, 24f, 24f)
            preciseBlit(
                guiGraphics,
                TEXTURE_AMMO_STACK,
                x - 62f,
                y - 20.5f,    // Note: 20.5f, not 21 - this is the key difference!
                0f, 0f,
                24f, 8.5f,    // render size (24 wide, 8.5 tall for brackets)
                24f, 24f      // texture size
            );

            // Check if bullet has custom hudIcon defined
            String hudIconPath = loadedBullet.definition.bullet.hudIcon;
            if (hudIconPath != null && !hudIconPath.isEmpty()) {
                // Use custom HUD icon texture
                // Format: "packid:path/to/texture" -> ResourceLocation
                String[] parts = hudIconPath.split(":", 2);
                String namespace = parts.length > 1 ? parts[0] : "mts";
                String path = parts.length > 1 ? parts[1] : hudIconPath;
                // Add .png extension if not present
                if (!path.endsWith(".png")) {
                    path = path + ".png";
                }
                ResourceLocation hudIconTexture = ResourceLocation.fromNamespaceAndPath(namespace, path);

                // Render custom icon at same position as item would be rendered
                // Position matches the item rendering: x-57 + 3*0.75 + 1.75*0.75 ≈ x-53.5, y-21 + (-1)*0.75 ≈ y-21.75
                poseStack.pushPose();
                preciseBlit(
                    guiGraphics,
                    hudIconTexture,
                    x - 54f,
                    y - 22f,
                    0f, 0f,
                    12f, 12f,    // render size (scaled to match 16x16 item at 0.75x)
                    12f, 12f     // texture size (assuming 12x12 icon texture)
                );
                poseStack.popPose();
            } else {
                // Render the bullet item inside the brackets - EXACTLY like Superb Warfare
                // Superb Warfare code:
                //   poseStack.translate((x - 57).toFloat(), (y - 21).toFloat(), 0f)
                //   poseStack.scale(0.75f, 0.75f, 1f)
                //   poseStack.translate(1.75f, 0f, 0f)
                //   guiGraphics.renderFakeItem(ammoStack, 3, -1)
                poseStack.pushPose();
                poseStack.translate(x - 57, y - 21, 0);
                poseStack.scale(0.75f, 0.75f, 1f);
                poseStack.translate(1.75f, 0f, 0f);

                // Get the MC ItemStack for the bullet
                ItemStack bulletStack = ((WrapperItemStack) loadedBullet.getNewStack(null)).stack;
                guiGraphics.renderFakeItem(bulletStack, 3, -1);

                poseStack.popPose();
            }
            // Note: In Superb Warfare, a number (stack count) appears next to brackets
            // MTS doesn't need this since backup ammo (∞) is shown separately by GUIWeaponHUD
        }

        // === FIRE MODE SECTION ===
        // Supports 3 fire modes: semi, auto, burst
        // With backward compatibility for legacy isSemiAuto boolean
        // Runtime fire mode switching is now implemented!

        // Render keybind [N] at EXACT Superb Warfare position: x - 111.5f, y - 20
        guiGraphics.drawString(font, "[N]", x - 111.5f, (float)(y - 20), 0xFFFFFF, false);

        // Get current fire mode from NBT (like Superb Warfare reads from ItemStack)
        // NBT is kept fresh by EntityPlayerGun saving every client tick
        String currentFireMode = "auto";  // default
        if (heldGunData != null && heldGunData.hasKey("currentFireModeIndex")) {
            int fireModeIndex = heldGunData.getInteger("currentFireModeIndex");
            // Get fire modes from gun definition
            if (heldGunItem.definition.gun.fireModes != null && !heldGunItem.definition.gun.fireModes.isEmpty()) {
                if (fireModeIndex >= 0 && fireModeIndex < heldGunItem.definition.gun.fireModes.size()) {
                    currentFireMode = heldGunItem.definition.gun.fireModes.get(fireModeIndex);
                } else {
                    currentFireMode = heldGunItem.definition.gun.fireModes.get(0);
                }
            } else {
                // Backward compatibility: use legacy isSemiAuto boolean
                currentFireMode = heldGunItem.definition.gun.isSemiAuto ? "semi" : "auto";
            }
        } else {
            // No NBT data yet, use gun definition defaults
            if (heldGunItem.definition.gun.fireModes != null && !heldGunItem.definition.gun.fireModes.isEmpty()) {
                currentFireMode = heldGunItem.definition.gun.fireModes.get(0);
            } else {
                currentFireMode = heldGunItem.definition.gun.isSemiAuto ? "semi" : "auto";
            }
        }

        // Select fire mode texture based on current mode
        ResourceLocation fireModeTexture;
        switch (currentFireMode.toLowerCase()) {
            case "semi":
                fireModeTexture = TEXTURE_SEMI;
                break;
            case "burst":
                fireModeTexture = TEXTURE_BURST;
                break;
            case "auto":
            default:
                fireModeTexture = TEXTURE_AUTO;
                break;
        }

        // Render fire mode icon using preciseBlit for exact Superb Warfare match
        // Superb Warfare: guiGraphics.blit(fireMode, x - 95, y - 21, 0f, 0f, 8, 8, 8, 8)
        preciseBlit(
            guiGraphics,
            fireModeTexture,
            x - 95f,
            y - 21f,
            0f, 0f,    // UV offset
            8f, 8f,    // render size
            8f, 8f     // texture size
        );

        // Render line using preciseBlit
        // Superb Warfare: guiGraphics.blit(LINE, x - 95, y - 16, 0f, 0f, 8, 8, 8, 8)
        preciseBlit(
            guiGraphics,
            TEXTURE_LINE,
            x - 95f,
            y - 16f,
            0f, 0f,
            8f, 8f,
            8f, 8f
        );
    }

    /**
     * Renders the vehicle-mounted gun HUD at the top-right of the screen.
     * This is a simplified HUD showing gun name, ammo count, and gun index.
     */
    private static void renderVehicleGunHUD(GuiGraphics guiGraphics, int screenWidth, int screenHeight,
                                             PartGun vehicleGun, PartSeat seat) {
        // Check if gun has modern HUD enabled - if not, let legacy GUIOverlay handle it
        if (vehicleGun.definition.gunHUD == null || !vehicleGun.definition.gunHUD.enabled) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        var poseStack = guiGraphics.pose();

        // Position at top-right of screen
        int x = screenWidth;
        int y = 10;  // 10 pixels from top

        // Get gun info
        String gunName = vehicleGun.cachedItem.getItemName();
        String ammoText = vehicleGun.getBulletText();
        int gunIndex = seat.gunIndex + 1;
        boolean fireSolo = vehicleGun.cachedItem.definition.gun.fireSolo;

        // === GUN NAME (top line) ===
        poseStack.pushPose();
        poseStack.scale(0.9f, 0.9f, 1f);
        float gunNameX = x / 0.9f - (10 + font.width(gunName)) / 0.9f;
        float gunNameY = y / 0.9f;
        guiGraphics.drawString(font, gunName, gunNameX, gunNameY, 0xFFFFFF, true);
        poseStack.popPose();

        // === GUN INDEX (if fireSolo) ===
        if (fireSolo) {
            String indexText = "[" + gunIndex + "]";
            guiGraphics.drawString(font, indexText, x - 10 - font.width(indexText), y + 12, 0xAAAAAA, true);
        }

        // === AMMO COUNT (larger, below name) ===
        poseStack.pushPose();
        poseStack.scale(1.2f, 1.2f, 1f);
        float ammoX = x / 1.2f - (10 + font.width(ammoText)) / 1.2f;
        float ammoY = (y + 22) / 1.2f;
        guiGraphics.drawString(font, ammoText, ammoX, ammoY, 0xFFFF00, true);  // Yellow for visibility
        poseStack.popPose();
    }
}
