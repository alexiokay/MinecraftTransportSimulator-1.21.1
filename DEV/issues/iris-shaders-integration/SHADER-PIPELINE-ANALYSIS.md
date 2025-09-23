# MTS Shader Pipeline vs Iris Integration Analysis

## Current MTS Shader System

### MTS Custom Shaders Structure
```glsl
// mts_entity_lights.fsh (Vanilla MC pipeline)
#version 150
uniform sampler2D Sampler0;  // Main texture
uniform vec4 ColorModulator;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec2 texCoord0;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    color *= vertexColor * ColorModulator;

    // MTS custom lighting enhancements
    color.rgb *= 3.0; // Triple brightness
    color.rgb = max(color.rgb, vec3(0.5)); // 50% minimum

    // Extra boost for bright sources
    if (brightness > 0.7) color.rgb *= 2.0;

    fragColor = linear_fog(color, ...);
}
```

### MTS Render Pipeline Flow
1. **Shader Selection** (`InterfaceRender.java:1200-1250`)
   ```java
   if (data.lightingMode.disableWorldLighting) {
       if (IrisLightingIntegration.isIrisLoaded() && entityLightsIrisShader != null) {
           stateBuilder.setShaderState(MTS_ENTITY_LIGHTS_IRIS_SHADER);
       } else {
           stateBuilder.setShaderState(MTS_ENTITY_LIGHTS_SHADER);
       }
   }
   ```

2. **Light Value Assignment** (`InterfaceRender.java:900-950`)
   ```java
   int lightValue = data.lightingMode.disableWorldLighting ?
       LightTexture.FULL_BRIGHT : data.worldLightValue;
   buffer.setLight(lightValue);
   ```

3. **Render Type Creation** (`InterfaceRender.java:1100-1200`)
   ```java
   RenderType renderType = RenderType.create(
       "mts_entity_custom",
       DefaultVertexFormat.NEW_ENTITY,
       VertexFormat.Mode.QUADS,
       stateBuilder.build()
   );
   ```

## Iris Shader Pipeline (Reference: ComplementaryUnbound)

### Iris gbuffers_entities Structure
```glsl
// gbuffers_entities.glsl (Iris pipeline)
#version 130
#define GBUFFERS_ENTITIES

void main() {
    vec4 color = texture2D(tex, texCoord);
    color *= glColor;

    // Material detection and processing
    float emission = 0.0;
    float materialMask = OSIEBCA * 254.0;
    vec3 normalM = normal;

    // IPBR material handling
    #ifdef IPBR
        #include "/lib/materials/materialHandling/entityMaterials.glsl"
        emission = GetCustomEmissionForIPBR(color, emission);
    #endif

    // Output to multiple buffers
    /* DRAWBUFFERS:06 */
    gl_FragData[0] = color;                    // colortex0: Main color
    gl_FragData[1] = vec4(smoothnessD, materialMask, skyLightFactor, 1.0); // colortex6: Material properties
}
```

### Iris Pipeline Differences
| Aspect | MTS Pipeline | Iris Pipeline |
|--------|-------------|---------------|
| **Shaders** | Core shaders (`mts_entity_lights`) | gbuffers programs (`gbuffers_entities`) |
| **Output** | Single `fragColor` | Multiple `gl_FragData[]` buffers |
| **Lighting** | Direct color multiplication | Deferred lighting via gbuffers |
| **Materials** | Simple color + brightness | Complex material properties |
| **Integration** | Direct OpenGL calls | Shader pack framework |

## Key Integration Issues

### 1. **Shader System Mismatch**
**Problem**: MTS uses Minecraft's core shader system, Iris uses gbuffers programs
```java
// MTS registers core shaders
event.registerShader(new ShaderInstance(..., "mts_entity_lights", ...));

// Iris expects gbuffers programs
// gbuffers_entities.fsh, gbuffers_particles.fsh, etc.
```

**Solution**: Create gbuffers-compatible shaders that hook into Iris pipeline

### 2. **Light Data Loss in Pipeline**
**Problem**: MTS's `LightTexture.FULL_BRIGHT` doesn't translate to Iris gbuffers
```java
// MTS sets light value
buffer.setLight(LightTexture.FULL_BRIGHT); // = 15728640

// Iris gbuffers may not respect this value
```

**Solution**: Output emissive data in gbuffer material properties

### 3. **Entity Color/Texture Issues**
**Problem**: Entities appear white instead of textured
```glsl
// Current MTS-Iris shaderpack
vec4 color = texture2D(texture, texCoord);
// This may be failing - texture sampling issue?
```

**Solution**: Debug texture binding and sampling in gbuffers context

## Iris Integration Points

### 1. **gbuffers_entities.fsh** - Main Entity Rendering
```glsl
/* RENDERTARGETS: 0,1,2 */
layout(location = 0) out vec4 colortex0; // Main color + alpha
layout(location = 1) out vec4 colortex1; // Normals
layout(location = 2) out vec4 colortex2; // Material data (emission, smoothness, etc.)
```

### 2. **gbuffers_particles.fsh** - Particle Effects
```glsl
// Handle fire extinguisher particles, flame effects
void main() {
    vec4 color = texture2D(texture, texCoord);

    // Detect MTS particles by color/properties
    bool isMTSParticle = (color.r > 0.8 && color.g > 0.8 && color.b > 0.8) || // White particles
                        (color.r > 0.7 && color.g > 0.3 && color.b < 0.3);   // Flame particles

    if (isMTSParticle) {
        color.rgb *= MTS_LIGHT_BRIGHTNESS;
        // Mark as emissive in colortex2
    }
}
```

### 3. **Material Properties Buffer (colortex2)**
```glsl
// Store MTS-specific material data
float emission = isMTSLight ? 1.0 : 0.0;
float smoothness = 0.0;
float materialMask = 0.0;
float skyLightFactor = isMTSLight ? 1.0 : lmCoord.y; // Override sky light for MTS lights

colortex2 = vec4(emission, smoothness, materialMask, skyLightFactor);
```

## Recommended Integration Strategy

### Phase 1: Fix Current Pipeline Issues
1. **Debug white entity issue**
   - Add debug output to verify texture sampling
   - Check if `uniform sampler2D texture` is properly bound
   - Verify `texCoord` values are correct

2. **Improve MTS detection in shaderpack**
   ```glsl
   // Better MTS entity detection
   bool isMTSEntity() {
       // Check entity data, texture patterns, or specific properties
       return mc_Entity.x > 1000 || // Example entity ID range
              texture2D(texture, texCoord).a > 0.99 || // Full alpha entities
              vertexColor.a > 0.95; // High alpha vertex colors
   }
   ```

### Phase 2: Proper gbuffer Integration
1. **Create MTS-aware gbuffers shaders**
   ```glsl
   // gbuffers_entities_mts.fsh
   #define MTS_ENTITY_ENHANCEMENT
   #include "/lib/mts_lighting.glsl"
   ```

2. **Implement material property output**
   ```glsl
   // Output proper material data for deferred lighting
   vec4 materialData = vec4(
       isMTSLight ? 1.0 : 0.0,  // Emission
       0.0,                     // Smoothness
       MTS_MATERIAL_MASK,       // Material ID
       1.0                      // Sky light override
   );
   ```

### Phase 3: MTS Code Integration
1. **Modify MTS to provide better entity identification**
   ```java
   // In InterfaceRender.java
   // Pass MTS-specific data through mc_Entity attribute
   builder.setEntity(entity.getId(), isMTSLight ? 1 : 0, lightType, brightness);
   ```

2. **Create Iris-compatible render layers**
   ```java
   public static final RenderType MTS_EMISSIVE_ENTITIES = RenderType.create(
       "mts_emissive_entities",
       DefaultVertexFormat.NEW_ENTITY,
       VertexFormat.Mode.QUADS,
       // State compatible with gbuffers_entities
   );
   ```

## Testing Protocol

### 1. **Debug Current White Entity Issue**
```glsl
// Add to gbuffers_entities.fsh
void main() {
    vec4 debugColor = texture2D(texture, texCoord);

    // Debug output: Show texture sampling result
    if (debugColor.a < 0.01) {
        // Texture failed to load - output bright red
        colortex0 = vec4(1.0, 0.0, 0.0, 1.0);
    } else {
        // Texture loaded - show actual color
        colortex0 = debugColor * glColor;
    }
}
```

### 2. **Test MTS Light Detection**
```glsl
// Test brightness-based detection
float brightness = (color.r + color.g + color.b) / 3.0;
if (brightness > 0.7) {
    // This should be an MTS light - make it glow
    colortex0.rgb *= 5.0; // Extreme brightness for testing
    colortex2.r = 1.0;    // Mark as emissive
}
```

### 3. **Verify Pipeline Data Flow**
```java
// Add debug logging to InterfaceRender.java
InterfaceManager.coreInterface.logError("MTS RENDER: lightValue=" + lightValue +
    ", disableWorldLighting=" + data.lightingMode.disableWorldLighting +
    ", color=" + data.color + ", alpha=" + data.alpha);
```

## Expected Results After Integration

### Working Vehicle Lights
- ✅ Headlights appear as bright, emissive light sources
- ✅ Light beams cast by headlights are visible
- ✅ Vehicle textures remain colored (not white)
- ✅ Different light types (headlights, turn signals, etc.) work properly

### Working Particles
- ✅ Fire extinguisher particles are bright white and visible
- ✅ Flame particles from engines/fires are bright orange/red
- ✅ Particles don't disappear in dark shader environments

### Performance
- ✅ No significant FPS impact from shader enhancements
- ✅ Compatible with various shader packs (Complementary, BSL, SEUS)
- ✅ Graceful fallback when Iris is not present