# NeoForge 1.21.1 Translucent Rendering Fix

## Issue Summary
Vehicle lights (headlights, taillights, indicators) and fire extinguisher particles were not rendering in NeoForge 1.21.1.

## Root Cause
The **translucent rendering pass was completely missing** from the MTS NeoForge implementation.

### Technical Details
- MTS only called `doRenderCall(false, partialTicks)` (solid pass)
- Never called `doRenderCall(true, partialTicks)` (translucent pass)
- Emissive light overlays have `isTranslucent=true` and require the translucent pass
- Without the translucent pass, lights were skipped with "wrong pass" error

### Debug Evidence
```
RENDER_PASS_DEBUG: &headlights blendingEnabled=false isTranslucent=true actualLevel=0.8
RENDER_SKIP: Skipping &headlights wrong pass
```

## Solution
**File:** `neoforge/src/main/java/mcinterface1211/InterfaceRender.java`
**Lines:** 239-240

### Fix Applied
```java
// Render solid pass first
doRenderCall(false, partialTicks);

// CRITICAL FIX: Add missing translucent pass for emissive lights
doRenderCall(true, partialTicks);
```

### Location
In the `onIVRegisterRenderersEvent` method, after setting up matrix variables but before the closing block.

## What This Fixes
- ✅ Vehicle headlights, taillights, indicators
- ✅ Fire extinguisher particle effects
- ✅ All MTS entities requiring alpha blending/transparency
- ✅ Emissive light overlays in general

## Impact
This is a **critical architectural fix** for NeoForge 1.21.1 compatibility. Without this change, any MTS content requiring translucent rendering will be invisible.

## Notes
- This issue was specific to NeoForge 1.21.1
- Previous versions may have handled translucent rendering differently
- The fix adds the missing rendering pass without affecting existing solid rendering

## Verification
After applying the fix, debug logs should show:
```
EMISSIVE_RENDER: Rendering [light_name] with alpha=X.X
```
Instead of:
```
RENDER_SKIP: Skipping [light_name] wrong pass
```

---
**Date:** September 22, 2025
**Branch:** 1.21.1
**Severity:** Critical
**Status:** Fixed