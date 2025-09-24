# Iris Shaders Integration - Current Status

## BREAKTHROUGH: Lights ARE Rendering in Shader Packs!

**Date:** September 24, 2025
**Status:** PROGRESS - Lights render but are black, Fire extinguisher particles work correctly

## Current State

### ✅ WORKING
- **Fire extinguisher particles** work correctly with shaders
- **Vehicle headlights render** with Complementary shaders (geometry is there)
- **Iris detection system** properly identifies when shader packs are active
- **Fallback rendering pathway** is implemented and active

### ❌ BROKEN
- **Vehicle headlights appear BLACK** instead of bright white/yellow
- **Headlight textures** may not be loading correctly through gbuffers pipeline
- **Color values** are being lost somewhere in the Iris rendering chain

## Technical Implementation

### What Made Lights Render in Shader Packs

#### 🚫 **What We STOPPED Doing (The Key Fix)**

**Before:** MTS was using **custom OpenGL lighting** that bypassed Minecraft's standard rendering:
- Custom shader programs (`mts_entity_lights`, `mts_entity_lights_iris`)
- Direct OpenGL blending calls
- Custom "True Lighting" system that ignored the lightmap

**The Problem:** When Iris shader packs are active, they **completely replace** the rendering pipeline and **ignore all custom shaders**. So our custom lighting was invisible.

#### ✅ **What We STARTED Doing (The Solution)**

**1. Shader Selection Fix** (`InterfaceRender.java:789-797`):
```java
if (IrisLightingIntegration.shouldUseFallbackRendering()) {
    // Iris shader pack is active - use standard Minecraft shaders only
    // Per Iris documentation: custom shaders are ignored when shader packs are loaded
    if (data.isTranslucent) {
        stateBuilder.setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER);
    } else {
        stateBuilder.setShaderState(RENDERTYPE_ENTITY_CUTOUT_SHADER);
    }
}
```

**What this does:** Instead of trying to use `mts_entity_lights` (which Iris ignores), we now use **standard Minecraft entity shaders** (`RENDERTYPE_ENTITY_CUTOUT_SHADER`) that Iris **does process** through its gbuffers pipeline.

**2. Standard Rendering Pipeline**
By using standard Minecraft shaders, the lights now go through:
- Minecraft's standard vertex processing
- Iris's `gbuffers_entities` program
- Complementary's entity material detection
- Standard texture/UV/color processing

#### 🔍 **Why This Works**

**Before:**
```
MTS Light → Custom MTS Shader → [IGNORED BY IRIS] → Black/Invisible
```

**After:**
```
MTS Light → Standard MC Entity Shader → Iris gbuffers_entities → Complementary Processing → Visible (but black)
```

#### 🎯 **The Critical Insight**

The breakthrough was understanding that **we can't fight Iris** - we have to **work with it**. Instead of trying to force our custom lighting through, we:

1. **Detect when Iris shader packs are active**
2. **Switch to standard Minecraft rendering**
3. **Let Iris process everything through gbuffers**
4. **Enhance colors in the standard pipeline**

This is exactly what the **Iris documentation recommends**:
> "It is strongly advised that mods that wish to maintain compatibility with Iris shader packs avoid using their own shader programs if at all possible"

### What We've Implemented
1. **Iris Detection System** (`IrisLightingIntegration.java`)
   - Detects when Iris is loaded
   - Detects when shader packs are active
   - Enables fallback rendering mode

2. **Fallback Rendering** (`InterfaceRender.java`)
   - Avoids MTS custom shaders when Iris is active
   - Forces pure white RGB(1.0, 1.0, 1.0) for light sources
   - Uses standard Minecraft entity shaders instead
   - Sets `LightTexture.FULL_BRIGHT` for maximum lightmap values

3. **Debug Logging**
   - Logs when fallback rendering is activated
   - Logs color forcing for debugging
   - Tracks Iris shader pack status

### Key Discovery: The Fundamental Issue

According to **official Iris documentation**:
> "Custom shaders added by mods or resource packs are ignored by Iris when an Iris shader pack is loaded"

This means:
- **MTS custom OpenGL lighting cannot work** with shader packs
- **Shader packs completely replace** the rendering pipeline
- **No general API exists** for mods to communicate with shader packs

## Current Debugging

### What We Need to Test
1. **Are the debug messages appearing?** Check logs for:
   ```
   IRIS FALLBACK: Forcing light to pure white RGB(1.0,1.0,1.0)
   ```

2. **Is the texture loading correctly?** The black appearance suggests:
   - Texture sampling might be failing
   - UV coordinates could be wrong
   - Complementary's material detection might be interfering

3. **Are vertex colors preserved?** Complementary's gbuffers might be:
   - Overriding vertex colors
   - Applying different material properties
   - Using texture-based emissive detection instead

### Next Steps
1. **Test pure white color forcing** (currently implemented)
2. **Check texture loading** in gbuffers pipeline
3. **Investigate Complementary's emissive detection** systems
4. **Consider texture naming conventions** (e.g., `_e` suffix for LabPBR)

## Fire Extinguisher Success

**Fire extinguisher particles work correctly** - this proves that:
- Our Iris detection is working
- Particle rendering pipeline is compatible
- The issue is specific to vehicle light geometry/textures

## Conclusion

We've made **significant progress**:
- ✅ Solved the fundamental incompatibility issue
- ✅ Implemented proper fallback rendering
- ✅ Proved lights CAN render with shader packs
- ❌ Need to fix the black color issue

The hardest part (making lights render at all) is **DONE**. Now we just need to fix the color/texture issue.