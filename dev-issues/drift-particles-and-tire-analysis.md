# Drift Particles and Tire Dirt Analysis

## Date: 2025-01-22

## Analysis Summary

### 1. Particle System Architecture

The MTS mod uses a particle definition system where particles are defined in JSON files with activation conditions:

**Key Variables for Ground Devices:**
- `ground_onground` - Whether wheel is on ground (0 or 1)
- `ground_contacted` - Whether wheel made contact this tick (for striking effects)
- `ground_skidding` - Whether wheel is skidding (skipAngularCalcs = true)
- `ground_slipping` - Whether vehicle is slipping (vehicleOn.slipping = true)
- `ground_blockmaterial_*` - Material type below wheel

**Particle Spawning Logic:**
```java
// In PartGroundDevice.java
case ("ground_skidding"):
    return new ComputedVariable(this, variable, partialTicks -> skipAngularCalcs ? 1 : 0, false);
case ("ground_slipping"):
    return new ComputedVariable(this, variable, partialTicks -> vehicleOn != null && vehicleOn.slipping && animateAsOnGround ? 1 : 0, false);
```

### 2. Drift/Skid Particle Definitions

From the wheel JSON files, particles are triggered by combinations of:
- Ground material (dirt, sand, stone, etc.)
- Speed thresholds
- Ground contact state
- Slipping/skidding variables

Example particle definition for dirt:
```json
{
  "type": "generic",
  "spawnEveryTick": true,
  "textureList": ["dirt0.png", "dirt1.png", "dirt2.png", "dirt3.png"],
  "activeAnimations": [
    {
      "animationType": "visibility",
      "variable": "ground_blockmaterial_dirt",
      "clampMin": 1.0,
      "clampMax": 1.0
    },
    {
      "animationType": "visibility",
      "variable": "ground_slipping",
      "clampMin": 1.0,
      "clampMax": 1.0
    }
  ]
}
```

### 3. Why Particles Aren't Spawning

**Possible Causes:**

1. **Slipping/Skidding Detection Issue**
   - The `vehicleOn.slipping` or `skipAngularCalcs` flags may not be set properly
   - Angular velocity calculations might be incorrect

2. **Particle Definition Missing**
   - The wheel parts might not have drift-specific particle definitions
   - The `ground_slipping` or `ground_skidding` conditions might not be in the JSON

3. **Animation Switchbox Not Triggering**
   - The AnimationSwitchbox that controls particle spawning might not evaluate to true
   - Multiple conditions might need to be met simultaneously

### 4. Tire Dirt/Wear System

**Current Issue:** Tires appear dirty immediately, not progressively

**Likely Cause:**
The tire dirt is probably a texture overlay that should:
1. Start with 0% opacity (clean tire)
2. Increase opacity based on driving conditions
3. Use variables like distance traveled, terrain type, or drift time

**Missing Component:**
We haven't found a specific "tire_dirt_level" or "wear_amount" variable. The dirt appearance might be:
- A texture layer that's always at 100% opacity due to lighting issues
- Missing alpha blending in the shader
- Incorrectly initialized variable

### 5. Key Files to Investigate

1. **PartGroundDevice.java** - Contains slip/skid detection logic
2. **Vehicle JSON files** - Define particle spawn conditions
3. **Wheel part JSON files** - Define particle effects for drifting
4. **EntityParticle.java** - Particle entity spawning and rendering
5. **AnimationSwitchbox.java** - Controls when particles spawn

### 6. Recommended Fixes

1. **For Drift Particles:**
   - Add debug logging to check if `ground_slipping` and `ground_skidding` variables are changing
   - Verify particle definitions exist in wheel JSON files
   - Check if particle spawning conditions are being met

2. **For Tire Dirt:**
   - Look for wear/dirt texture overlays in the model
   - Check if there's an alpha/opacity animation for dirt overlay
   - Verify texture blending is working correctly with our lighting fixes

### 7. Next Steps

1. Add debug output for ground device variables:
   ```java
   System.out.println("Skidding: " + skipAngularCalcs + ", Slipping: " + vehicleOn.slipping);
   ```

2. Check if particle definitions exist for the specific wheels being tested

3. Investigate if there's a dirt overlay texture system we haven't found yet

4. Compare with 1.20.1 to see if particle spawning logic changed