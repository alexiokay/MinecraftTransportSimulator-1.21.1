# MTS Texture/Model Rendering Issue Analysis

## Problem Summary
Content pack entities (planes, cars) display textures and models correctly when placed in the world, but appear as missing textures (purple/black cubes) in:
- Inventory slots
- Equipment slots
- JEI recipe displays
- Creative tabs

## Root Cause Analysis

### 1. Different Rendering Systems

**Entity Rendering (Works Correctly)**
- Uses custom MTS rendering pipeline via `InterfaceRender.java`
- Direct 3D model rendering with custom shaders
- Bypasses Minecraft's item model system entirely
- Textures loaded via `getTextureStream()` with multiple fallback strategies
- Path: Entity → `renderVertices()` → Custom shaders → Direct texture binding

**Item Rendering (Broken)**
- Relies on Minecraft's standard item rendering system
- Requires JSON model definitions and registered textures
- Uses ResourceManager and item model registry
- Path: Item → JSON model → Texture atlas → Standard rendering

### 2. Resource Loading Mechanisms

**Entity Texture Loading (`InterfaceRender.java:240-296`)**
```java
public InputStream getTextureStream(String name) {
    // Strategy 1: Main resource manager
    var resource = Minecraft.getInstance().getResourceManager().getResource(resourceLocation);

    // Strategy 2: Alternative locations
    ResourceLocation altLocation = ResourceLocation.fromNamespaceAndPath(domain, "textures/mcfont/" + ...);

    // Strategy 3: Direct mod resource access
    InputStream fallbackStream = InterfaceManager.coreInterface.getPackResource(name);

    // Strategy 4: Class resource loading
    InputStream classResource = this.getClass().getResourceAsStream(name);
}
```

**Item Resource Loading (`InterfaceEventsModelLoader.java:52-72`)**
```java
public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
    // PROBLEM: Only handles .png files, not .json models
    if (location.getPath().endsWith(".png")) {
        // Texture loading logic
    }
    // Missing: JSON model file handling
}
```

### 3. Missing Components

#### A. JSON Model Registration
- `InterfaceEventsModelLoader.PackResourcePack` only processes PNG files
- No handling for `.json` model files required by Minecraft's item rendering
- Item models default to missing/broken state

#### B. Resource Provider Integration
- `ContentPackResourceProvider.java` scans content pack JARs correctly
- Registers resources with ResourceManager
- BUT: Item rendering still fails due to model registration issues

#### C. Pack Resource Structure
Content packs follow these structures (`PackResourceLoader.java:31-44`):
- **DEFAULT**: `/assets/[packid]/models/item/[classification]/[name].json`
- **LAYERED**: `/assets/[packid]/models/item/[classification]/[subfolder]/[name].json`
- **MODULAR**: `/assets/[packid]/[classification]/[subfolder]/[name]_item.json`

### 4. Texture Caching Issues
- Entity rendering has sophisticated caching (`textureExistenceCache`, `textureStateCache`)
- Item rendering lacks similar caching mechanisms
- First-render delays may cause texture loading failures

## Technical Details

### File Locations
- **Entity Rendering**: `mcinterfaceneoforge1211/src/main/java/mcinterface1211/InterfaceRender.java`
- **Model Loading**: `mcinterfaceneoforge1211/src/main/java/mcinterface1211/InterfaceEventsModelLoader.java`
- **Resource Provider**: `mcinterfaceneoforge1211/src/main/java/mcinterface1211/ContentPackResourceProvider.java`
- **Item Registration**: `mcinterfaceneoforge1211/src/main/java/mcinterface1211/BuilderItem.java`

### Key Code Sections
1. **Texture Loading**: `InterfaceRender.getTextureStream()` (lines 240-296)
2. **Resource Processing**: `InterfaceEventsModelLoader.getResource()` (lines 52-72)
3. **Item Model Rendering**: `InterfaceRender.renderItemModel()` (lines 299-301)
4. **GUI Item Rendering**: `GUIComponentItem.render()` (lines 56-76)

## Solutions Required

### Immediate Fix
Extend `InterfaceEventsModelLoader.PackResourcePack.getResource()` to handle JSON models:

```java
// Change line 54 from:
if (location.getPath().endsWith(".png")) {

// To:
if (location.getPath().endsWith(".png") || location.getPath().endsWith(".json")) {
```

### Comprehensive Solution
1. **JSON Model Generation**: Auto-generate basic item models for content pack items
2. **Resource Registration**: Ensure all item models are properly registered with ResourceManager
3. **Fallback Models**: Implement fallback cube models for missing definitions
4. **Caching**: Add item model caching similar to entity texture caching

### Alternative Approaches
1. **Custom Item Renderer**: Implement custom item renderer that uses entity rendering system
2. **Model Overrides**: Override Minecraft's item model system for MTS items
3. **Dynamic Model Generation**: Generate JSON models at runtime from OBJ models

## Testing Verification
After implementing fixes, verify:
1. Content pack items display correctly in inventory
2. JEI recipes show proper item textures
3. Creative tab items render correctly
4. Equipment slots display items properly
5. No performance regression in entity rendering

## Notes
- Entity rendering works because it's completely custom and bypasses MC's item system
- Item rendering fails because it depends on MC's model registry which isn't populated
- The resource providers are working correctly - the issue is in model registration
- NeoForge 1.21.1 changes may have affected resource loading order/timing