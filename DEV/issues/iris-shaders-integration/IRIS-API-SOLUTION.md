# Iris API Integration - SOLUTION FOUND

**Date:** September 25, 2025
**Status:** ✅ FULLY WORKING - Real-time shader pack detection implemented

## The Problem

MTS needed to detect when Iris shader packs are actively enabled to:
1. **Disable beam rendering** when shaders are active (to prevent conflicts)
2. **Enable beam rendering** when shaders are disabled
3. **Detect changes in real-time** when users toggle shaders in-game

## Initial Attempts (Failed)

### ❌ Wrong API Usage
```java
// This FAILED - trying to call instance method as static
Object result = irisApi.getMethod("isShaderPackInUse").invoke(null);
```

**Issues:**
- `isShaderPackInUse()` is NOT a static method
- Requires getting API instance first
- Silent failure with no error messages

## ✅ WORKING SOLUTION

### ⚠️ CRITICAL: Correct API Usage Pattern
**This is the ONLY way that works - do NOT use static method calls!**

```java
// ✅ CORRECT METHOD - Step by step:

// 1. Load the API class using reflection
Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");

// 2. Get the API instance (this IS a static method)
Object apiInstance = irisApi.getMethod("getInstance").invoke(null);

// 3. Call isShaderPackInUse() on the INSTANCE (NOT static!)
Object result = irisApi.getMethod("isShaderPackInUse").invoke(apiInstance);

// 4. Cast and return the Boolean result
return (Boolean) result;
```

### ❌ COMMON MISTAKES TO AVOID:
```java
// ❌ WRONG - This will fail silently:
Object result = irisApi.getMethod("isShaderPackInUse").invoke(null);

// ❌ WRONG - Trying to call directly without reflection in cross-mod scenario:
IrisApi.getInstance().isShaderPackInUse(); // ClassNotFoundException

// ❌ WRONG - Wrong package name:
Class.forName("net.iris.api.IrisApi"); // ClassNotFoundException
```

### Complete Implementation (`ModCompatibility.java`)
```java
public static boolean areShadersEnabled() {
    if (!hasShaderMod()) {
        return false; // No Iris mod installed
    }

    try {
        if (ModList.get().isLoaded("iris")) {
            Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object apiInstance = irisApi.getMethod("getInstance").invoke(null);
            Object result = irisApi.getMethod("isShaderPackInUse").invoke(apiInstance);

            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        }
    } catch (Exception e) {
        // API not available, assume disabled to allow beams
        return false;
    }

    return false; // Default: beams enabled
}
```

## Key Technical Discoveries

### 1. **Iris API is Instance-Based (CRITICAL KNOWLEDGE)**
- ⚠️ **`isShaderPackInUse()` is NOT static** - this was the main bug!
- ✅ Must call `IrisApi.getInstance()` first to get an instance
- ✅ Then call `isShaderPackInUse()` on that instance
- ❌ Calling `invoke(null)` on instance methods fails silently

### 2. **Real-Time Detection Works**
- API returns `false` when no shader pack loaded
- API returns `true` when shader pack is active
- Changes immediately when user toggles shaders in Iris GUI

### 3. **Mod ID Confirmation**
- Iris mod ID is correctly `"iris"` (not `"irisshaders"`)
- `ModList.get().isLoaded("iris")` works perfectly

### 4. **API Class Location**
- Full class path: `net.irisshaders.iris.api.v0.IrisApi`
- Available in Iris version: `1.8.12-snapshot+mc1.21.1-local`

## Test Results

### ✅ Shader Pack Disabled
```
IRIS DEBUG: Iris API result = false
ModCompatibility.areShadersEnabled() returned: false
shouldDisableBeams=false
→ Beams render normally
```

### ✅ Shader Pack Enabled
```
IRIS DEBUG: Iris API result = true
ModCompatibility.areShadersEnabled() returned: true
shouldDisableBeams=true
→ Beams disabled (prevented from rendering)
```

## Integration Points

### 1. **Beam Rendering Logic** (`RenderableModelObject.java:326`)
```java
if (beamRenderable != null && ConfigSystem.client.renderingSettings.renderBeams.value &&
    !InterfaceManager.renderingInterface.shouldDisableBeamsForShaderCompatibility()) {
    // Render beams only when shaders are disabled
}
```

### 2. **Interface Implementation** (`InterfaceRender.java:738`)
```java
@Override
public boolean shouldDisableBeamsForShaderCompatibility() {
    return ModCompatibility.areShadersEnabled();
}
```

## Performance Considerations

### ✅ Efficient Implementation
- **Cached mod detection**: `hasShaderMod()` checks mod presence once
- **Reflection caching**: Could be optimized to cache Method objects
- **Minimal overhead**: Only called during beam rendering (when lights are on)

### Future Optimization (Optional)
```java
// Cache reflection objects for better performance
private static Method getInstanceMethod = null;
private static Method isShaderPackInUseMethod = null;
```

## 🎯 FOR FUTURE DEVELOPERS

**If you need to integrate with Iris API, follow this EXACT pattern:**

### Template Code:
```java
public static boolean isIrisShaderActive() {
    try {
        // Step 1: Load Iris API class
        Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");

        // Step 2: Get API instance (static method)
        Object apiInstance = irisApiClass.getMethod("getInstance").invoke(null);

        // Step 3: Call instance method
        Object result = irisApiClass.getMethod("isShaderPackInUse").invoke(apiInstance);

        // Step 4: Cast and return
        return result instanceof Boolean ? (Boolean) result : false;

    } catch (Exception e) {
        // Iris not available or API changed
        return false;
    }
}
```

### Essential Details:
- **Package**: `net.irisshaders.iris.api.v0.IrisApi`
- **Mod ID**: `"iris"` (for `ModList.get().isLoaded("iris")`)
- **Method Pattern**: Instance-based, not static
- **Return Type**: `Boolean` (true = shader active, false = no shader)

## Conclusion

The Iris API integration is **fully functional** and provides real-time shader pack detection. This solves the core compatibility issue where MTS beam rendering conflicted with Iris shader packs.

**Next Step:** Fix the remaining black beam appearance issue when shaders are disabled.