# MTS Iris Shaders Integration Issues & Solutions

## Problem Summary

MTS (Minecraft Transport Simulator) vehicle lights, headlights, and fire extinguisher particles become invisible or severely dimmed when using Iris Shaders. The custom lighting system that works perfectly in vanilla Minecraft fails to render properly with shader packs enabled.

## Technical Root Causes

### 1. Custom Shader Pipeline Conflicts
- **Issue**: MTS uses custom shaders (`mts_entity_lights`, `mts_entity_cutout_noshadows`) that work in vanilla but conflict with Iris's rendering pipeline
- **Evidence**: Log shows `CRITICAL FAILURE: COULD NOT LOAD MTS SHADERS! Details: Invalid shaders/core/mts_entity_lights_iris.json: File not found`
- **Impact**: Falls back to regular shaders that aren't compatible with Iris's light handling

### 2. Entity Rendering Pipeline Wrapping
- **Issue**: Iris wraps the `MultiBufferSource` (`BufferSourceWrapper`) which changes how render data flows
- **Evidence**: Debug logging shows `buffer=BufferSourceWrapper` when Iris is active
- **Impact**: MTS lighting data may not be passed through correctly

### 3. Light Data Not Preserved Through Shader Pipeline
- **Issue**: MTS's `JSONLight` definitions and emissive properties don't translate to Iris's gbuffer system
- **Evidence**: Entities render white instead of with proper lighting/colors
- **Impact**: All lighting information is lost, headlights appear as solid white blocks

## Current Status

### What Works ✅
- Entity rendering pipeline (`doRenderCall()`) is being called with Iris active
- MTS entities are being rendered (visible as white shapes)
- Basic geometry and positioning is preserved

### What Doesn't Work ❌
- Vehicle headlights are invisible or severely dimmed
- Fire extinguisher particles disappear
- All entities appear solid white (losing color/texture information)
- Custom lighting effects completely fail
- Emissive properties are not respected

## Investigation Results

### Debug Findings
```
IRIS RENDER DEBUG: Iris active=true, buffer=BufferSourceWrapper, packedLight=15728640
```
- Confirmed Iris is active and intercepting renders
- `packedLight=15728640` suggests maximum light level but not working properly
- `BufferSourceWrapper` indicates Iris is modifying the render pipeline

### Shader Loading Analysis
```
ATTEMPTING TO REGISTER MTS CUSTOM SHADERS...
CRITICAL FAILURE: COULD NOT LOAD MTS SHADERS! Details: Invalid shaders/core/mts_entity_lights_iris.json: File not found
SUCCESS: mts_entity_lights shader loaded!
SUCCESS: mts_entity_cutout_noshadows shader loaded!
```
- Iris-specific shaders fail to load during initialization
- Fallback to vanilla shaders occurs
- Vanilla MTS shaders are incompatible with Iris pipeline

## Solution Approaches

### 1. ✅ Custom Shaderpack Solution (Implemented)
**Location**: `MTS-Iris-Shaderpack/`

Created a dedicated Iris-compatible shaderpack that:
- Detects bright entities and enhances them (3x brightness multiplier)
- Specifically targets vehicle lights and fire extinguisher particles
- Uses `gbuffers_entities.fsh` and `gbuffers_particles.fsh` for proper integration
- Provides configurable brightness settings

**Limitations**:
- Generic detection (enhances any bright entity, not just MTS)
- May still have white entity issue
- Requires users to install separate shaderpack

### 2. 🔄 MTS Code Integration (Partially Implemented)
**Files Modified**:
- `InterfaceRender.java` - Added Iris detection and shader switching
- `IrisLightingIntegration.java` - Created Iris API integration
- Shader files for Iris compatibility

**Status**: Compiled successfully but shader files not loading at runtime

### 3. 🚧 Pipeline-Level Integration (Needs Work)
The core issue appears to be that MTS's lighting data isn't being properly passed through Iris's gbuffer pipeline.

## Recommended Solutions

### Immediate Solution: Enhanced Shaderpack
Improve the current shaderpack to better detect MTS entities:

```glsl
// In gbuffers_entities.fsh
// Better MTS detection using entity data
varying vec4 entityData; // mc_Entity attribute

bool isMTSEntity() {
    // Check if entity ID suggests MTS origin
    // This would need specific entity ID ranges for MTS
    return entityData.x > 1000; // Example threshold
}
```

### Medium-term Solution: Render Layer Integration
Create MTS-specific render layers that work better with Iris:

```java
// Create dedicated render types for MTS lights
public static final RenderType MTS_VEHICLE_LIGHTS = RenderType.create(
    "mts_vehicle_lights",
    DefaultVertexFormat.NEW_ENTITY,
    VertexFormat.Mode.QUADS,
    // State setup that works with Iris
);
```

### Long-term Solution: Iris API Integration
Work with Iris developers to:
- Expose hooks for mod-specific lighting
- Allow custom light registration in gbuffer pipeline
- Provide mod compatibility API

## Technical Implementation Details

### Current Entity Rendering Flow
1. `BuilderEntityRenderForwarder.render()` called
2. Iris wraps `MultiBufferSource` → `BufferSourceWrapper`
3. `doRenderCall()` executes both solid and translucent passes
4. MTS lighting data gets lost in translation
5. Entities render as solid white

### Required Fix Points

#### 1. Color/Texture Preservation
```java
// In InterfaceRender.java, need to ensure texture data flows through
// Current issue: finalColor becomes white instead of textured
```

#### 2. Emissive Data Transmission
```java
// Need to mark MTS lights as emissive in gbuffer
// colortex2 should contain material properties
```

#### 3. Light Level Override
```java
// Force bright light levels for MTS lights
// Override packedLight value for vehicle lights
```

## Testing Protocol

### Test Cases
1. **Vehicle Headlights** - Spawn car, turn on headlights, test visibility with/without shaders
2. **Fire Extinguisher** - Use fire extinguisher, verify particle visibility
3. **Various Shader Packs** - Test with Complementary, BSL, SEUS
4. **Day/Night Cycle** - Verify lights work in different lighting conditions

### Expected Results
- ✅ Headlights should be clearly visible as bright light sources
- ✅ Fire extinguisher particles should appear white/bright
- ✅ Vehicle textures should preserve original colors
- ✅ Emissive areas should glow appropriately

## Files Requiring Attention

### Core MTS Files
- `InterfaceRender.java:500-600` - Shader selection and rendering logic
- `RenderableModelObject.java:200-300` - Light level calculation
- `EntityParticle.java` - Particle rendering with Iris

### Shader Files
- `gbuffers_entities.fsh` - Entity lighting and color preservation
- `gbuffers_particles.fsh` - Particle visibility enhancement
- `shaders.properties` - Configuration and compatibility settings

### Integration Files
- `IrisLightingIntegration.java` - Iris API compatibility layer
- `BuilderEntityRenderForwarder.java` - Debug logging and render flow

## Next Steps

1. **Fix White Entity Issue** - Investigate why entities lose color/texture data
2. **Improve MTS Detection** - Better entity identification in shaders
3. **Test Light Visibility** - Verify headlights actually appear bright
4. **Performance Optimization** - Ensure solution doesn't impact FPS
5. **User Documentation** - Create installation guide for shaderpack solution

## Community Impact

This integration affects:
- ✈️ **Aviation packs** - Aircraft landing lights, navigation lights
- 🚗 **Vehicle packs** - Car headlights, emergency vehicle lights
- 🚢 **Maritime packs** - Ship navigation lights, search lights
- 🏭 **Industrial packs** - Machinery indicator lights
- 🎮 **General gameplay** - All MTS content becomes more immersive with shaders

## Reference Links

- [Iris Shaders Documentation](https://github.com/IrisShaders/Iris)
- [MTS Rendering System](../../../mccore/src/main/java/minecrafttransportsimulator/rendering/)
- [Test Shaderpack](../../../MTS-Iris-Shaderpack/)
- [Integration Code](../../../neoforge/src/main/java/mcinterface1211/IrisLightingIntegration.java)