# Tire Dirt System and Drift Particles Issue

## Date: 2025-01-22

## Issue Summary
The tire dirt/wear system and drift particle effects are not working correctly in NeoForge 1.21.1 port.

## Expected Behavior (1.20.1 Original)
1. **Clean Tires**: Vehicles spawn with clean, black rubber tire textures
2. **Dirt Accumulation**: As vehicles drive (especially off-road or drifting), tires gradually accumulate dirt/wear texture overlay
3. **Drift Particles**: When drifting/sliding, particles are emitted from the tires showing dirt/smoke effects
4. **Dynamic System**: The dirt level changes based on driving behavior and terrain

## Current Behavior (1.21.1 Port)
1. **Always Dirty**: Tires appear dirty/worn from the start, even on newly spawned vehicles
2. **No Particles**: No drift/slide particles are generated when drifting
3. **Static Appearance**: Tire appearance doesn't change based on driving

## Root Cause Analysis

### Fixed Issues
1. **Translucent Render Pass**: Added missing `doRenderCall(true, partialTicks)` - this was causing lights and translucent particles to not render at all
2. **Light Value Handling**: Changed from `setUv2()` to `setLight()` for proper NeoForge 1.21.1 compatibility

### Temporary Workarounds
1. **Full Bright Lighting**: Currently using `LightTexture.FULL_BRIGHT` instead of actual world lighting
   - **Why**: The calculated light values were causing everything to render too dark
   - **Side Effect**: This might be causing the dirt overlay to always show at full opacity
   - **Proper Fix Needed**: Need to investigate why `LightTexture.pack(blockLight, skyLight)` produces incorrect values

### Remaining Issues
1. **Dirt Overlay System**:
   - The tire dirt/wear is likely a texture overlay system that uses alpha blending
   - With full bright lighting, the overlay might be rendering at 100% opacity always
   - Need to find where tire dirt level is calculated and how it's applied

2. **Missing Drift Particles**:
   - Particles are likely spawned by PartGroundDevice when detecting sliding/drifting
   - The particle spawning code might be checking conditions that aren't being met
   - Could be related to the light value issue affecting particle visibility/spawning

## Investigation Needed

### For Tire Dirt System:
- Find tire dirt/wear level calculation in PartGroundDevice or vehicle physics
- Check how dirt overlay texture is applied (likely in RenderableModelObject)
- Verify alpha blending for overlay textures works correctly

### For Drift Particles:
- Locate particle spawning in PartGroundDevice during drift/slide detection
- Check if particles are being spawned but not rendered, or not spawned at all
- Verify particle color and alpha values are correct

### For Lighting System:
- Investigate why world light values cause dark rendering
- Check if light value packing format changed in NeoForge 1.21.1
- Determine if shaders need adjustment for proper lighting

## Impact of Current Fixes

### Permanent Fixes (Correct)
1. **Translucent render pass addition** - This is the correct fix and matches original behavior
2. **Using setLight() instead of setUv2()** - This is the correct API for NeoForge 1.21.1

### Temporary Fixes (Need Refinement)
1. **Full bright lighting (`LightTexture.FULL_BRIGHT`)** - This is a workaround that:
   - Fixes the immediate "everything is dark" issue
   - But breaks dynamic lighting effects
   - May interfere with dirt overlay opacity
   - Should be replaced with proper light calculation

## Next Steps
1. Investigate PartGroundDevice for drift detection and particle spawning
2. Find tire dirt/wear overlay system and how it determines opacity
3. Fix the world lighting calculation to use actual light values instead of full bright
4. Test that drift particles spawn and render correctly
5. Verify tire appearance changes dynamically based on driving behavior