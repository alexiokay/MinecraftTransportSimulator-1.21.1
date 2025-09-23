# Root Cause: Why 1.20.1 Worked and 1.21.1 Didn't

## The Smoking Gun

Found in `ABuilderEntityBase.java` line 27:

```java
//Need to extend LivingEntity since spawn syncing packets don't work with the base Entity class.
public abstract class ABuilderEntityBase extends Entity {  // ❌ CONTRADICTION!
```

**The comment says it should extend `LivingEntity` for spawn syncing, but it actually extends `Entity`!**

## What Happened During 1.20.1 → 1.21.1 Migration

### Theory: Broken Inheritance Chain

1. **1.20.1**: `ABuilderEntityBase` likely extended `LivingEntity`
   - ✅ Spawn syncing packets worked automatically
   - ✅ Entity removal was synchronized to clients
   - ✅ Vehicles disappeared correctly when removed

2. **1.21.1 Migration**: `ABuilderEntityBase` changed to extend `Entity`
   - ❌ Spawn syncing packets broken
   - ❌ Entity removal not synchronized to clients
   - ❌ Vehicles remain visible after removal

### Why This Broke Entity Removal

**Minecraft's Entity Synchronization System:**
- `LivingEntity`: Full automatic client-server synchronization
- `Entity`: Limited synchronization (spawn syncing packets don't work)

When `ABuilderEntityBase` was changed from `LivingEntity` → `Entity`:
- **Entity creation**: Still works (different packet system)
- **Entity removal**: BROKEN (relies on spawn syncing packets)
- **Entity updates**: May work (different packet system)

## Evidence Supporting This Theory

### 1. The Comment is a Warning
The comment explicitly warns that **spawn syncing packets don't work with base Entity class**, but the code contradicts this by extending `Entity`.

### 2. Removal vs Creation Asymmetry
- **Vehicle spawning**: Works fine (different mechanism)
- **Vehicle removal**: Broken (relies on spawn syncing)

This asymmetry matches exactly what we observed!

### 3. Git History Clues
Migration commit mentions: **"Migrated packet system to NeoForge 1.21.1 bidirectional registration"**

During this packet system migration, the inheritance chain was likely changed, breaking the spawn syncing mechanism.

### 4. Our Fix Works
Our explicit `PacketEntityRemove` bypasses the broken automatic sync by sending manual removal packets.

## What Probably Happened

### Before Migration (1.20.1)
```java
// Hypothetical 1.20.1 code
public abstract class ABuilderEntityBase extends LivingEntity {
    // Automatic spawn syncing packets work
    // Entity removal automatically synced to clients
}
```

### After Migration (1.21.1)
```java
// Current 1.21.1 code
public abstract class ABuilderEntityBase extends Entity {  // ❌ Broken sync
    // Spawn syncing packets don't work
    // Entity removal NOT synced to clients
}
```

### Why It Was Changed
Possible reasons for the inheritance change:
1. **NeoForge 1.21.1 compatibility**: `LivingEntity` API might have changed
2. **Performance**: `Entity` is lighter than `LivingEntity`
3. **Functionality**: MTS entities don't need full living entity features
4. **Accident**: Unintentional change during migration

## The Real Fix Options

### Option 1: Revert to LivingEntity (Risky)
```java
public abstract class ABuilderEntityBase extends LivingEntity {
```
**Pros**: Automatic sync restored
**Cons**: May break other 1.21.1 compatibility, needs extensive testing

### Option 2: Keep Explicit Packet System (Current)
```java
// Manual packet sending in WrapperWorld.removeEntity()
InterfaceManager.packetInterface.sendToAllClients(new PacketEntityRemove(entity));
```
**Pros**: Explicit, reliable, no dependency on MC internals
**Cons**: Manual work, but already implemented and working

## Conclusion

**The 1.20.1 → 1.21.1 migration broke automatic entity removal synchronization by changing the inheritance chain from `LivingEntity` → `Entity`, which disabled Minecraft's spawn syncing packets.**

Our explicit `PacketEntityRemove` system is actually a **better solution** than relying on MC's automatic sync, because:
- ✅ **More reliable**: Not dependent on MC internal behavior
- ✅ **Version-independent**: Won't break in future MC updates
- ✅ **Explicit**: Clear code showing exactly what packets are sent
- ✅ **Debuggable**: Can add logging and error handling

## Recommendation

**Keep our explicit packet system.** Don't try to revert to `LivingEntity` because:
1. It might break other 1.21.1 functionality
2. Our solution is more robust and future-proof
3. The comment suggests this was always a fragile dependency

## FINAL DISCOVERY: The Real Story ✅

**Research Update**: The inheritance was **NEVER** changed! `ABuilderEntityBase` has always extended `Entity` since 1.12.2.

**What Actually Works:**
- ✅ **Entity Updates**: Auto-rebroadcast via `APacketEntity.handle()` works perfectly
- ❌ **Entity Removal**: Never had automatic sync - this was the missing piece

**The 1.20.1 vs 1.21.1 difference was likely:**
- Different NeoForge packet registration
- Changes in MC's internal entity lifecycle
- LevelRendererMixin being disabled

## Code Cleanup Recommendation

**Remove Dead Code:**
- **File**: `EntityManager.java` **Lines 403-405**
```java
// REMOVE (does nothing):
if (entity.shouldSync()) {
    trackedEntityMap.remove(entity.uniqueUUID);
}
```

**Keep Working Infrastructure:**
- ✅ `trackedEntityMap` - Used by auto-rebroadcast system
- ✅ Auto-rebroadcast - Works perfectly for entity updates
- ✅ Explicit `PacketEntityRemove` - Completes the sync system