# Native Rendering Implementation Guide

This guide explains how to implement the native text/item rendering methods and the below-vanilla rendering system in `InterfaceRender.java` for different Minecraft versions. These are required for the weapon HUD system.

## Overview

The weapon HUD uses two key features:
1. **Native font rendering** - Minecraft's built-in font instead of MTS's custom font
2. **Below-vanilla rendering** - HUD renders below vanilla elements (health, hunger, hotbar)

### Interface Methods Required

```java
// In IInterfaceRender.java (mccore)
void renderNativeText(String text, float x, float y, int color, float scale, boolean shadow, boolean rightAligned);
float getNativeTextWidth(String text);
void renderNativeItem(IWrapperItemStack stack, float x, float y, float scale);
```

### AGUIBase Method

```java
// In AGUIBase.java (mccore) - already implemented
public boolean renderBelowVanilla() {
    return false;  // Override to return true for below-vanilla rendering
}
```

---

## Required Setup

Each version needs:
1. A `GuiGraphics` (or equivalent) context stored during GUI rendering
2. Below-vanilla overlay registration (platform-specific)
3. Skip below-vanilla GUIs in normal `renderGUI()`

Add this field to your `InterfaceRender.java`:

```java
private static GuiGraphics currentGuiGraphics;  // Name varies by version
```

---

## NeoForge 1.21.1 Implementation

### Native Rendering Methods

```java
@Override
public void renderNativeText(String text, float x, float y, int color, float scale, boolean shadow, boolean rightAligned) {
    if (currentGuiGraphics == null || text == null || text.isEmpty()) {
        return;
    }
    net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
    var poseStack = currentGuiGraphics.pose();

    poseStack.pushPose();
    // MTS GUI renders with inverted Y-axis (scale 1, -1, 1), so we need to:
    // 1. Undo the Y inversion by scaling Y by -1
    // 2. Negate Y position since MTS uses negative Y for screen positions
    poseStack.scale(scale, -scale, 1f);

    float scaledX = x / scale;
    float scaledY = -y / scale;  // Negate Y to convert from MTS coords

    if (rightAligned) {
        scaledX -= font.width(text);
    }

    currentGuiGraphics.drawString(font, text, scaledX, scaledY, color, shadow);
    poseStack.popPose();
}

@Override
public float getNativeTextWidth(String text) {
    if (text == null || text.isEmpty()) {
        return 0;
    }
    return Minecraft.getInstance().font.width(text);
}

@Override
public void renderNativeItem(IWrapperItemStack stack, float x, float y, float scale) {
    if (currentGuiGraphics == null || stack == null) {
        return;
    }
    var poseStack = currentGuiGraphics.pose();

    poseStack.pushPose();
    poseStack.translate(x, y, 0);
    poseStack.scale(scale, scale, 1f);

    net.minecraft.world.item.ItemStack mcStack = ((WrapperItemStack) stack).stack;
    currentGuiGraphics.renderFakeItem(mcStack, 0, 0);

    poseStack.popPose();
}
```

### Below-Vanilla Overlay Registration (ClientRenderingEvents.java)

```java
public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
    // Register overlay that renders below all vanilla HUD elements
    event.registerBelowAll(
        ResourceLocation.fromNamespaceAndPath("mts", "below_vanilla_overlay"),
        (guiGraphics, partialTick) -> {
            // Set the GuiGraphics context for native rendering
            InterfaceRender.setCurrentGuiGraphics(guiGraphics);

            // Render all GUIs that want below-vanilla timing
            float partialTicks = partialTick.getGameTimeDeltaPartialTick(true);
            int screenWidth = guiGraphics.guiWidth();
            int screenHeight = guiGraphics.guiHeight();

            for (AGUIBase gui : AGUIBase.activeGUIs) {
                if (gui.renderBelowVanilla() && !gui.capturesPlayer()) {
                    if (gui.components.isEmpty() || gui.hasScreenSizeChanged(screenWidth, screenHeight)) {
                        gui.setupComponentsInit(screenWidth, screenHeight);
                    }
                    InterfaceRender.renderBelowVanillaGUI(guiGraphics, gui, 0, 0, screenWidth, screenHeight, partialTicks);
                }
            }

            InterfaceRender.clearCurrentGuiGraphics();
        }
    );
}
```

### Helper Methods (InterfaceRender.java)

```java
public static void setCurrentGuiGraphics(GuiGraphics graphics) {
    currentGuiGraphics = graphics;
}

public static void clearCurrentGuiGraphics() {
    currentGuiGraphics = null;
}

public static void renderBelowVanillaGUI(GuiGraphics mcGUI, AGUIBase gui, int mouseX, int mouseY,
                                          int screenWidth, int screenHeight, float partialTicks) {
    matrixStack = mcGUI.pose();
    matrixStack.pushPose();
    renderingGUI = true;
    MultiBufferSource.BufferSource guiBuffer = mcGUI.bufferSource();
    renderBuffer = guiBuffer;

    matrixStack.scale(1.0F, -1.0F, 1.0F);
    matrixStack.pushPose();
    matrixStack.translate(0, 0, -500);

    gui.render(mouseX, mouseY, false, partialTicks);
    guiBuffer.endBatch();

    RenderSystem.enableBlend();
    gui.render(mouseX, mouseY, true, partialTicks);
    guiBuffer.endBatch();
    RenderSystem.disableBlend();

    // Handle item stack rendering...
    matrixStack.scale(1.0F, -1.0F, 1.0F);
    // ... (item rendering code)

    matrixStack.popPose();
    matrixStack.popPose();
    renderingGUI = false;
}
```

### Skip Below-Vanilla GUIs in renderGUI()

```java
protected static void renderGUI(GuiGraphics mcGUI, ...) {
    // ...
    for (AGUIBase gui : AGUIBase.activeGUIs) {
        if (gui.capturesPlayer()) continue;

        // Skip GUIs that render below vanilla - they're already rendered
        if (gui.renderBelowVanilla()) continue;

        // ... render normally
    }
}
```

**Required imports:**
```java
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import minecrafttransportsimulator.guis.components.AGUIBase;
```

---

## Forge 1.20.x Implementation

### Native Rendering (same as 1.21.1)
```java
// Same implementation as NeoForge 1.21.1
// Use currentGuiGraphics.drawString() and currentGuiGraphics.renderItem()
```

### Below-Vanilla Registration
```java
// Use RegisterGuiOverlaysEvent instead of RegisterGuiLayersEvent
@SubscribeEvent
public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
    event.registerBelowAll("below_vanilla_overlay", (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        InterfaceRender.setCurrentGuiGraphics(guiGraphics);
        for (AGUIBase mtsGui : AGUIBase.activeGUIs) {
            if (mtsGui.renderBelowVanilla() && !mtsGui.capturesPlayer()) {
                // render...
            }
        }
        InterfaceRender.clearCurrentGuiGraphics();
    });
}
```

---

## Forge 1.18.x / 1.19.x Implementation

These versions use `PoseStack` directly instead of `GuiGraphics`.

### Native Rendering
```java
private static PoseStack currentPoseStack;

@Override
public void renderNativeText(String text, float x, float y, int color, float scale, boolean shadow, boolean rightAligned) {
    if (currentPoseStack == null || text == null || text.isEmpty()) {
        return;
    }
    Font font = Minecraft.getInstance().font;

    currentPoseStack.pushPose();
    currentPoseStack.scale(scale, -scale, 1f);

    float scaledX = x / scale;
    float scaledY = -y / scale;

    if (rightAligned) {
        scaledX -= font.width(text);
    }

    if (shadow) {
        font.drawShadow(currentPoseStack, text, scaledX, scaledY, color);
    } else {
        font.draw(currentPoseStack, text, scaledX, scaledY, color);
    }
    currentPoseStack.popPose();
}
```

### Below-Vanilla Registration
```java
// Use RenderGameOverlayEvent.Pre with ElementType checking
@SubscribeEvent
public static void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
    if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
        // Render below-vanilla GUIs here (renders before vanilla HUD)
        for (AGUIBase gui : AGUIBase.activeGUIs) {
            if (gui.renderBelowVanilla() && !gui.capturesPlayer()) {
                // render...
            }
        }
    }
}
```

---

## Forge 1.16.x Implementation

```java
private static MatrixStack currentMatrixStack;

@Override
public void renderNativeText(String text, float x, float y, int color, float scale, boolean shadow, boolean rightAligned) {
    if (currentMatrixStack == null || text == null || text.isEmpty()) {
        return;
    }
    FontRenderer font = Minecraft.getInstance().font;

    currentMatrixStack.pushPose();
    currentMatrixStack.scale(scale, -scale, 1f);

    float scaledX = x / scale;
    float scaledY = -y / scale;

    if (rightAligned) {
        scaledX -= font.width(text);
    }

    if (shadow) {
        font.drawShadow(currentMatrixStack, text, scaledX, scaledY, color);
    } else {
        font.draw(currentMatrixStack, text, scaledX, scaledY, color);
    }
    currentMatrixStack.popPose();
}
```

---

## Forge 1.12.x Implementation

1.12.x uses the legacy rendering system without matrix stacks.

```java
@Override
public void renderNativeText(String text, float x, float y, int color, float scale, boolean shadow, boolean rightAligned) {
    if (text == null || text.isEmpty()) {
        return;
    }
    FontRenderer font = Minecraft.getMinecraft().fontRenderer;

    GlStateManager.pushMatrix();
    GlStateManager.scale(scale, -scale, 1f);

    float scaledX = x / scale;
    float scaledY = -y / scale;

    if (rightAligned) {
        scaledX -= font.getStringWidth(text);
    }

    if (shadow) {
        font.drawStringWithShadow(text, scaledX, scaledY, color);
    } else {
        font.drawString(text, (int)scaledX, (int)scaledY, color);
    }
    GlStateManager.popMatrix();
}

@Override
public float getNativeTextWidth(String text) {
    if (text == null || text.isEmpty()) {
        return 0;
    }
    return Minecraft.getMinecraft().fontRenderer.getStringWidth(text);
}

@Override
public void renderNativeItem(IWrapperItemStack stack, float x, float y, float scale) {
    if (stack == null) {
        return;
    }

    GlStateManager.pushMatrix();
    GlStateManager.translate(x, y, 0);
    GlStateManager.scale(scale, scale, 1f);

    ItemStack mcStack = ((WrapperItemStack) stack).stack;
    Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(mcStack, 0, 0);

    GlStateManager.popMatrix();
}
```

---

## Version Comparison Table

| Feature | 1.21.1 NeoForge | 1.20.x Forge | 1.18-19.x | 1.16.x | 1.12.x |
|---------|-----------------|--------------|-----------|--------|--------|
| Graphics Context | `GuiGraphics` | `GuiGraphics` | `PoseStack` | `MatrixStack` | `GlStateManager` |
| Font Class | `Font` | `Font` | `Font` | `FontRenderer` | `FontRenderer` |
| Draw Text | `drawString()` | `drawString()` | `draw()`/`drawShadow()` | `draw()`/`drawShadow()` | `drawString()` |
| Text Width | `font.width()` | `font.width()` | `font.width()` | `font.width()` | `getStringWidth()` |
| Item Render | `renderFakeItem()` | `renderItem()` | `renderGuiItem()` | `renderGuiItem()` | `renderItemIntoGUI()` |
| Below-All Event | `RegisterGuiLayersEvent` | `RegisterGuiOverlaysEvent` | `RenderGameOverlayEvent` | `RenderGameOverlayEvent` | `RenderGameOverlayEvent` |

---

## Testing

After implementing, test with:
1. Hold a handheld gun with `gunHUD.enabled: true` in its JSON
2. The weapon HUD should appear in bottom-right corner
3. Text should render with Minecraft's native font
4. **Weapon HUD should render BELOW vanilla health/hunger bars**
5. Fire mode icons and ammo counts should display correctly

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Text not showing | Check `currentGuiGraphics` is set in `renderGUI()` |
| Text upside down | Check Y-axis scaling: `poseStack.scale(scale, -scale, 1f)` |
| Text position wrong | MTS uses inverted Y coords, negate Y: `-y / scale` |
| HUD renders on top | Check `registerBelowAll()` is called and GUI returns `renderBelowVanilla() == true` |
| HUD renders twice | Ensure `renderGUI()` skips GUIs with `renderBelowVanilla() == true` |
| Crash on older version | Check import paths match your MC version |

## Fallback Behavior

If a platform doesn't implement `registerBelowAll()` or equivalent:
- GUIs with `renderBelowVanilla() == true` will render normally in `renderGUI()`
- They'll appear ON TOP of vanilla HUD instead of below
- This is acceptable fallback - the HUD still works, just different layering
