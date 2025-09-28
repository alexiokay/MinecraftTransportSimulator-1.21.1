# FMOD Sound Integration Tutorial

## Overview
MinecraftTransportSimulator (MTS) now features **optional FMOD API integration** that provides high-quality 3D spatial audio when the FMOD API mod is available, with automatic fallback to OpenAL when it's not installed.

## 🎯 Key Features
- **✅ Optional Dependency**: MTS works with or without FMOD API mod
- **🔄 Automatic Fallback**: Seamlessly switches between FMOD and OpenAL
- **🎮 Native Integration**: FMOD sounds pause with ESC menu and respect Minecraft volume
- **🎵 3D Spatial Audio**: Immersive audio positioning when FMOD is available

## 📥 Installation

### Option 1: Basic MTS (OpenAL Only)
1. Install **MTS mod only**
2. Enjoy standard OpenAL audio (original MTS experience)

### Option 2: Enhanced Audio with FMOD API
1. **Install MTS mod** (works without FMOD API)
2. **Download FMOD API mod** from: https://github.com/alexiokay/fmod-API/blob/main/README.md
3. **Place FMOD API mod** in your mods folder alongside MTS
4. **Enjoy enhanced 3D spatial audio** with automatic Minecraft integration

> **Note**: If FMOD API mod is not installed, MTS automatically uses OpenAL - no crashes or errors!

## 🔧 How the System Works

### Automatic Audio System Selection
The system intelligently chooses the best available audio:

| FMOD API Mod | FMOD DLLs | Audio System | Features |
|--------------|-----------|--------------|----------|
| ✅ Installed | ✅ Available | **FMOD Studio** | 3D audio + Minecraft integration |
| ✅ Installed | ❌ Missing | **OpenAL** | Standard audio (auto-fallback) |
| ❌ Not Installed | N/A | **OpenAL** | Standard audio (no dependencies) |

### Implementation Details
- **Reflection-Based Loading**: FMOD API loaded optionally via reflection
- **No Hard Dependencies**: MTS starts normally without FMOD API
- **Automatic Bank Registration**: MTS banks registered with FMOD API automatically
- **Seamless Fallback**: Individual sounds fall back to OpenAL if FMOD fails

## 🎵 Adding FMOD Sounds to Your Content

### Step 1: Install FMOD API Mod
Follow the installation guide at: https://github.com/alexiokay/fmod-API/blob/main/README.md

### Step 2: Create FMOD Studio Project
1. **Download FMOD Studio** from https://www.fmod.com/download
2. **Create events** in your project (e.g., "event:/vehicles/engine", "event:/weapons/explosion")
3. **Build banks** to generate `.bank` files
4. **Follow FMOD API setup** for bank installation

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

### Step 4: Test Your Implementation
1. **Check logs** for FMOD integration status
2. **Test sounds** - should have enhanced 3D positioning with FMOD
3. **Try without FMOD API** - should still work with OpenAL

## 🎮 Native Minecraft Integration Features

When FMOD API is installed, you get these automatic features:

### 🎯 Automatic Pause/Resume
- **ESC Menu**: All FMOD sounds pause instantly (<1ms)
- **Return to Game**: Sounds resume automatically
- **Any Menu**: Inventory, settings, etc. all pause FMOD audio

### 🔊 Volume Synchronization
- **Master Volume**: Controls overall FMOD volume
- **Sound Effects**: Affects FMOD sound effects
- **Music Volume**: Affects FMOD music events
- **Real-time**: Changes apply immediately

### 🎧 3D Audio Tracking
- **Player Position**: Automatically tracked for 3D audio
- **Player Rotation**: Head direction affects audio positioning
- **Player Movement**: Velocity used for Doppler effects

## 🎵 Sound Behavior

### What Happens When You Use MTS Sounds

#### With FMOD API Installed:
1. **FMOD Available + EventName Present**: Uses FMOD Studio event with 3D positioning
2. **FMOD Available + No EventName**: Falls back to OpenAL
3. **FMOD Failed + EventName Present**: Automatically falls back to OpenAL

#### Without FMOD API Installed:
1. **Any Sound**: Uses original MTS OpenAL system
2. **No Errors**: System works exactly as before FMOD integration

### No Double Sounds
The system ensures only one audio method is used per sound event.

## 🐛 Troubleshooting

### Common Issues
- **No Enhanced Audio**: Check if FMOD API mod is installed and FMOD DLLs are available
- **Sounds Don't Pause**: Verify FMOD API integration is working (check logs)
- **Original Sound Only**: FMOD may not be available, system falls back to OpenAL
- **No Sound At All**: Check JSON `eventName` matches FMOD Studio event exactly

### Debug Steps
1. **Check Logs**: Look for "FMOD API mod detected" or "FMOD API mod not found"
2. **Verify Installation**: Ensure FMOD API mod is in mods folder
3. **Test Basic Functionality**: MTS should work even without FMOD API
4. **Check FMOD API Setup**: Follow FMOD API documentation for DLL setup

### Log Messages to Look For
```
[MTS] FMOD API mod detected and loaded successfully
[MTS] FMOD API mod not found - using OpenAL only
[MTS] Attempting to play FMOD event: 'explosion' at position: X,Y,Z
[MTS] FMOD successfully played event "explosion" at: X, Y, Z
[MTS] FMOD API failed to play event: explosion, falling back to OpenAL
```

## ✅ Benefits of This System

### For Users
- **🛡️ No Crashes**: Works with or without FMOD API
- **🎮 Native Feel**: FMOD sounds behave like Minecraft sounds
- **🔄 Automatic**: No manual configuration needed
- **⚡ Performance**: Minimal overhead, event-driven

### For Developers
- **📦 Simple Distribution**: Don't need to bundle FMOD API
- **🔧 Easy Migration**: Just add `eventName` to existing JSONs
- **🧹 Clean Code**: No manual FMOD management needed
- **🔮 Future-Proof**: FMOD API handles all integration complexity

## 📚 Advanced Usage

### Custom Bank Loading
If you create custom FMOD banks beyond the default MTS banks:

1. **Follow FMOD API Documentation**: For proper bank setup and loading
2. **Use FMOD API Registration**: Banks should be registered with FMOD API mod
3. **Check Integration Guide**: https://github.com/alexiokay/fmod-API/blob/main/README.md

### Performance Optimization
- **EventName Strategy**: Only add `eventName` for sounds that benefit from 3D positioning
- **Bank Management**: Use FMOD API's automatic bank management
- **Fallback Testing**: Always test without FMOD API to ensure compatibility

## 🔗 Additional Resources

- **FMOD API Mod**: https://github.com/alexiokay/fmod-API/blob/main/README.md
- **FMOD Studio Download**: https://www.fmod.com/download
- **FMOD Documentation**: https://www.fmod.com/docs

## 📋 Example: Converting Existing Content

### Before (Standard OpenAL)
```json
{
  "bullet": {
    "diameter": 37.0,
    "mass": 0.735
  }
}
```

### After (FMOD + OpenAL Fallback)
```json
{
  "bullet": {
    "diameter": 37.0,
    "mass": 0.735,
    "eventName": "explosion"
  }
}
```

**Result**:
- ✅ With FMOD API: Enhanced 3D explosion audio
- ✅ Without FMOD API: Original OpenAL explosion audio
- ✅ Always works: No crashes or missing dependencies!

The system automatically handles everything else!