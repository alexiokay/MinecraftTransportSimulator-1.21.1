# Entity Disappearance Analysis - Critical Issue

## The Problem
Vehicles and MTS entities randomly disappear from the world, especially after:
- Code changes
- World reload/rejoin
- Sometimes spontaneously during gameplay
- Player gets stuck in place unable to move

## Root Causes Identified

### 1. **RACE CONDITION: Entity Registration vs World Loading**

**Location**: `InterfaceLoader.java:275-297` vs `BuilderEntityExisting.java:140-168`

**The Issue**:
- Entity factories are registered during `FMLConstructModEvent` (mod construction)
- World data loading happens when players join worlds
- **There's no guarantee the entityMap is fully populated when entities try to load from NBT**

**Evidence**:
```java
// BuilderEntityExisting.java:150-153
if (entityMap.get(entityId) == null) {
    InterfaceManager.coreInterface.logError("ENTITY DEBUG: Entity factory not found for ID: '" + entityId + "'");
    InterfaceManager.coreInterface.logError("ENTITY DEBUG: This entity will be skipped");
    return; // ENTITY DISAPPEARS HERE!
}
```

### 2. **Pack Loading Order Dependencies**

**Location**: `InterfaceLoader.java:287-291`

**The Issue**:
```java
for (AItemPack<?> packItem : PackParser.getAllPackItems()) {
    if (packItem instanceof IItemEntityProvider) {
        ((IItemEntityProvider) packItem).registerEntities(BuilderEntityExisting.entityMap);
    }
}
```

- If `PackParser.getAllPackItems()` hasn't finished loading all packs
- Or if packs load in different order between sessions
- Entity factories won't be registered → entities can't be restored

### 3. **NBT Data Corruption/Mismatch**

**Location**: `BuilderEntityExisting.java:144-166`

**Symptoms**:
- `lastLoadedNBT.getString("entityid")` returns ID that doesn't exist in entityMap
- Pack changes or updates change entity class names
- Old save data references entities that no longer exist

### 4. **Exception Handling Issues**

**Location**: `BuilderEntityExisting.java:163-167`

```java
} catch (Exception e) {
    InterfaceManager.coreInterface.logError("Failed to load entity on builder from saved NBT.  Did a pack change?");
    InterfaceManager.coreInterface.logError(e.getMessage());
    discard(); // ENTITY DISAPPEARS!
}
```

**Problem**: Any exception during entity loading causes immediate entity removal

### 5. **World Data Save/Load Synchronization**

**Location**: `WrapperWorld.java:157-189`

**Issues**:
- World data loading can fail silently
- Empty NBT data causes entities to not be restored
- Save data corruption leads to entity loss

## Specific Failure Scenarios

### Scenario A: Code Changes
1. You change entity-related code
2. Entity class names or structure changes
3. Old save data becomes incompatible
4. Entities fail to load → disappear

### Scenario B: Pack Loading Race
1. World loads before all packs are fully parsed
2. entityMap is incomplete
3. Entity restoration fails
4. Player stuck in vehicle that no longer exists

### Scenario C: NBT Corruption
1. Save/load process gets interrupted
2. NBT data becomes corrupted
3. Entity restoration throws exception
4. All entities in affected chunks disappear

## The "Player Stuck" Issue

When vehicles disappear but player is still "riding":
- Vehicle entity is gone from world
- Player's ridingEntity reference still exists (client-side)
- Player can't move because they think they're in a vehicle
- No vehicle to process player input

## Solutions Required

### 1. **Delayed Entity Loading**
- Don't attempt entity restoration until ALL packs are loaded
- Add synchronization point after pack parsing completes
- Queue entity loading operations until system is ready

### 2. **Entity Factory Validation**
```java
// Add before entity registration loop
while (!PackParser.areAllPacksLoaded()) {
    Thread.sleep(100); // Wait for packs to finish loading
}
```

### 3. **Better Error Recovery**
```java
// Instead of immediate discard(), retry mechanism:
private int loadRetries = 0;
private static final int MAX_RETRIES = 10;

if (entityMap.get(entityId) == null && loadRetries < MAX_RETRIES) {
    loadRetries++;
    // Defer loading to next tick
    return;
}
```

### 4. **Entity Validation Before Save**
- Validate entity data before writing to NBT
- Add entity ID validation
- Checksum verification for critical entity data

### 5. **Emergency Recovery System**
- If entity loading fails, keep entity data in memory
- Allow manual entity recovery commands
- Backup entity positions/data separately

## Immediate Debug Actions

1. **Monitor entityMap Population**:
   - Log entityMap size during different loading phases
   - Ensure all expected entities are registered

2. **Track Loading Order**:
   - Log when packs start/finish loading
   - Log when entity factories are registered
   - Log when world data requests entity restoration

3. **NBT Data Validation**:
   - Verify entity NBT data before restoration attempts
   - Log entity IDs that fail to match entityMap

## Priority Fix

**HIGHEST PRIORITY**: Fix the race condition by ensuring entity restoration only happens AFTER all entity factories are registered.

This single issue is likely causing 80%+ of the disappearance problems.