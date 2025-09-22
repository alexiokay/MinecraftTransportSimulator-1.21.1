# Tire Dirt System - Complete Analysis

## Date: 2025-01-22

## Key Finding: There May Not Be a Dynamic Dirt System

After extensive analysis, I've found:

### 1. No Dynamic Dirt/Wear System Found

**Evidence:**
- No "dirt level" or "wear amount" variables in the code
- No texture overlay system for progressive dirt accumulation
- No texture switching based on driving conditions
- The "wheellarge_rusty.png" texture appears to be a static variant, not dynamically applied

### 2. What Actually Exists

**Particle Effects Only:**
- Dirt/smoke particles spawn when driving based on:
  - Ground material (dirt, sand, gravel, etc.)
  - Speed thresholds
  - Slipping/skidding conditions
- These are visual effects only, not texture changes

**Texture Variants:**
- Different wheel designs (gold, goth, holes, rusty, etc.)
- These are static item variants, not condition-based changes

### 3. The "Always Dirty" Tire Issue

**Likely Cause:**
The tire texture itself may have:
1. **Dark/worn appearance baked into the base texture**
2. **Specular/roughness maps** that appear different under proper lighting
3. **Ambient occlusion** in the texture that looks like dirt

**Why it's visible now:**
- Our lighting fix (removing FULL_BRIGHT) reveals the true texture appearance
- The original texture might be designed to look "used" or "realistic"
- Without proper lighting, these details were washed out

### 4. Drift Particles Not Spawning

**Root Cause:**
The particles are defined but conditions aren't met:

```java
// From PartGroundDevice.java
case ("ground_slipping"):
    return vehicleOn != null && vehicleOn.slipping && animateAsOnGround ? 1 : 0
case ("ground_skidding"):
    return skipAngularCalcs ? 1 : 0
```

**Why they might not trigger:**
1. `vehicleOn.slipping` is not being set during drifts
2. `skipAngularCalcs` condition for skidding is too strict
3. The angular velocity threshold for detecting skids is incorrect

### 5. Vehicle Dirt System (If It Exists)

**Possibilities:**
1. **Pack-Specific Feature**: Some content packs might implement custom dirt
2. **Model-Based**: Specific vehicle models might have dirt variants
3. **Not in Core**: The dirt system might be in specific vehicle packs, not MTS core

### 6. Testing Required

To verify if cars get dirty in the original mod:
1. Test different vehicles (some might have it, others not)
2. Drive through mud/dirt/water
3. Check if any visual changes occur over time
4. Look for specific "weathered" or "dirty" model variants

### 7. The Real Issue

**For Tires:**
The tire texture appears dirty because:
- It's the actual texture design (realistic used tire look)
- Proper lighting reveals texture details that were hidden
- There's no actual dirt overlay system - it's just how the tire looks

**For Particles:**
- The particle system exists and works (fire extinguisher proves this)
- The drift/skid detection logic isn't triggering
- Need to debug why `slipping` and `skipAngularCalcs` aren't being set

### 8. Recommended Solution

**For "Dirty" Tires:**
1. Check if it's actually the intended tire texture appearance
2. If too dark, adjust the minimum light values in our lighting fix
3. Consider this might be the correct appearance

**For Missing Drift Particles:**
1. Add debug logging to PartGroundDevice:
```java
if (definition.ground.isWheel) {
    System.out.println("Wheel: " + this.getItem() +
        " Slipping: " + (vehicleOn != null ? vehicleOn.slipping : "null") +
        " Skidding: " + skipAngularCalcs +
        " Speed: " + (vehicleOn != null ? vehicleOn.velocity : "null"));
}
```

2. Lower the thresholds for skid detection
3. Verify particle definitions exist in the wheel JSON files

### Conclusion

The "car getting dirty" might be a misconception or limited to specific vehicles/packs. The core MTS system appears to only have:
- Static texture variants (clean vs rusty as separate items)
- Particle effects for visual feedback
- No progressive dirt accumulation system

The tire appearance issue is likely the correct texture being properly lit for the first time.