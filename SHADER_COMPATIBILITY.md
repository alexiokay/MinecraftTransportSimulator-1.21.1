# Shader Pack Compatibility Guide

## Current Status (NeoForge 1.21.1 + NeOculus/Iris)

### What Works ✅

- **Emissive light overlays**: Vehicle lights glow correctly on models
- **Lens flares**: Render correctly with shader packs active
- **Particles**: All particle effects work with shader packs
- **Shader detection**: Real-time toggle when enabling/disabling shader packs
- **Vanilla shader fallback**: Automatically uses standard Minecraft shaders when Iris is active

### Known Issues ⚠️

- **Light beams appear black**: Directional headlight beams render as black/dark instead of bright when shader packs are active
  - **Workaround**: Beams are automatically disabled when shader packs are detected
  - **Status**: This is a limitation of how shader packs process custom mod shaders

- **Emissive lights may appear as flat colors**: Some shader packs don't process alpha/transparency on emissive overlays correctly
  - **Root cause**: NeOculus texture binding issue with custom mod shaders
  - **Status**: Under investigation

### Architecture

When a shader pack is active (Iris/NeOculus):

```
MTS Rendering Flow with Shader Packs:
┌─────────────────────────────────────┐
│  1. Detect shader pack active      │
│     (ModCompatibility.areShadersEnabled())
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  2. Switch to vanilla shaders       │
│     - EntityTranslucentShader       │
│     - EntityCutoutShader            │
│     (Custom MTS shaders disabled)   │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  3. Disable light beams             │
│     (shouldDisableBeamsForShaderCompatibility())
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  4. Render with standard pipeline   │
│     - Shader pack processes through │
│       gbuffers_entities             │
│     - Lens flares still work        │
│     - Emissive overlays still work  │
└─────────────────────────────────────┘
```

## Solutions for Full Compatibility

### Solution 1: Emissive Texture Convention (Recommended)

Use the `_e` suffix texture naming convention that shader packs recognize:

**Structure**:
```
assets/yourpack/textures/vehicles/
├── headlight.png       # Base vehicle texture
└── headlight_e.png     # Emissive overlay (glowing parts)
```

**Benefits**:
- ✅ Works with ALL shader packs (Complementary, BSL, Iris, OptiFine)
- ✅ Automatic emissive detection by shader packs
- ✅ No code changes required
- ✅ Proper alpha/transparency handling

**Limitations**:
- Requires texture reorganization
- Static emissive areas (can't dynamically change which parts glow)

### Solution 2: Shader Pack Integration (Advanced)

Work with shader pack developers to add MTS-specific detection in their `gbuffers_entities.glsl`:

See [COMPLEMENTARY_BEAM_DETECTION.md](./COMPLEMENTARY_BEAM_DETECTION.md) for details on vertex color encoding for beam detection.

**Benefits**:
- ✅ Full control over beam appearance
- ✅ Can enable beams with shader packs
- ✅ Custom lighting effects per shader pack

**Limitations**:
- Requires shader pack developer cooperation
- Only works with modified shader packs
- Maintenance burden as shader packs update

### Solution 3: Particle-Based Beams (Future)

Replace volumetric beam geometry with particle chains:

```java
// Concept (not yet implemented)
for (Point3D point : beamPath) {
    spawnEmissiveParticle(point, lightColor, brightness);
}
```

**Benefits**:
- ✅ Shader packs process particles through `gbuffers_particles`
- ✅ Works with all shader packs
- ✅ No texture issues

**Limitations**:
- Different visual appearance than current beams
- Performance considerations with many particles
- Less precise beam geometry

## For Content Pack Creators

### Current Best Practices

1. **Headlights and Spotlights**:
   - Use `blendableComponents` for lens flares (these work great!)
   - Beam rendering is automatically disabled with shader packs
   - Consider this when designing vehicle lighting

2. **Colored Lights**:
   - Emissive overlays work, but may appear flat with some shader packs
   - Color specification in JSON works correctly
   - Test with popular shader packs (Complementary, BSL)

3. **Night Visibility**:
   - Don't rely solely on beams for night driving
   - Use emissive overlays on headlight models
   - Lens flares provide good visual feedback

### Testing Checklist

- [ ] Test vehicles WITHOUT shader packs (should see beams)
- [ ] Test vehicles WITH Complementary shaders
- [ ] Test vehicles WITH BSL shaders
- [ ] Verify emissive lights are visible
- [ ] Check lens flares appear correctly
- [ ] Confirm no rendering errors in logs

## For Developers

### Key Files

- `mcinterface1211/InterfaceRender.java:719-721` - Beam disabling logic
- `mcinterface1211/InterfaceRender.java:636-655` - Shader detection and fallback
- `mcinterface1211/ModCompatibility.java` - Iris/Oculus detection
- `mcinterface1211/InterfaceRender.java:369` - Cache key includes shader state

### Adding Shader Pack Support

To add support for a new shader pack:

1. Verify detection in `ModCompatibility.areShadersEnabled()`
2. Ensure render type cache invalidates when shaders toggle
3. Test with config option `MTSConfig.IRIS_SHADER_COMPATIBILITY`

### Debug Logging

Enable rendering debug logs to troubleshoot:

```java
// Check if shader state changes are detected
System.out.println("Shaders enabled: " + ModCompatibility.areShadersEnabled());
System.out.println("Render type: " + renderType);
```

## Known Compatible Shader Packs

| Shader Pack | Version | Status | Notes |
|-------------|---------|--------|-------|
| **Complementary** | 5.x | ✅ Works | Lights visible, beams disabled |
| **BSL** | Latest | ✅ Works | Lights visible, beams disabled |
| **Vanilla Plus** | Latest | ✅ Works | Lights visible, beams disabled |
| **SEUS** | PTGI | ⚠️ Partial | Some emissive issues |

## Future Improvements

### Planned
- [ ] Investigate emissive texture `_e` suffix auto-detection
- [ ] Test particle-based beam system prototype
- [ ] Collaborate with Complementary developers for beam detection

### Under Consideration
- [ ] Dynamic light integration (LambDynamicLights API)
- [ ] Hybrid rendering (custom shaders + particles)
- [ ] Per-shader-pack rendering profiles

## Support

If you encounter shader compatibility issues:

1. Check if `MTSConfig.IRIS_SHADER_COMPATIBILITY` is enabled
2. Verify shader pack version is compatible with NeoForge 1.21.1
3. Test without shader packs to confirm MTS rendering works
4. Check logs for shader-related errors
5. Report issues with:
   - Shader pack name and version
   - NeOculus/Iris version
   - Screenshots with and without shaders
   - MTS vehicle pack being used

---

**Note**: This is an evolving document. As shader pack compatibility improves, this guide will be updated with new solutions and workarounds.
