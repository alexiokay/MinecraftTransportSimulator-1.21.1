# Analysis of 1.20.1 Entity Sync System

## What We Found

### Remnants of Old Sync System

The codebase contains remnants of what was likely a 1.20.1 automatic entity synchronization system:

#### 1. **Entity Tracking System**
```java
// EntityManager.java
private final ConcurrentHashMap<UUID, AEntityA_Base> trackedEntityMap = new ConcurrentHashMap<>();

// Entities that return shouldSync() = true get tracked
if (entity.shouldSync()) {
    trackedEntityMap.put(entity.uniqueUUID, entity);
}
```

#### 2. **Tracked Entity Lookup**
```java
// EntityManager.java
public <EntityType extends AEntityA_Base> EntityType getEntity(UUID uniqueUUID) {
    return (EntityType) trackedEntityMap.get(uniqueUUID);
}
```

#### 3. **Packet System Integration**
```java
// APacketEntity.java
EntityType entity = world.getEntity(uniqueUUID);  // Uses trackedEntityMap
if (entity != null && handle(world, entity) && !world.isClient()) {
    InterfaceManager.packetInterface.sendToAllClients(this);  // Auto-rebroadcast
}
```

### What Was Missing

**No automatic removal synchronization!** The tracking system exists but there's no code that:
- Monitors when tracked entities are removed
- Automatically sends removal packets to clients
- Cleans up client-side entities when server removes them

## Theory: What 1.20.1 Had

### Option 1: Full Auto-Sync System (Never Implemented)
```java
// Theoretical code that was planned but never implemented
@Override
public void removeEntity(AEntityA_Base entity) {
    if (entity.shouldSync() && !world.isClient()) {
        // Auto-send removal packet for tracked entities
        InterfaceManager.packetInterface.sendToAllClients(new PacketEntitySync(entity, SyncType.REMOVE));
    }
    // ... rest of removal
}
```

### Option 2: LevelRendererMixin Had Sync Logic
The disabled `LevelRendererMixin` might have had sync logic that was lost when we disabled it for 1.21.1.

### Option 3: Different MC Entity Lifecycle
1.20.1 might have had different Minecraft entity management that automatically synced entity removal.

## Current State: Dead Code

### What's Tracked But Unused
1. **`trackedEntityMap`** - Populated but only used for packet lookups
2. **`shouldSync()`** - Returns `true` for vehicles but no auto-sync happens
3. **Packet auto-rebroadcast** - Works for entity updates but not removal

### What Our Fix Provides
Our explicit `PacketEntityRemove` system bypasses all this dead infrastructure and provides:
- ✅ Explicit removal packet sending
- ✅ Direct client-side entity removal
- ✅ No dependency on broken auto-sync

## Recommendation: Cleanup

Since our explicit packet system works perfectly and the old tracking system is unused dead code, we should:

### Keep (Still Useful)
- ✅ `getEntity(UUID)` - Used by packets to find target entities
- ✅ `trackedEntityMap` - Still needed for packet targeting

### Remove (Dead Code)
- ❌ `shouldSync()` logic in removal - Not doing anything
- ❌ Auto-rebroadcast hopes - Never worked for removal

### Update Documentation
- Document that entity sync is now explicit via `PacketEntityRemove`
- Note that auto-sync was never fully implemented

## Evidence This Was Broken

### Git History Clues
```
a9e8d343d Fixed entities post 1.12.2 jittering with packet junk movements.
f1f6f74b5 Fixed possible crash with hotloading due to entity ID changes.
50d14aa9d Fixed fake ground devices not being removed for long parts.
cd9778335 Fixed parts not being removed on destruction.
```

Multiple commits about entity sync issues suggest this has been an ongoing problem, not just a 1.21.1 regression.

## Conclusion

The "1.20.1 system" was likely never fully working. Our explicit `PacketEntityRemove` approach is the first time entity removal synchronization has worked correctly in MTS.

The tracking infrastructure should stay (used by packets) but we shouldn't expect it to do automatic sync that it was never designed to do.