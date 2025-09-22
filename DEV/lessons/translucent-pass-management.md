# Translucent Pass Management: 1.20.1 vs 1.21.1

## Why Manual Translucent Pass Management is Required in 1.21.1

### The Problem

In NeoForge 1.21.1, we discovered that vehicle lights and particles were not rendering despite the core rendering engine working correctly. The issue was traced to **missing translucent render passes** in the entity renderer registration.

## Two Different Rendering Contexts

### 1. World-Level Rendering (Working in Both Versions)

**LevelRendererMixin** handles world-level translucent rendering automatically:

```java
// Both 1.20.1 and 1.21.1 have this working correctly
@Inject(method = "renderLevel", at = @At(value = "TAIL"))
public void inject_renderLevelBlended(...) {
    if (ConfigSystem.settings.general.forceRenderLastSolid.value) {
        InterfaceRender.doRenderCall(false, pPartialTicks);  // Solid pass
    }
    InterfaceRender.doRenderCall(true, pPartialTicks);       // Translucent pass ✅
}
```

This handles vehicles and parts when rendered in the world context.

### 2. Entity Renderer Registration (Broken in 1.21.1)

**Entity renderer registration** handles rendering when entities are in specific contexts (like when following a player, specific camera angles, etc.):

**1.20.1 (Working):**
```java
// In BuilderEntityRenderForwarder
public void render(...) {
    matrixStack = stack;
    renderBuffer = buffer;
    doRenderCall(false, partialTicks);  // Only solid pass, but somehow translucent worked
}
```

**1.21.1 (Broken → Fixed):**
```java
// In BuilderEntityRenderForwarder
public void render(...) {
    matrixStack = stack;
    renderBuffer = buffer;
    // BEFORE FIX: Only solid pass
    doRenderCall(false, partialTicks);

    // AFTER FIX: Added missing translucent pass
    doRenderCall(true, partialTicks);   // ✅ Manual translucent pass
}
```

## Why This Difference Exists

### 1.20.1 Behavior
In Forge 1.20.1, the entity rendering system had **implicit translucent handling** or different render pass management that automatically included translucent elements even when only the solid pass was explicitly called.

### 1.21.1 Behavior
In NeoForge 1.21.1, the rendering system became **more explicit** - you must manually call both render passes:
- `doRenderCall(false, partialTicks)` - Solid objects
- `doRenderCall(true, partialTicks)` - Translucent objects (lights, particles, glass, etc.)

## Impact and Solution

### What Wasn't Working
- Vehicle headlights, taillights, indicators
- Fire extinguisher particles
- Any translucent/emissive elements in entity renderer context

### The Fix
```java
// Added explicit translucent pass call
doRenderCall(false, partialTicks);  // Solid pass
doRenderCall(true, partialTicks);   // Translucent pass - REQUIRED in 1.21.1
```

### Why Manual Management is Needed

1. **API Changes**: NeoForge 1.21.1 changed how render passes are handled
2. **Explicit Control**: More granular control over when each pass executes
3. **Performance**: Better separation of solid vs translucent rendering
4. **Correctness**: Ensures proper depth sorting and alpha blending

## Technical Details

### Render Pass Separation

**Solid Pass (`false`):**
- Opaque geometry
- No alpha blending
- Depth buffer writes enabled
- Faster rendering

**Translucent Pass (`true`):**
- Transparent/translucent geometry
- Alpha blending enabled
- Depth buffer reads only (no writes)
- Slower rendering, requires sorting

### Why Both Are Needed

**Without Solid Pass:**
- Main vehicle geometry wouldn't render
- No base model to attach lights to

**Without Translucent Pass (Our Bug):**
- Lights appear black/invisible
- Particles don't render
- Glass/transparent parts missing
- Emissive textures don't glow

## Comparison: Automatic vs Manual

### 1.20.1 - "Automatic" (Hidden Complexity)
```java
doRenderCall(false, partialTicks);  // Somehow handled translucent too
```
- Simpler code
- Hidden behavior
- Less control
- Harder to debug

### 1.21.1 - "Manual" (Explicit Control)
```java
doRenderCall(false, partialTicks);   // Solid pass
doRenderCall(true, partialTicks);    // Translucent pass
```
- More verbose code
- Explicit behavior
- Full control over render timing
- Easier to debug and optimize

## Best Practices for 1.21.1

### Always Call Both Passes
```java
// ✅ Correct - Both passes
doRenderCall(false, partialTicks);   // Solid
doRenderCall(true, partialTicks);    // Translucent

// ❌ Wrong - Missing translucent
doRenderCall(false, partialTicks);   // Lights won't work
```

### Order Matters
```java
// ✅ Correct order
doRenderCall(false, partialTicks);   // Solid first
doRenderCall(true, partialTicks);    // Translucent second

// ❌ Wrong order - depth issues
doRenderCall(true, partialTicks);    // Translucent first
doRenderCall(false, partialTicks);   // Solid second
```

### Context Awareness
- **World rendering**: LevelRendererMixin handles automatically
- **Entity rendering**: Must manually call both passes
- **GUI rendering**: Different system entirely

## Conclusion

The "manual" translucent pass management in 1.21.1 isn't a regression - it's a more explicit, controllable system. While it requires more code, it provides better performance, debugging capabilities, and correctness. The key is understanding **when** each context applies and ensuring both render passes are called in entity renderer registration.

This change reflects NeoForge's evolution toward more explicit, performance-oriented rendering APIs that give developers better control over the rendering pipeline.