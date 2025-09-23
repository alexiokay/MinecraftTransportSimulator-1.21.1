# LevelRendererMixin Migration: 1.20.1 to 1.21.1 Complete Analysis

## Overview
This document details the complete migration of the LevelRendererMixin approach from Minecraft 1.20.1 to NeoForge 1.21.1, including the architectural changes, API adaptations, and final working solution.

## Background: The Migration Challenge

### Initial Problem
- Original mod worked with LevelRendererMixin in 1.20.1
- Migration to 1.21.1 NeoForge initially used BuilderEntityRenderForwarder as a workaround
- Goal: Restore proper LevelRendererMixin approach for better architecture

### Why LevelRendererMixin is Superior
1. **Direct Integration**: Integrates directly into `LevelRenderer.renderLevel()` - the core world rendering method
2. **Proper Timing**: Renders at exact right moment in level rendering pipeline (after main geometry, before UI)
3. **Better Performance**: No overhead of maintaining fake entities
4. **Robust Architecture**: Works regardless of player state, world conditions, or entity limitations
5. **NeoForge Best Practices**: Uses proper Sponge Mixin system as intended

## Key API Changes Between Versions

### 1.20.1 Method Signature (Original)
```java
@Inject(method = "renderLevel", at = @At(value = "TAIL"))
public void inject_renderLevelBlended(
    PoseStack pMatrixStack,        // ✅ Directly provided
    float pPartialTicks,           // ✅ Directly provided
    long pFinishTimeNano,
    boolean pDrawBlockOutline,
    Camera pCamera,
    GameRenderer pGameRenderer,
    LightTexture pLightmap,        // ✅ LightTexture parameter existed
    Matrix4f pProjection,          // ✅ Single projection matrix
    CallbackInfo ci
) {
    // PoseStack and timing ready to use
    InterfaceRender.matrixStack = pMatrixStack;
    InterfaceRender.doRenderCall(true, pPartialTicks);
}
```

### 1.21.1 Method Signature (Final Working)
```java
@Inject(method = "renderLevel", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V",
    shift = At.Shift.AFTER))
public void inject_renderLevelBlended(
    DeltaTracker pDeltaTracker,    // 🔄 Replaced float + long timing
    boolean pDrawBlockOutline,
    Camera pCamera,
    GameRenderer pGameRenderer,
    LightTexture pLightmap,        // ❌ "renderLevel no longer takes in the LightTexture"
    Matrix4f pModelViewMatrix,     // 🔄 Two matrices instead of one
    Matrix4f pProjection,
    CallbackInfo ci
) {
    // Must create PoseStack manually and apply transformations
    PoseStack pMatrixStack = new PoseStack();
    Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    pMatrixStack.translate(-position.x, -position.y, -position.z);

    // Extract timing from DeltaTracker
    float pPartialTicks = pDeltaTracker.getGameTimeDeltaPartialTick(false);

    InterfaceRender.matrixStack = pMatrixStack;
    InterfaceRender.doRenderCall(true, pPartialTicks);
}
```

## Critical Changes Required

### 1. DeltaTracker Introduction
**Official MC 1.21 Change**: `DeltaTracker` "replaces most instances passing around the delta time or current partial tick"

**Impact**: Must extract timing manually:
```java
float pPartialTicks = pDeltaTracker.getGameTimeDeltaPartialTick(false);
```

### 2. PoseStack Parameter Removal
**Official MC 1.21 Change**: Core rendering methods no longer provide PoseStack directly

**Impact**: Must create and configure PoseStack manually:
```java
PoseStack pMatrixStack = new PoseStack();
// Apply camera transformation that 1.20.1 had automatically
pMatrixStack.translate(-position.x, -position.y, -position.z);
```

### 3. Injection Point Precision
**1.20.1**: Simple end-of-method injection `@At(value = "TAIL")`
**1.21.1**: Precise injection after specific method call for proper timing

### 4. Matrix Parameters
**1.20.1**: Single `Matrix4f projectionMatrix`
**1.21.1**: Two matrices: `Matrix4f pModelViewMatrix, Matrix4f pProjection`

## Technical Debugging Process

### Phase 1: Method Signature Mismatch
- **Problem**: Crash due to 1.20.1 signatures in 1.21.1
- **Solution**: Updated to DeltaTracker-based signature

### Phase 2: Missing Textures
- **Problem**: Objects rendered but no textures visible
- **Root Cause**: Fresh PoseStack without proper world transformation
- **Solution**: Manual camera offset translation

### Phase 3: Injection Timing
- **Problem**: Textures still not rendering properly
- **Root Cause**: Injecting at wrong point in rendering pipeline
- **Solution**: Inject after `MultiBufferSource.BufferSource.endBatch()` call

## Supporting Mixins Required

### Essential for 1.21.1 Functionality
```json
"client": [
    "client.CameraMixin",           // Camera transformations
    "client.HumanoidModelMixin",    // Player model in vehicles
    "client.LivingEntityRendererMixin", // Player rendering
    "client.LevelRendererMixin",    // Main rendering integration
    "client.GuiGraphicsMixin",      // GUI rendering fixes
    "client.MinecraftMixin",        // Core game integration
    "client.ModelBakeryMixin"       // Model loading integration
]
```

## What We Completely Replaced

### Disabled BuilderEntityRenderForwarder
```java
// OLD: Hacky entity-based rendering
@Override
public void render(BuilderEntityRenderForwarder builder, ...) {
    // Rendering via fake entity following player
}

// NEW: Disabled - rendering handled by LevelRendererMixin
@Override
public void render(BuilderEntityRenderForwarder builder, ...) {
    // Rendering now handled by LevelRendererMixin - BuilderEntityRenderForwarder disabled
}
```

## Final Architecture Comparison

### 1.20.1 → 1.21.1 Rendering Flow
**Before**: `Player Entity` → `BuilderEntityRenderForwarder` → `doRenderCall()`
**After**: `LevelRenderer.renderLevel()` → `LevelRendererMixin` → `doRenderCall()`

### Advantages of Final Implementation
1. **Cleaner**: No fake entities, direct rendering integration
2. **More Performant**: No entity overhead
3. **More Robust**: Independent of entity system changes
4. **Future-Proof**: Follows NeoForge best practices

## Additional Features Confirmed Working

### Rain/Weather Integration
- **Rain Blocking**: `inject_renderSnowAndRain` properly blocks rain from vehicles
- **Dirty Car Effects**: Confirmed working during storms and weather events
- **Height Map Adjustment**: Vehicles properly affect precipitation calculations

### Visual Effects
- **Texture Rendering**: All vehicle and mod object textures render correctly
- **Shader Integration**: Lighting and shaders work properly
- **Translucent Support**: Proper translucent rendering without depth buffer issues
- **World Stability**: Objects stable in world space, not floating with camera

## Key Lessons Learned

### 1. Manual PoseStack Creation is REQUIRED
This is **not optional** - it's a forced change due to MC 1.21 API changes:
- Mojang removed PoseStack parameter from renderLevel
- All mods targeting MC 1.21+ must handle this manually
- No way to get PoseStack "automatically" like in 1.20.1

### 2. DeltaTracker Adaptation is MANDATORY
- Part of core MC 1.21 vanilla changes affecting all modding platforms
- Replaces individual timing parameters in most rendering methods
- Requires manual extraction: `pDeltaTracker.getGameTimeDeltaPartialTick(false)`

### 3. Injection Point Precision Matters
- 1.21.1 has more explicit render pass management
- Simple `@At("TAIL")` no longer sufficient
- Need precise injection after specific rendering operations

## Migration Summary

**Result**: Successfully restored LevelRendererMixin approach with proper 1.21.1 compatibility
**Performance**: Eliminated fake entity overhead, improved rendering efficiency
**Architecture**: Cleaner, more maintainable, follows NeoForge best practices
**Compatibility**: Full feature parity with 1.20.1 behavior

This migration demonstrates that while the API changes were significant, the core rendering functionality could be fully preserved with proper adaptation to the new 1.21.1 rendering architecture.