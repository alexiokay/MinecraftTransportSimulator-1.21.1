# Audio ListenerMixin Issue - NeoForge 1.21.1 Migration

## Issue Description
The `ListenerMixin` that works in Forge 1.20.1 is failing in NeoForge 1.21.1 due to changes in the audio API.

## Error Message
```
Critical injection failure: @Inject annotation on inject_setListenerPosition could not find any targets matching 'setListenerPosition' in com/mojang/blaze3d/audio/Listener
```

## Current Status
- **Status**: DISABLED
- **Location**: `mcinterface1211.mixin.client.ListenerMixin`
- **Disabled in**: `mts.mixins.json` (moved to `client_disabled` section)

## Problem Details

### Methods That Don't Exist in 1.21.1:
1. `setListenerPosition(Vec3)` - Used to set the audio listener's position in 3D space
2. `setListenerOrientation(Vector3f, Vector3f)` - Used to set the audio listener's orientation

### What The Mixin Does:
The ListenerMixin is used to adjust audio listener position and orientation when riding in a vehicle, particularly to support roll rotation which Minecraft doesn't normally handle. It directly calls OpenAL functions to override the listener properties.

## Investigation Notes

1. The `com.mojang.blaze3d.audio.Listener` class exists in 1.21.1 but the method signatures have changed
2. The audio system in Minecraft 1.21.1 might have moved to a different architecture
3. Need to find the replacement methods or new API for controlling audio listener properties

## TODO: Fix Requirements

1. **Research the new audio API in NeoForge 1.21.1**
   - Find what replaced `setListenerPosition` and `setListenerOrientation`
   - Check if the Listener class still handles these operations or if it moved elsewhere
   - Look for new event hooks or injection points for audio customization

2. **Update the Mixin**
   - Either update method names to match 1.21.1 API
   - Or find alternative injection points
   - Or potentially use a different approach (events, capabilities, etc.)

3. **Test the fix**
   - Ensure audio position/orientation works correctly when riding vehicles
   - Verify roll rotation audio adjustment works
   - Check that OpenAL calls are still compatible

## Impact
Without this Mixin, audio positioning when riding vehicles (especially with roll rotation) may not work correctly. The audio will not properly reflect the player's orientation in the vehicle.

## Original 1.20.1 Code Reference
The mixin injects into:
- `setListenerPosition(Vec3 pPosition)`
- `setListenerOrientation(Vector3f pClientViewVector, Vector3f pViewVectorRaised)`

And uses OpenAL directly:
- `AL10.alListener3f(AL10.AL_POSITION, ...)`
- `AL10.alListenerfv(AL10.AL_ORIENTATION, ...)`