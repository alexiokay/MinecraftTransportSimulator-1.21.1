# MTS Beam Detection Test Guide

This guide helps you test the new MTS beam detection system with Complementary shaders.

## What Was Implemented

### 1. Vertex Data Markers
- MTS beams now encode identification data in vertex colors when Iris is active
- Red channel: 252/255 (0.988) = beam identifier
- Green channel: beam intensity (0.0-1.0)
- Blue channel: beam alpha/transparency
- Alpha channel: rendering alpha

### 2. Enhanced Shaders
- Modified `mts_entity_lights_iris.fsh` to detect and enhance beams
- Created `complementary_mts_beam_detection.fsh` as reference for Complementary devs
- Added beam-specific brightness, glow, and color temperature effects

## How to Test

### Prerequisites
1. Build and run MTS with the new changes
2. Load a vehicle pack with light beam definitions (`isBeam: true`)
3. Have Iris shaders installed with any shader pack

### Testing Steps

1. **Enable Iris Shaders**
   - Install Iris and a shader pack (BSL, Complementary, etc.)
   - Enable shaders in video settings

2. **Spawn Vehicle with Beams**
   ```
   /mts give @s mts:handbook
   ```
   - Use handbook to spawn a vehicle with headlights
   - Or place a vehicle with defined light beams

3. **Activate Beams**
   - Get in vehicle and turn on headlights (usually `R` key)
   - Or use vehicle controls to activate beacon/marker lights

4. **Visual Verification**
   - Beams should appear significantly brighter than normal
   - Check console logs for beam detection messages:
     ```
     [INFO] IRIS BEAM: Using vertex data markers - R:252 G:[intensity] B:[alpha]
     ```

### Expected Behavior

#### With Iris Active (Fallback Mode):
- **High Intensity Beams** (>0.8): Warm white, very bright
- **Medium Intensity Beams** (0.4-0.8): Neutral white, bright
- **Low Intensity Beams** (<0.4): Cool white, moderate brightness
- **All Beams**: Enhanced glow and emissive effects

#### Without Iris:
- Normal MTS beam rendering behavior

### Debug Mode

To enable debug visualization, uncomment this line in the shader:
```glsl
// color.rgb = mix(color.rgb, vec3(0.0, 1.0, 0.0), 0.3);
```

This will tint detected beams green for easy identification.

### Console Logging

Watch for these log messages:

**Beam Detection:**
```
IRIS BEAM: Using vertex data markers - R:252 G:255 B:128
```

**Iris Status:**
```
IRIS INTEGRATION: Shaders enabled, using fallback rendering
IRIS INTEGRATION: Iris shaders detected!
```

## Troubleshooting

### Beams Not Enhanced
1. Check Iris is properly installed and enabled
2. Verify vehicle has `isBeam: true` in light definitions
3. Look for beam detection log messages
4. Try different shader packs

### Performance Issues
1. The beam detection adds minimal overhead (one conditional per fragment)
2. If needed, adjust intensity multipliers in shader
3. Consider reducing beam count in vehicle JSON

### False Positives
- Very unlikely due to specific red channel value (252/255)
- If conflicts arise, the marker value can be adjusted

## Integration with Complementary

For Complementary shader pack integration:

1. Use the provided `complementary_mts_beam_detection.fsh` as reference
2. Add detection logic to `gbuffers_entities.fsh`
3. Consider adding user controls for beam enhancement strength
4. Test with various MTS vehicle packs

## Next Steps

1. **Community Testing**: Get feedback from MTS + shader pack users
2. **Performance Optimization**: Fine-tune enhancement effects
3. **Complementary Integration**: Work with Complementary devs for official support
4. **Additional Features**: Consider beam direction, color temperature, etc.

This system provides a clean, performant way to detect and enhance MTS beams within any shader pack that uses the standard entity rendering pipeline.