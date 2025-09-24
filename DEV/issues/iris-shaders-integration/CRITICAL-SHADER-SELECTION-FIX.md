# 🚨 CRITICAL: Shader Selection Logic Fix

**Date:** September 25, 2025
**Priority:** CRITICAL - Core rendering functionality
**Status:** ✅ FIXED - Custom MTS shaders restored when appropriate

## 🎯 THE PROBLEM

MTS beams were appearing **BLACK** when shaders were disabled due to **incorrect shader selection logic**.

### ❌ BROKEN LOGIC:
```java
// WRONG: This checked if Iris MOD was installed (always true)
if (ModCompatibility.hasShaderMod()) {
    // This ALWAYS used standard MC shaders when Iris mod present
    stateBuilder.setShaderState(GameRenderer.getRendertypeEntityCutoutShader());
}
```

**Result:** Even with shaders DISABLED, MTS used standard MC shaders → No custom lighting → BLACK beams

## 🎯 THE SOLUTION

### ✅ CORRECT LOGIC:
```java
// CORRECT: This checks if shader PACK is actively running
if (ModCompatibility.areShadersEnabled()) {
    // Use standard MC shaders ONLY when shader pack is active
    stateBuilder.setShaderState(GameRenderer.getRendertypeEntityCutoutShader());
} else {
    // Use custom MTS shaders when no shader pack active
    if (data.lightingMode.disableWorldLighting) {
        stateBuilder.setShaderState(MTS_ENTITY_LIGHTS_SHADER);  // ← BRIGHT beams!
    }
}
```

## 🔑 CRITICAL DIFFERENCE:

| Function | Purpose | When True |
|----------|---------|-----------|
| `hasShaderMod()` | ❌ Mod detection | Always true if Iris installed |
| `areShadersEnabled()` | ✅ Real-time status | Only true when shader pack running |

## 📍 FILE LOCATION:
**File:** `neoforge/src/main/java/mcinterface1211/InterfaceRender.java`
**Method:** `createForObject(RenderableData data)`
**Lines:** ~656

## 🎯 TECHNICAL IMPACT:

### Before Fix:
- **Shaders enabled**: Beams disabled (correct) ✅
- **Shaders disabled**: BLACK beams (wrong shader) ❌

### After Fix:
- **Shaders enabled**: Beams disabled (correct) ✅
- **Shaders disabled**: BRIGHT beams (custom MTS shader) ✅

## ⚠️ LESSON LEARNED:

**NEVER confuse mod detection with feature state detection!**

- **Mod detection** (`hasShaderMod`) = Is the mod installed?
- **Feature detection** (`areShadersEnabled`) = Is the feature currently active?

This is a **fundamental architectural principle** for any mod compatibility system.

## 🚨 FOR FUTURE DEVELOPERS:

When implementing mod compatibility:

1. **✅ DO:** Check feature state in real-time
2. **❌ DON'T:** Make assumptions based on mod presence alone
3. **✅ DO:** Use proper API calls that return current state
4. **❌ DON'T:** Cache static compatibility decisions

**Code Pattern:**
```java
// ❌ WRONG
if (hasOptiFine()) { /* assume OptiFine behavior */ }

// ✅ CORRECT
if (isOptiFineShaderActive()) { /* respond to current state */ }
```

This fix ensures MTS lighting works correctly in all scenarios while maintaining full Iris compatibility.