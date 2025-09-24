# MTS Beam Detection for Complementary Shaders

This guide explains how Complementary shader developers can detect and enhance Minecraft Transport Simulator (MTS) light beams within the standard `gbuffers_entities` pipeline.

## Overview

MTS now embeds beam identification data directly into vertex colors when Iris shaders are active, allowing Complementary shaders to detect beams without requiring separate shader programs.

## Detection Method

### Vertex Color Encoding

When Iris is active, MTS beams use a specific vertex color encoding:

```glsl
// In your gbuffers_entities fragment shader
vec4 color = texture2D(gtexture, texcoord) * glcolor;

// Detect MTS beams by checking the red channel
bool isMTSBeam = (glcolor.r > 0.98 && glcolor.r < 0.99); // ~252/255

if (isMTSBeam) {
    float beamIntensity = glcolor.g;  // 0.0-1.0 intensity
    float beamAlpha = glcolor.b;      // Original beam alpha
    float renderAlpha = glcolor.a;    // Current rendering alpha

    // Apply custom beam lighting effects here
    // ...
}
```

### Vertex Color Channels

| Channel | Value Range | Purpose |
|---------|-------------|---------|
| **Red** | ~0.988 (252/255) | Beam identifier - constant marker |
| **Green** | 0.0-1.0 | Beam light intensity from JSON |
| **Blue** | 0.0-1.0 | Beam alpha/transparency |
| **Alpha** | 0.0-1.0 | Current rendering alpha |

## Integration Examples

### Basic Beam Enhancement

```glsl
#version 150

// In gbuffers_entities.fsh
uniform sampler2D gtexture;

in vec4 glcolor;
in vec2 texcoord;
in vec3 normal;

out vec4 fragColor;

void main() {
    vec4 color = texture2D(gtexture, texcoord) * glcolor;

    // Detect MTS beams
    bool isMTSBeam = (glcolor.r > 0.98 && glcolor.r < 0.99);

    if (isMTSBeam) {
        float intensity = glcolor.g;

        // Enhanced beam rendering
        color.rgb *= 2.0 + intensity; // Extra brightness
        color.rgb += vec3(0.1 * intensity); // Slight bloom

        // Optional: Add color temperature based on intensity
        if (intensity > 0.8) {
            color.rgb *= vec3(1.0, 0.95, 0.9); // Warm white
        }
    }

    fragColor = color;
}
```

### Advanced Beam Effects

```glsl
// More sophisticated beam handling
void main() {
    vec4 color = texture2D(gtexture, texcoord);

    bool isMTSBeam = (glcolor.r > 0.98 && glcolor.r < 0.99);

    if (isMTSBeam) {
        float intensity = glcolor.g;
        float beamAlpha = glcolor.b;

        // Dynamic range compression for HDR-like effect
        float enhancedIntensity = 1.0 - exp(-intensity * 2.0);

        // Apply beam-specific lighting
        color.rgb = mix(color.rgb, vec3(1.0), enhancedIntensity * 0.3);
        color.rgb *= 1.0 + intensity * 3.0; // Strong brightness boost

        // Preserve original alpha behavior
        color.a = glcolor.a;

        // Optional: Add distance-based effects using screen position
        // vec2 screenPos = gl_FragCoord.xy / screenSize;
        // float distanceFromCenter = length(screenPos - vec2(0.5));
        // color.rgb *= mix(1.2, 0.8, distanceFromCenter);
    } else {
        color *= glcolor;
    }

    fragColor = color;
}
```

### Compatibility Check

```glsl
// Check if MTS beams are present in the scene
uniform sampler2D colortex0;

bool hasMTSBeams() {
    // Sample a few pixels to detect beam markers
    ivec2 screenSize = textureSize(colortex0, 0);

    for (int x = 0; x < screenSize.x; x += 32) {
        for (int y = 0; y < screenSize.y; y += 32) {
            vec2 coord = vec2(x, y) / screenSize;
            vec4 sample = texture2D(colortex0, coord);

            if (sample.r > 0.98 && sample.r < 0.99) {
                return true;
            }
        }
    }

    return false;
}
```

## Implementation Notes

### Performance Considerations

1. **Minimal Overhead**: The detection adds only one conditional check per fragment
2. **No Texture Sampling**: Uses existing vertex color data
3. **Compatible**: Works with all existing Complementary features

### Edge Cases

1. **False Positives**: Very unlikely due to specific red channel value (252/255)
2. **Multiple Beams**: Each beam vertex is individually marked
3. **Transparency**: Uses standard alpha blending - no special handling needed

### Shader Pack Integration

To integrate this into Complementary:

1. Add the detection code to `gbuffers_entities.fsh`
2. Optionally add beam-specific uniforms for user control
3. Consider adding beam effects to deferred lighting passes
4. Test with various MTS vehicle packs

## Testing

### Vehicle Types with Beams
- **Cars**: Headlights, fog lights
- **Aircraft**: Navigation lights, landing lights
- **Boats**: Spotlights, navigation lights
- **Trains**: Headlights, marker lights

### Test Scenarios
- **Day/Night**: Beams should be more visible at night
- **Weather**: Rain/fog should interact with beams
- **Distance**: Beam effects should scale appropriately
- **Performance**: No significant FPS impact

## Future Enhancements

### Potential Additions
- **Beam Direction**: Could use normal vectors for directional effects
- **Beam Type**: Additional markers for different light types
- **Animation Data**: Support for flashing/pulsing lights
- **Color Temperature**: More sophisticated color information

### Feedback Integration
- Monitor community feedback for detection accuracy
- Adjust marker values if conflicts arise
- Consider additional data channels if needed

## Support

For shader pack developers:
- Test with MTS vehicles that have defined light beams
- Check the MTS logs for beam identification messages
- Report any detection issues or conflicts

This system provides a clean, performant way for Complementary shaders to detect and enhance MTS light beams while maintaining full compatibility with the existing shader pipeline.