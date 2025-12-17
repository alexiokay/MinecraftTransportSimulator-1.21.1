# MTS (Minecraft Transport Simulator) HUD System Analysis

This document describes how the MTS mod implements its HUD/overlay system for displaying item status (ammo, fuel, etc.) on screen.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Display Systems](#display-systems)
3. [Key Source Files](#key-source-files)
4. [GUIOverlay - Hand-Held Items](#guioverlay---hand-held-items)
5. [GUIHUD - Vehicle Instruments](#guihud---vehicle-instruments)
6. [Instrument System](#instrument-system)
7. [ComputedVariable System](#computedvariable-system)
8. [Gun Variables Reference](#gun-variables-reference)
9. [JSON Configuration](#json-configuration)
10. [Data Flow Summary](#data-flow-summary)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                      MTS HUD ARCHITECTURE                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  LAYER 1: GUIOverlay (Always visible overlay)                  │  │
│  │  File: guis/instances/GUIOverlay.java                          │  │
│  │  Shows: Hand-held gun info, mouseover labels, scanner display  │  │
│  │  Position: Top-right corner of screen                          │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                              │                                       │
│                              ▼                                       │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  LAYER 2: GUIHUD (Vehicle HUD)                                 │  │
│  │  File: guis/instances/GUIHUD.java                              │  │
│  │  Shows: Instruments, health, custom keybinds                   │  │
│  │  Position: Bottom of screen (400x140 pixels)                   │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                              │                                       │
│                              ▼                                       │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  LAYER 3: Instruments (Texture-based gauges)                   │  │
│  │  File: rendering/RenderInstrument.java                         │  │
│  │  Shows: Fuel, speed, ammo, RPM, etc. via animated textures     │  │
│  │  Position: Configurable via JSON (hudX, hudY, hudScale)        │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                              │                                       │
│                              ▼                                       │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  LAYER 4: ComputedVariable System                              │  │
│  │  File: baseclasses/ComputedVariable.java                       │  │
│  │  Provides: Dynamic values (gun_ammo_count, fuel, speed, etc.)  │  │
│  │  Used by: All rendering layers for real-time data              │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Display Systems

MTS uses three distinct systems for displaying item/vehicle status:

### 1. Text Labels (GUIOverlay)
Simple text displayed in the corner for hand-held items:
```
Gun:Fire Extinguisher Loaded:500
```

### 2. Instrument Gauges (GUIHUD + RenderInstrument)
Texture-based animated displays showing values like:
- Fuel level (needle rotation)
- Speed (digital/analog)
- Ammo count (bar/counter)

### 3. Model-Based Indicators
3D model elements animated based on variables:
```json
{
  "objectName": "ammo_gauge",
  "animations": [
    {
      "animationType": "rotation",
      "variable": "gun_ammo_count",
      "axis": [0.18, 0, 0]
    }
  ]
}
```

---

## Key Source Files

| File | Purpose |
|------|---------|
| `guis/instances/GUIOverlay.java` | Always-visible overlay for hand-held items |
| `guis/instances/GUIHUD.java` | Vehicle HUD with instruments |
| `guis/components/GUIComponentInstrument.java` | Individual instrument rendering component |
| `rendering/RenderInstrument.java` | Core instrument texture rendering |
| `rendering/RenderText.java` | Text rendering system |
| `entities/instances/PartGun.java` | Gun part with ammo tracking |
| `entities/instances/EntityPlayerGun.java` | Hand-held gun entity |
| `baseclasses/ComputedVariable.java` | Dynamic variable computation |
| `jsondefs/JSONInstrument.java` | Instrument JSON schema |
| `jsondefs/JSONPart.java` | Part definition schema (guns, etc.) |

---

## GUIOverlay - Hand-Held Items

The `GUIOverlay` class renders an always-visible overlay showing:

### Gun Label (Top-Right)
```java
// Location: GUIOverlay.java:48
addComponent(gunLabel = new GUIComponentLabel(
    screenWidth,           // X position (right-aligned)
    0,                     // Y position (top)
    ColorRGB.WHITE,        // Text color
    "",                    // Initial text
    TextAlignment.RIGHT_ALIGNED,
    1.0F                   // Scale
));
```

### State Update Logic
```java
// Location: GUIOverlay.java:74-98
@Override
public void setStates() {
    gunLabel.visible = false;

    if (!InterfaceManager.clientInterface.isChatOpen()) {
        // Check for hand-held gun
        EntityPlayerGun playerGun = EntityPlayerGun.playerClientGuns.get(player.getID());
        if (playerGun != null && playerGun.activeGun != null) {
            gunLabel.visible = true;
            gunLabel.text = "Gun:" + playerGun.activeGun.cachedItem.getItemName()
                          + " Loaded:" + playerGun.activeGun.getBulletText();
        } else {
            // Check for vehicle-mounted gun control
            AEntityB_Existing entityRiding = player.getEntityRiding();
            if (entityRiding instanceof PartSeat) {
                PartSeat seat = (PartSeat) entityRiding;
                if (seat.canControlGuns) {
                    gunLabel.visible = true;
                    gunLabel.text = "Active Gun:";
                    if (seat.activeGunItem != null) {
                        gunLabel.text += seat.activeGunItem.getItemName();
                        if (seat.activeGunItem.definition.gun.fireSolo) {
                            gunLabel.text += " [" + (seat.gunIndex + 1) + "]";
                        }
                    } else {
                        gunLabel.text += "None";
                    }
                }
            }
        }
    }
}
```

### Mouseover Tank Display
```java
// Location: GUIOverlay.java:115-124
if (interactable.tank != null) {
    String fluidName = interactable.tank.getFluid();
    if (fluidName.isEmpty()) {
        mouseoverLabel.text = String.format("%.1f/%.1fb",
            interactable.tank.getFluidLevel() / 1000F,
            interactable.tank.getMaxLevel() / 1000F);
    } else {
        mouseoverLabel.text = String.format("%s: %.1f/%.1fb",
            InterfaceManager.clientInterface.getFluidName(fluidName, interactable.tank.getFluidMod()),
            interactable.tank.getFluidLevel() / 1000F,
            interactable.tank.getMaxLevel() / 1000F);
    }
}
```

---

## GUIHUD - Vehicle Instruments

The `GUIHUD` class renders the vehicle dashboard with instruments.

### HUD Dimensions
```java
// Location: GUIHUD.java:34-35
private static final int HUD_WIDTH = 400;
private static final int HUD_HEIGHT = 140;
```

### Instrument Setup
```java
// Location: GUIHUD.java:55-82
@Override
public void setupComponents() {
    if (seat.placementDefinition.isController) {
        // Add vehicle instruments
        for (int i = 0; i < vehicle.instruments.size(); ++i) {
            if (vehicle.instruments.get(i) != null
                && !vehicle.definition.instruments.get(i).placeOnPanel) {
                GUIComponentInstrument instrument = new GUIComponentInstrument(
                    guiLeft, guiTop, vehicle, i
                );
                instruments.add(instrument);
                addComponent(instrument);
            }
        }

        // Add part instruments (e.g., gun ammo displays)
        for (APart part : vehicle.parts) {
            for (int i = 0; i < part.instruments.size(); ++i) {
                if (part.instruments.get(i) != null
                    && !part.definition.instruments.get(i).placeOnPanel) {
                    GUIComponentInstrument instrument = new GUIComponentInstrument(
                        guiLeft, guiTop, part, i
                    );
                    instruments.add(instrument);
                    addComponent(instrument);
                }
            }
        }
    }
}
```

### Health Display
```java
// Location: GUIHUD.java:130-132
healthLabel.text = String.format("Health: %d/%d",
    (int) Math.ceil(vehicle.definition.general.health - vehicle.damageVar.currentValue),
    vehicle.definition.general.health);
healthLabel.visible = seat.placementDefinition.isController || seat.canControlGuns;
healthLabel.color = vehicle.outOfHealth ? ColorRGB.RED : ColorRGB.WHITE;
```

---

## Instrument System

Instruments are texture-based displays that can show values through:
- **Rotation** - Needle gauges
- **Translation** - Sliding bars
- **Text** - Digital displays
- **Visibility** - Warning lights

### RenderInstrument Core Method
```java
// Location: RenderInstrument.java:41-134
public static void drawInstrument(
    AEntityE_Interactable<?> entity,
    TransformationMatrix transform,
    int slot,
    boolean onGUI,
    boolean blendingEnabled,
    float partialTicks
) {
    ItemInstrument instrument = entity.instruments.get(slot);
    JSONInstrumentDefinition slotDefinition = entity.definition.instruments.get(slot);

    // Get scale and part number
    float slotScale = onGUI ? slotDefinition.hudScale : slotDefinition.scale;
    partNumber = slotDefinition.optionalPartNumber;

    // Render each component layer
    for (int i = 0; i < instrument.definition.components.size(); ++i) {
        JSONInstrumentComponent component = instrument.definition.components.get(i);

        if (component.textObject != null) {
            // TEXT RENDERING
            String value = entity.getRawTextVariableValue(component.textObject, partialTicks);
            if (value == null) {
                value = String.format(component.textObject.variableFormat,
                    getInstrumentVariableValue(entity, null,
                        component.textObject.variableName,
                        component.textObject.variableFactor, partialTicks)
                    + component.textObject.variableOffset);
            }
            RenderText.draw3DText(value, entity, textTransform,
                component.textObject, true, renderLit);
        } else {
            // TEXTURE RENDERING with animations
            InstrumentSwitchbox switchbox = entity.instrumentComponentSwitchboxes.get(component);
            if (switchbox == null || switchbox.runSwitchbox(partialTicks, true)) {
                // Apply UV coordinates and render
                renderable.render();
            }
        }
    }
}
```

### Variable Resolution
```java
// Location: RenderInstrument.java:136-154
private static double getInstrumentVariableValue(
    AEntityD_Definable<?> entity,
    DurationDelayClock clock,
    String variable,
    double scaleFactor,
    float partialTicks
) {
    double value;
    if (ComputedVariable.isNumberedVariable(variable)) {
        // Variable has explicit part index (e.g., gun_ammo_count_1)
        value = entity.getOrCreateVariable(variable).computeValue(partialTicks);
    } else {
        // Try with current part number first
        value = entity.getOrCreateVariable(variable + "_" + partNumber).computeValue(partialTicks);
        if (value == 0) {
            // Fall back to general variable
            value = entity.getOrCreateVariable(variable).computeValue(partialTicks);
        }
    }
    return value * scaleFactor;
}
```

---

## ComputedVariable System

The `ComputedVariable` class provides dynamic, computed values for animations and displays.

### Variable Types
| Type | Example | Description |
|------|---------|-------------|
| **Computed** | `gun_ammo_count` | Recalculated each tick |
| **Saved** | `door_open` | Persisted in NBT |
| **Inverted** | `!gun_firing` | Automatically created inverse |
| **Constant** | `#0.5` | Fixed value |
| **Numbered** | `damage_1` | Part-specific index |

### Core Structure
```java
// Location: ComputedVariable.java
public class ComputedVariable {
    public final String variableKey;        // Variable name
    public double currentValue;             // Current computed value
    public boolean isActive;                // True if > 0
    public final ComputedVariable invertedVariable;  // Auto-created !variable

    public double computeValue(float partialTicks) {
        if (function != null) {
            if (changesOnPartialTicks && partialTicks != 0) {
                setInternal(function.apply(partialTicks), false);
            } else if (lastTickChecked != entity.ticksExisted) {
                setInternal(function.apply(partialTicks), false);
                lastTickChecked = entity.ticksExisted;
            }
        }
        return currentValue;
    }
}
```

---

## Gun Variables Reference

The `PartGun` class exposes these computed variables for HUD display:

### Ammo Variables
```java
// Location: PartGun.java:1411-1418
case ("gun_ammo_count"):
    return new ComputedVariable(this, variable,
        partialTicks -> loadedBulletCount, false);

case ("gun_ammo_count_reloading"):
    return new ComputedVariable(this, variable,
        partialTicks -> reloadingBulletCount, false);

case ("gun_ammo_percent"):
    return new ComputedVariable(this, variable,
        partialTicks -> loadedBulletCount / definition.gun.capacity, false);
```

### Complete Variable List

| Variable | Type | Description |
|----------|------|-------------|
| `gun_ammo_count` | int | Number of bullets currently loaded |
| `gun_ammo_count_reloading` | int | Bullets being reloaded |
| `gun_ammo_percent` | float | Percentage of capacity (0.0-1.0) |
| `gun_ammo_X_loaded` | bool | True if at least X bullets loaded |
| `gun_firing` | bool | Currently firing |
| `gun_fired` | bool | Just fired (single tick) |
| `gun_reload` | bool | Currently reloading |
| `gun_reload_windup` | bool | In reload start phase |
| `gun_reload_main` | bool | In main reload phase |
| `gun_reload_winddown` | bool | In reload end phase |
| `gun_inhand` | bool | Being held by player |
| `gun_inhand_equipped` | bool | Equipped in hand |
| `gun_inhand_aimed` | bool | Aiming down sights |
| `gun_windup` | int | Current windup rotation |
| `gun_windup_complete` | bool | Windup finished |
| `gun_cooldown` | int | Cooldown remaining |
| `gun_yaw` | float | Current yaw angle |
| `gun_pitch` | float | Current pitch angle |
| `gun_active_muzzlegroup` | int | Current muzzle group (1-indexed) |
| `gun_bullet_present` | bool | Active guided bullet exists |
| `gun_bullet_x/y/z` | float | Guided bullet relative position |
| `gun_bullet_yaw/pitch` | float | Guided bullet orientation |
| `gun_muzzleflash` | bool | Muzzle flash active |
| `gun_lockedon` | bool | Has locked target |
| `gun_lockedon_name` | string | Name of locked target |

### State Machine
```java
// Location: PartGun.java:1493-1511
public enum GunState {
    INACTIVE,           // Gun not in use
    ACTIVE,             // Gun activated but not controlled
    CONTROLLED,         // Player/seat controlling gun
    FIRING_REQUESTED,   // Fire button pressed
    FIRING_CURRENTLY;   // Actually firing bullets
}
```

---

## JSON Configuration

### Gun Part Definition (gunfireextinguisher.json)
```json
{
  "generic": {
    "type": "gun_hand_extinguisher",
    "canBeRemovedByHand": true,
    "canBePlacedOnGround": true,
    "width": 0.185896,
    "height": 0.5
  },
  "gun": {
    "handHeld": true,
    "capacity": 500,
    "preloadedBullet": "mtsofficialpack:extinguisherfoam:1",
    "reloadTime": 40,
    "fireDelay": 1.0,
    "muzzleVelocity": 40,
    "bulletSpreadFactor": 30.0,
    "minYaw": -30.0,
    "maxYaw": 30.0,
    "minPitch": -30.0,
    "maxPitch": 30.0,
    "diameter": 10.01,
    "handHeldNormalOffset": [0.0, 0.0, 0.75],
    "handHeldAimedOffset": [0.0, 0.0, 0.75],
    "muzzleGroups": [
      {
        "muzzles": [
          { "pos": [0.0, 0.65625, 0.039063] }
        ]
      }
    ]
  },
  "rendering": {
    "animatedObjects": [
      {
        "objectName": "ammo_gauge",
        "animations": [
          {
            "animationType": "rotation",
            "variable": "gun_ammo_count",
            "centerPoint": [0.09765, 0.7578, 0.0],
            "axis": [0.18, 0, 0.0]
          }
        ]
      }
    ]
  }
}
```

### Instrument Definition (instrument_aircraft_fuelqty.json)
```json
{
  "textureName": "instruments.png",
  "components": [
    {
      "scale": 1.0,
      "textureXCenter": 192,
      "textureYCenter": 448,
      "textureWidth": 128,
      "textureHeight": 128,
      "lightUpTexture": true
    },
    {
      "yCenter": 16,
      "scale": 1.0,
      "textureXCenter": 970,
      "textureYCenter": 192,
      "textureWidth": 8,
      "textureHeight": 128,
      "lightUpTexture": true,
      "animations": [
        {
          "animationType": "rotation",
          "variable": "fuel",
          "centerPoint": [0.0, 0.0, 0.0],
          "axis": [0.0, 0.0, 100.0],
          "offset": -50.0
        }
      ]
    },
    {
      "scale": 1.0,
      "textureXCenter": 960,
      "textureYCenter": 320,
      "textureWidth": 128,
      "textureHeight": 128,
      "lightUpTexture": true,
      "overlayTexture": true
    }
  ],
  "general": {
    "name": "Fuel Gauge",
    "description": "Indicates fuel."
  }
}
```

### Instrument Component Schema

| Parameter | Type | Description |
|-----------|------|-------------|
| `xCenter` | int | X offset from instrument center (pixels) |
| `yCenter` | int | Y offset from instrument center (pixels) |
| `scale` | float | Component scale (default 1.0) |
| `textureXCenter` | int | Texture X center on sheet (pixels) |
| `textureYCenter` | int | Texture Y center on sheet (pixels) |
| `textureWidth` | int | Texture width (pixels) |
| `textureHeight` | int | Texture height (pixels) |
| `textObject` | object | Optional text display instead of texture |
| `rotateWindow` | bool | Rotate grabbed texture region |
| `extendWindow` | bool | Extend texture edge on translation |
| `moveComponent` | bool | Move rendered position on translation |
| `lightUpTexture` | bool | Brighten when vehicle lights on |
| `alwaysLit` | bool | Always bright |
| `overlayTexture` | bool | Render as overlay blend |
| `animations` | array | Animation definitions |

### Instrument Slot Definition (on vehicle/part)

| Parameter | Type | Description |
|-----------|------|-------------|
| `pos` | Point3D | 3D position on vehicle |
| `rot` | RotationMatrix | 3D rotation |
| `scale` | float | 3D scale (default 128x128) |
| `hudX` | int | HUD X position (pixels from left) |
| `hudY` | int | HUD Y position (pixels from top) |
| `hudScale` | float | HUD scale factor |
| `optionalPartNumber` | int | Part slot to read variables from |
| `placeOnPanel` | bool | Show on panel GUI instead of main HUD |
| `defaultInstrument` | string | Default instrument to install |

### Text Object Schema

| Parameter | Type | Description |
|-----------|------|-------------|
| `variableName` | string | Variable to display |
| `variableFormat` | string | Printf format (e.g., "%.0f") |
| `variableOffset` | double | Add to value before display |
| `variableFactor` | double | Multiply value by this |
| `maxLength` | int | Max digits (adds leading 0s) |
| `fontName` | string | Font to use |
| `color` | ColorRGB | Text color |
| `autoScale` | bool | Auto-scale for size |

---

## Data Flow Summary

```
┌─────────────────────────────────────────────────────────────────────┐
│                     HAND-HELD ITEM (e.g., Fire Extinguisher)        │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  EntityPlayerGun holds reference to PartGun                         │
│  playerClientGuns.get(player.getID()) -> EntityPlayerGun            │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  PartGun.loadedBulletCount = 500  (internal state)                  │
│  PartGun.getBulletText() returns "500"                              │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  GUIOverlay.setStates() checks every frame                          │
│  gunLabel.text = "Gun:Fire Extinguisher Loaded:500"                 │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Rendered at top-right of screen                                    │
│  [Gun:Fire Extinguisher Loaded:500]                                 │
└─────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────┐
│                     VEHICLE INSTRUMENT (e.g., Fuel Gauge)           │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Vehicle/Part JSON defines instrument slot                          │
│  { "hudX": 200, "hudY": 100, "hudScale": 1.0 }                      │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  GUIHUD.setupComponents() creates GUIComponentInstrument            │
│  for each slot not marked placeOnPanel                              │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  RenderInstrument.drawInstrument() renders each component           │
│  Reads variable: entity.getOrCreateVariable("fuel")                 │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  ComputedVariable.computeValue() returns current fuel level         │
│  Animation applies rotation/translation based on value              │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Texture rendered at hudX, hudY with hudScale                       │
│  Needle rotated based on fuel percentage                            │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Comparison with Superb Warfare

| Feature | MTS | Superb Warfare |
|---------|-----|----------------|
| **HUD Position** | Top-right (overlay) + Bottom (HUD) | Bottom-right (overlay) |
| **Item Display** | Text label + Instruments | Texture-based HUD components |
| **Variable System** | ComputedVariable with lambda | KProperty reflection |
| **Attachments** | N/A (not applicable) | 5 slots (scope, barrel, etc.) |
| **Fire Modes** | Single mode per gun | Multiple switchable modes |
| **Data Format** | JSON in content packs | JSON + Kotlin classes |
| **Texture System** | 1024x1024 instrument sheets | Individual overlay textures |
| **Animation** | JSON-defined transforms | Kotlin animation definitions |

---

## Key Differences from Superb Warfare

### MTS Approach:
1. **Text-based overlay** for simple ammo display
2. **Instrument system** for detailed gauges (reusable across vehicles)
3. **ComputedVariable** system for any animated value
4. **Content pack driven** - all data in JSON

### Superb Warfare Approach:
1. **Texture-based overlay** for all weapon info
2. **Per-weapon rendering** with custom icons
3. **Attachment slot system** with visual indicators
4. **Fire mode switching** with HUD feedback

---

*Document created for MTS development reference*
