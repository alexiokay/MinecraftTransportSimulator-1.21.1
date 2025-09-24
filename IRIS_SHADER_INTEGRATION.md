# MTS Iris Shader Integration Guide

This guide explains how the Minecraft Transport Simulator (MTS) now integrates with Iris shaders and NeoForge 1.21.1 to provide enhanced lighting effects for vehicles, parts, and particles.

## Overview

The MTS Iris integration provides:
- **Dynamic Light Sources**: Vehicle headlights, beacons, and other light sources now work with Iris shaders
- **Enhanced Particle Lighting**: Fire extinguisher effects, muzzle flashes, and other bright particles cast dynamic lights
- **Shader Compatibility**: Custom MTS shaders work alongside popular shader packs like BSL, Complementary, and SEUS

## Requirements

- **Minecraft**: 1.21.1
- **NeoForge**: Latest version for 1.21.1
- **Iris Shaders**: 1.8.0+ for NeoForge 1.21.1
- **LambDynamicLights** (optional but recommended): For enhanced dynamic lighting effects

## Features

### 1. Vehicle Lighting

**Headlights and Beacons**:
- Car headlights now cast dynamic light when turned on
- Aircraft beacons create pulsing light effects
- Emergency vehicle lights work with shader packs
- Light intensity matches the JSON light definitions

**Usage**:
```json
{
  "objectName": "headlight_left",
  "emissive": true,
  "color": "FFFFFF",
  "isBeam": true,
  "blendableComponents": [
    {
      "pos": [0, 0, 1],
      "axis": [0, 0, 1],
      "beamLength": 10,
      "beamDiameter": 2
    }
  ]
}
```

### 2. Particle Effects

**Fire Extinguisher Particles**:
- Fire extinguisher effects now emit light
- Particles automatically register with dynamic lighting systems
- Works with both `isBright` and `FLAME` type particles

**Enhanced Particles**:
```json
{
  "type": "FLAME",
  "isBright": true,
  "isBlended": true,
  "scale": 2.0,
  "duration": 100
}
```

### 3. Shader Compatibility

**Automatic Detection**:
- System automatically detects Iris and LambDynamicLights
- Falls back gracefully if shaders aren't available
- Uses enhanced shaders when Iris is present

**Custom Shaders**:
- `mts_entity_lights_iris.json/vsh/fsh`: Enhanced lighting shader for Iris
- Supports emissive materials and dynamic lighting
- Compatible with most popular shader packs

## Installation

1. Install NeoForge 1.21.1
2. Install Iris Shaders 1.8.0+ for NeoForge
3. Install LambDynamicLights (optional)
4. Install MTS with the updated lighting system
5. Choose your favorite shader pack

## Configuration

### In-Game Settings

**MTS Settings**:
- `Blended Lights`: Enable for best visual quality with shaders
- `Rendering Mode`: Set to 0 for best performance with dynamic lighting

**Iris Settings**:
- Enable `Dynamic Lights` in shader pack options if available
- Adjust `Light Quality` for performance vs. visual quality

### JSON Configuration

**Light Definitions**:
```json
{
  "lights": [
    {
      "objectName": "headlight",
      "emissive": true,
      "isElectric": true,
      "color": "FFDD88",
      "brightnessAnimations": [
        {
          "animationType": "visibility",
          "variable": "headlight_on"
        }
      ]
    }
  ]
}
```

**Particle Definitions**:
```json
{
  "particles": [
    {
      "type": "GENERIC",
      "isBright": true,
      "isBlended": true,
      "daytimeReductionFactor": 0.3,
      "emissiveStrength": 1.5
    }
  ]
}
```

## Performance Optimization

### Best Practices

1. **Light Count**: Limit active lights to ~20-30 for best performance
2. **Particle Density**: Use `quantity` sparingly for bright particles
3. **Shader Quality**: Lower shader quality if experiencing lag
4. **LOD Settings**: Increase LOD distance to reduce distant light calculations

### Performance Settings

**High Performance**:
```json
{
  "renderingMode": 0,
  "blendedLights": false,
  "particleDensity": 0.5
}
```

**High Quality**:
```json
{
  "renderingMode": 0,
  "blendedLights": true,
  "particleDensity": 1.0
}
```

## Troubleshooting

### Common Issues

**Lights Not Working**:
1. Check that Iris is properly installed
2. Verify LambDynamicLights is compatible with your Iris version
3. Ensure the vehicle's battery isn't dead (use `isElectric: false` for testing)

**Performance Issues**:
1. Reduce number of active lights
2. Lower shader quality settings
3. Disable dynamic lighting temporarily to test

**Shader Conflicts**:
1. Update to latest Iris version
2. Check shader pack compatibility
3. Try a different shader pack

### Debug Information

The system logs debug information to help diagnose issues:
```
[INFO] IRIS INTEGRATION: Iris shaders detected!
[INFO] IRIS INTEGRATION: LambDynamicLights detected!
[INFO] IRIS INTEGRATION: Registered dynamic light vehicle_123_headlight with level 15
```

## Supported Shader Packs

**Fully Compatible**:
- BSL Shaders
- Complementary Shaders
- SEUS PTGI
- Photon Shader

**Partially Compatible**:
- Sildurs Vibrant (limited dynamic light support)
- Continuum RT (may need manual configuration)

## Advanced Usage

### Custom Light Sources

You can create custom light sources that work with Iris:

```java
// In your custom part class
@Override
public void update() {
    super.update();
    if (shouldEmitLight()) {
        IrisLightingIntegration.registerDynamicLight(this, lightDefinition, brightness);
    }
}
```

### Particle Integration

Custom particles can register with the lighting system:

```java
// In your particle constructor
if (definition.isBright) {
    IrisLightingIntegration.registerParticleLight(this, lightIntensity);
}
```

## API Reference

### IrisLightingIntegration

**Methods**:
- `registerDynamicLight(entity, lightDef, brightness)`: Register a light source
- `unregisterDynamicLight(entity, objectName)`: Remove a light source
- `registerParticleLight(particle, brightness)`: Register particle lighting
- `isIrisLoaded()`: Check if Iris is available
- `isDynamicLightingAvailable()`: Check if dynamic lighting is available

### Light JSON Properties

**Enhanced Properties**:
- `emissiveStrength`: Multiplier for light intensity (default: 1.0)
- `irisCompatible`: Force Iris-specific rendering (default: auto-detect)
- `dynamicRange`: Maximum light distance in blocks (default: 15)

## Version History

**v1.0.0**:
- Initial Iris integration
- Basic dynamic lighting support
- Particle light registration

**v1.1.0**:
- Enhanced shader compatibility
- Performance optimizations
- LambDynamicLights integration

**v1.2.0**:
- Custom shader support
- Advanced particle effects
- Improved fallback handling

## Support

For issues related to Iris integration:
1. Check this documentation first
2. Review the debug logs
3. Test with minimal shader packs
4. Report issues with specific reproduction steps

The integration is designed to enhance the MTS experience while maintaining compatibility with all existing content and configurations.