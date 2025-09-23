# Vehicle Removal Synchronization Issue - NeoForge 1.21.1

**Status**: ✅ **FIXED**
**Priority**: Critical
**Affected Version**: NeoForge 1.21.1
**Date Discovered**: 2025-09-23
**Date Fixed**: 2025-09-23

## Issue Summary

When vehicles were removed using a wrench (sneak + left click), the vehicle item would drop and interaction would stop, but the vehicle texture/model would remain visible until the player exited and rejoined the world.

## Root Cause Analysis

### The Problem
**Client-server entity removal synchronization was broken in NeoForge 1.21.1**

1. **Server-side removal**: Worked correctly - MTS entity marked as `isValid = false`, MC wrapper discarded
2. **Client-side sync**: BROKEN - Client never received notification that entity was removed
3. **Result**: Client-side MTS entity remained `isValid = true` and continued rendering

### Why This Wasn't an Issue in 1.20.1

In 1.20.1, entity removal synchronization likely worked through one of these mechanisms:
- **Automatic MC entity sync**: Different/better entity sync in 1.20.1
- **LevelRendererMixin**: May have had sync logic that was lost when disabled
- **Different packet handling**: 1.20.1 may have had implicit entity removal packets

The 1.20.1 → 1.21.1 migration broke whatever mechanism was handling this synchronization.

## Evidence from Logs

### Server-Side (Working)
```
[Server thread] ENTITY REMOVE CALL: remove() called on EntityVehicleF_Physics UUID eafa16a6... isValid=true on SERVER
[Server thread] ENTITY REMOVE DEBUG: Setting isValid=false for EntityVehicleF_Physics UUID eafa16a6... on SERVER
[Server thread] ENTITY REMOVE DEBUG: Force discarding MC wrapper for EntityVehicleF_Physics UUID eafa16a6... on SERVER
```

### Client-Side (Missing)
```
[Render thread] BUILDER ENTITY TICK DEBUG: EntityVehicleF_Physics UUID eafa16a6... isValid=true side=CLIENT
```

**The same UUID shows `isValid=true` on client while `isValid=false` on server!**

## Investigation Journey

### Initial False Leads
1. **Rendering system**: Thought issue was with `doRenderCall()` or entity rendering
2. **Entity queue cleanup**: Tried cleaning `renderableEntities` queue
3. **BuilderEntityExisting**: Focused on MC wrapper entity removal
4. **LevelRendererMixin**: Tried re-enabling disabled mixin

### The Breakthrough
Added comprehensive debug logging and discovered that:
- ✅ Server-side removal was perfect
- ❌ Client-side entities never got removal notification
- ❌ Client entities continued with `isValid=true`

This revealed it was a **synchronization issue**, not a rendering issue.

## The Fix

### Created PacketEntityRemove System

1. **New Packet Class**: `PacketEntityRemove.java`
   ```java
   public class PacketEntityRemove extends APacketEntity<AEntityA_Base> {
       // Sends entity UUID from server to clients
       // Clients call entity.remove() when received
   }
   ```

2. **Packet Registration**: Added to `APacketBase.java` packet system

3. **Server-Side Integration**: Modified `WrapperWorld.removeEntity()`
   ```java
   if (!world.isClientSide) {
       InterfaceManager.packetInterface.sendToAllClients(new PacketEntityRemove(entity));
   }
   ```

4. **Client-Side Handling**: Packet received → `entity.remove()` called on client

### Result
- ✅ Server removes entity → Packet sent to clients
- ✅ Clients receive packet → Remove their entity copy
- ✅ Vehicle texture disappears immediately
- ✅ No need to exit/rejoin world

## Files Modified

### New Files
- `mccore/src/main/java/minecrafttransportsimulator/packets/instances/PacketEntityRemove.java`

### Modified Files
- `mccore/src/main/java/minecrafttransportsimulator/packets/components/APacketBase.java`
- `neoforge/src/main/java/mcinterface1211/WrapperWorld.java`

## Testing

### Before Fix
1. Spawn vehicle with wrench
2. Remove with wrench (sneak + left click)
3. ❌ Item drops, interaction stops, but texture remains
4. ❌ Must exit/rejoin world to see texture disappear

### After Fix
1. Spawn vehicle with wrench
2. Remove with wrench (sneak + left click)
3. ✅ Item drops, interaction stops, texture disappears immediately
4. ✅ No exit/rejoin needed

## Lessons Learned

### 1. Client-Server Sync is Critical
Entity synchronization between client and server is fundamental but easy to break during version migrations.

### 2. Debug Logging Strategy
Comprehensive logging across client/server revealed the real issue after many false leads:
```java
InterfaceManager.coreInterface.logError("DEBUG: " + entity.getClass().getSimpleName() + " UUID " + entity.uniqueUUID + " isValid=" + entity.isValid + " side=" + (world.isClient() ? "CLIENT" : "SERVER"));
```

### 3. Don't Assume Rendering Issues
What appeared to be a rendering problem was actually a data synchronization problem. The entities were still valid on client-side, so they continued rendering correctly.

### 4. Version Migration Gotchas
Moving from 1.20.1 → 1.21.1 likely broke implicit entity sync mechanisms that weren't obviously documented.

## Impact

### Before Fix
- **User Experience**: Confusing ghost vehicles requiring world restart
- **Multiplayer**: Likely worse on dedicated servers
- **Development**: Wasted time on rendering optimizations that weren't needed

### After Fix
- **User Experience**: Seamless vehicle removal as expected
- **Multiplayer**: Proper sync across all clients
- **Maintainability**: Explicit packet-based sync is more reliable than implicit MC mechanisms

## Future Prevention

1. **Entity Lifecycle Testing**: Always test entity creation/removal across client-server
2. **Sync Audits**: When migrating MC versions, audit all client-server sync mechanisms
3. **Explicit Over Implicit**: Prefer explicit packet-based sync over relying on MC internals

## Related Issues

This fix may resolve other entity synchronization issues in the codebase. Any entity removal that seemed to "stick around" on clients might have had the same root cause.