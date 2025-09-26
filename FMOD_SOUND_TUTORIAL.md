# FMOD Sound Integration Tutorial

## Overview
MinecraftTransportSimulator now features a smart sound system that automatically uses FMOD Studio events when available, with seamless fallback to original MTS sounds when FMOD is not initialized.

## How the Smart Sound System Works

### Automatic Sound Selection
The system automatically chooses the best available sound method:
1. **FMOD Available + EventName Present**: Uses FMOD Studio event
2. **FMOD Not Available OR No EventName**: Falls back to original MTS sounds
3. **Real-time Detection**: Checks FMOD status dynamically, no hardcoding required

### Implementation Details
- **Location**: `InterfaceSound.java:playSmartSound()` method
- **Integration**: Automatically used in `EntityBullet.java` for bullet impacts
- **Fallback Logic**: Built into existing `FMODPlaySoundEvent()` method
- **No Conflicts**: Original sounds are replaced, not layered

## Adding FMOD Sounds to Your Content

### Step 1: Create FMOD Studio Project
1. Open FMOD Studio
2. Create events in your project (e.g., "explosion", "engine_start", "horn")
3. Build banks to generate `.bank` files

### Step 2: Place Bank Files
Copy your built bank files to:
```
neoforge/run/fmod/
├── Master.bank
├── Master.strings.bank
└── YourCustom.bank
```

### Step 3: Add EventName to JSON
For bullets, add `eventName` to the bullet section:
```json
{
  "bullet": {
    "diameter": 37.0,
    "mass": 0.735,
    "eventName": "explosion"
  }
}
```

For other entities, add `eventName` to existing sound definitions:
```json
{
  "sounds": [
    {
      "name": "engine_startup",
      "eventName": "engine_start"
    }
  ]
}
```

### Step 4: Load Custom Banks (if needed)
If using custom banks beyond Master.bank, modify `InterfaceSound.java:FMODSystemInit()`:
```java
// Add after existing bank loading
FMODLoadBank("YourCustom.bank");
```

## Sound Behavior

### What Happens When You Shoot
1. **With FMOD + EventName**: Only FMOD event plays (original sound replaced)
2. **Without FMOD or EventName**: Original MTS sound plays
3. **No Double Sounds**: System ensures only one sound method is used

### Testing Your Implementation
1. Check logs for FMOD initialization: `Successfully initialized FMOD`
2. Check logs for event triggering: Look for FMOD event calls
3. Listen for sound replacement (FMOD should sound different from original)

## Troubleshooting

### Common Issues
- **No Sound**: Check if `eventName` matches exactly with FMOD Studio event name
- **Original Sound Playing**: FMOD may not be initialized or event not found
- **Bank Loading Errors**: Ensure bank files are in correct directory (`neoforge/run/fmod/`)

### Debug Steps
1. Check FMOD initialization in logs
2. Verify bank files exist and are accessible
3. Confirm eventName spelling in JSON matches FMOD Studio
4. Test with simple events first (like "explosion")

## Benefits of This System
- **Backward Compatible**: Content without `eventName` continues working
- **No Hardcoding**: Automatically detects FMOD availability
- **Easy Migration**: Just add `eventName` to existing JSON definitions
- **Performance**: Only initializes FMOD when needed
- **Flexible**: Works with any FMOD Studio event structure

## Example: Converting Existing Bullet
**Before** (original MTS sound only):
```json
{
  "bullet": {
    "diameter": 37.0,
    "mass": 0.735
  }
}
```

**After** (smart FMOD + fallback):
```json
{
  "bullet": {
    "diameter": 37.0,
    "mass": 0.735,
    "eventName": "explosion"
  }
}
```

The system automatically handles the rest!