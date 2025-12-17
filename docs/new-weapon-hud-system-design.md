# New Weapon HUD System Design

A modern weapon HUD system for MTS that maintains **full backwards compatibility** with existing content packs while providing enhanced display features for new packs.

---

## Table of Contents

1. [Design Goals](#design-goals)
2. [Compatibility Strategy](#compatibility-strategy)
3. [System Architecture](#system-architecture)
4. [JSON Schema Extensions](#json-schema-extensions)
5. [Rendering Pipeline](#rendering-pipeline)
6. [Variable System Extensions](#variable-system-extensions)
7. [Implementation Plan](#implementation-plan)
8. [Migration Guide for Pack Makers](#migration-guide-for-pack-makers)

---

## Design Goals

### Primary Goals
1. **100% Backwards Compatible** - Existing content packs work without modification
2. **Opt-in Enhancement** - New features only activate when JSON defines them
3. **Data-Driven** - All HUD elements configurable via JSON (no code changes for pack makers)
4. **Flexible Positioning** - HUD elements can be placed anywhere on screen
5. **Modern Look** - Support for icons, bars, attachment indicators like Superb Warfare

### Secondary Goals
- Support for custom textures per weapon
- Fire mode display and switching indicator
- Attachment slot visualization
- Ammo type switching display
- Heat/overheat indicators
- Lock-on status display

---

## Compatibility Strategy

### Layered Approach

```
┌─────────────────────────────────────────────────────────────────────┐
│  LAYER 3: New Weapon HUD Overlay (NEW - opt-in via JSON)           │
│  - Only renders if weapon JSON has "hudOverlay" section             │
│  - Modern icon-based display                                        │
│  - Hides legacy text when active                                    │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│  LAYER 2: Legacy GUIOverlay Text (EXISTING - always available)     │
│  - "Gun:Name Loaded:X" format                                       │
│  - Hidden when Layer 3 is active for this weapon                    │
│  - Still works for old packs without hudOverlay                     │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│  LAYER 1: Instrument System (EXISTING - unchanged)                  │
│  - Vehicle HUD instruments                                          │
│  - Part-based instruments                                           │
│  - Fully preserved                                                  │
└─────────────────────────────────────────────────────────────────────┘
```

### Fallback Logic

```java
// Pseudocode for rendering decision
if (weapon.definition.hudOverlay != null) {
    // New system - render modern HUD
    renderNewWeaponHUD(weapon);
    // Hide legacy text label
    gunLabel.visible = false;
} else {
    // Legacy system - render text label
    gunLabel.visible = true;
    gunLabel.text = "Gun:" + weapon.getName() + " Loaded:" + weapon.getBulletText();
}
```

---

## System Architecture

### New Classes to Add

```
mccore/src/main/java/minecrafttransportsimulator/
├── guis/
│   └── instances/
│       └── GUIWeaponHUD.java          # NEW: Modern weapon HUD renderer
├── jsondefs/
│   └── JSONHUDOverlay.java            # NEW: HUD overlay definition
└── rendering/
    └── RenderWeaponHUD.java           # NEW: Weapon HUD rendering logic
```

### Class Relationships

```
┌──────────────────────┐     ┌──────────────────────┐
│     GUIOverlay       │     │    GUIWeaponHUD      │
│   (existing class)   │     │    (new class)       │
├──────────────────────┤     ├──────────────────────┤
│ - gunLabel           │     │ - weaponIcon         │
│ - mouseoverLabel     │     │ - ammoDisplay        │
│ - scannerItem        │     │ - fireModeIndicator  │
│                      │     │ - attachmentSlots    │
│ setStates() {        │     │ - ammoTypeIndicator  │
│   if (hasNewHUD) {   │────▶│                      │
│     gunLabel.hide()  │     │ render() {...}       │
│   }                  │     │                      │
│ }                    │     │                      │
└──────────────────────┘     └──────────────────────┘
           │                            │
           │                            │
           ▼                            ▼
┌──────────────────────────────────────────────────────┐
│                    PartGun                            │
│              (existing class)                         │
├──────────────────────────────────────────────────────┤
│ - loadedBulletCount                                   │
│ - state (GunState enum)                               │
│ - definition.gun (JSONPartGun)                        │
│ - definition.hudOverlay (NEW: JSONHUDOverlay)         │
│                                                       │
│ + getComputedVariable("gun_ammo_count")               │
│ + getComputedVariable("gun_fire_mode") // NEW         │
│ + getComputedVariable("gun_ammo_type") // NEW         │
└──────────────────────────────────────────────────────┘
```

---

## JSON Schema Extensions

### New `hudOverlay` Section in Part JSON

This is **completely optional**. If omitted, the weapon uses legacy text display.

```json
{
  "generic": {
    "type": "gun_assault_rifle"
  },
  "gun": {
    "capacity": 30,
    "fireDelay": 2.0,
    "muzzleVelocity": 300,
    "handHeld": true,
    "muzzleGroups": [...]
  },

  "hudOverlay": {
    "enabled": true,
    "position": "bottom_right",
    "offsetX": -20,
    "offsetY": -20,
    "scale": 1.0,

    "weaponDisplay": {
      "showIcon": true,
      "iconTexture": "packid:textures/hud/ak47_icon.png",
      "iconWidth": 64,
      "iconHeight": 32,
      "showName": true,
      "nameColor": "FFFFFF"
    },

    "ammoDisplay": {
      "showCount": true,
      "countScale": 1.5,
      "countColor": "FFFFFF",
      "countEmptyColor": "FF0000",
      "showBar": true,
      "barTexture": "packid:textures/hud/ammo_bar.png",
      "barWidth": 60,
      "barHeight": 4,
      "barColor": "FFCC00",
      "barEmptyColor": "FF0000"
    },

    "fireModeDisplay": {
      "enabled": true,
      "showKeybind": true,
      "modes": [
        {
          "name": "SEMI",
          "displayText": "SEMI",
          "icon": "packid:textures/hud/mode_semi.png"
        },
        {
          "name": "AUTO",
          "displayText": "AUTO",
          "icon": "packid:textures/hud/mode_auto.png"
        },
        {
          "name": "BURST",
          "displayText": "BURST",
          "burstCount": 3,
          "icon": "packid:textures/hud/mode_burst.png"
        }
      ]
    },

    "attachmentDisplay": {
      "enabled": true,
      "slots": [
        {
          "type": "scope",
          "position": [0, 0],
          "iconEmpty": "mts:textures/hud/slot_scope_empty.png",
          "iconFilled": "mts:textures/hud/slot_scope_filled.png"
        },
        {
          "type": "barrel",
          "position": [20, 0],
          "iconEmpty": "mts:textures/hud/slot_barrel_empty.png",
          "iconFilled": "mts:textures/hud/slot_barrel_filled.png"
        },
        {
          "type": "grip",
          "position": [40, 0],
          "iconEmpty": "mts:textures/hud/slot_grip_empty.png",
          "iconFilled": "mts:textures/hud/slot_grip_filled.png"
        },
        {
          "type": "stock",
          "position": [60, 0],
          "iconEmpty": "mts:textures/hud/slot_stock_empty.png",
          "iconFilled": "mts:textures/hud/slot_stock_filled.png"
        },
        {
          "type": "magazine",
          "position": [80, 0],
          "iconEmpty": "mts:textures/hud/slot_mag_empty.png",
          "iconFilled": "mts:textures/hud/slot_mag_filled.png"
        }
      ]
    },

    "ammoTypeDisplay": {
      "enabled": true,
      "showDots": true,
      "selectedColor": "FFFFFF",
      "unselectedColor": "666666"
    },

    "heatDisplay": {
      "enabled": false,
      "barTexture": "mts:textures/hud/heat_bar.png",
      "barWidth": 60,
      "barHeight": 4,
      "coolColor": "00FF00",
      "hotColor": "FF0000",
      "overheatWarning": true
    },

    "lockOnDisplay": {
      "enabled": true,
      "lockingTexture": "mts:textures/hud/locking.png",
      "lockedTexture": "mts:textures/hud/locked.png",
      "showTargetName": true
    }
  },

  "rendering": {...},
  "general": {...}
}
```

### Minimal New HUD Example

For pack makers who just want the new look with minimal config:

```json
{
  "generic": {
    "type": "gun_pistol"
  },
  "gun": {
    "capacity": 15,
    "handHeld": true,
    "muzzleGroups": [...]
  },

  "hudOverlay": {
    "enabled": true
  }
}
```

This uses all defaults:
- Bottom-right position
- Auto-generated icon from item texture
- White ammo count
- No fire mode display (single mode)
- No attachment slots
- No heat display

### JSONHUDOverlay.java Schema

```java
package minecrafttransportsimulator.jsondefs;

public class JSONHUDOverlay {

    @JSONDescription("Enable the new weapon HUD overlay. If false or omitted, uses legacy text display.")
    public boolean enabled = true;

    @JSONDescription("Screen position: top_left, top_right, bottom_left, bottom_right, or custom")
    public String position = "bottom_right";

    @JSONDescription("X offset from position anchor (pixels)")
    public int offsetX = -20;

    @JSONDescription("Y offset from position anchor (pixels)")
    public int offsetY = -20;

    @JSONDescription("Overall scale of the HUD overlay")
    public float scale = 1.0f;

    public WeaponDisplay weaponDisplay;
    public AmmoDisplay ammoDisplay;
    public FireModeDisplay fireModeDisplay;
    public AttachmentDisplay attachmentDisplay;
    public AmmoTypeDisplay ammoTypeDisplay;
    public HeatDisplay heatDisplay;
    public LockOnDisplay lockOnDisplay;

    public static class WeaponDisplay {
        public boolean showIcon = true;
        public String iconTexture;  // null = auto from item texture
        public int iconWidth = 64;
        public int iconHeight = 32;
        public boolean showName = true;
        public String nameColor = "FFFFFF";
    }

    public static class AmmoDisplay {
        public boolean showCount = true;
        public float countScale = 1.5f;
        public String countColor = "FFFFFF";
        public String countEmptyColor = "FF0000";
        public boolean showBar = false;
        public String barTexture;
        public int barWidth = 60;
        public int barHeight = 4;
        public String barColor = "FFCC00";
        public String barEmptyColor = "FF0000";
        public boolean showBackupAmmo = true;
    }

    public static class FireModeDisplay {
        public boolean enabled = false;
        public boolean showKeybind = true;
        public String keybindVariable = "FIRE_MODE";  // Config key name
        public List<FireMode> modes;
    }

    public static class FireMode {
        public String name;           // Internal name
        public String displayText;    // Shown on HUD
        public String icon;           // Optional icon texture
        public int burstCount;        // For burst mode
    }

    public static class AttachmentDisplay {
        public boolean enabled = false;
        public List<AttachmentSlot> slots;
    }

    public static class AttachmentSlot {
        public String type;           // scope, barrel, grip, stock, magazine, custom
        public int[] position;        // [x, y] offset
        public String iconEmpty;      // Texture when empty
        public String iconFilled;     // Texture when attached
        public String iconInvalid;    // Texture when not available for this gun
    }

    public static class AmmoTypeDisplay {
        public boolean enabled = false;
        public boolean showDots = true;
        public String selectedColor = "FFFFFF";
        public String unselectedColor = "666666";
        public boolean showAmmoName = false;
    }

    public static class HeatDisplay {
        public boolean enabled = false;
        public String barTexture;
        public int barWidth = 60;
        public int barHeight = 4;
        public String coolColor = "00FF00";
        public String hotColor = "FF0000";
        public boolean overheatWarning = true;
        public String overheatTexture;
    }

    public static class LockOnDisplay {
        public boolean enabled = false;
        public String lockingTexture;
        public String lockedTexture;
        public boolean showTargetName = true;
        public String targetNameColor = "FF0000";
    }
}
```

---

## Rendering Pipeline

### GUIWeaponHUD.java

```java
package minecrafttransportsimulator.guis.instances;

/**
 * Modern weapon HUD overlay that renders when a weapon defines hudOverlay in JSON.
 * Falls back to legacy GUIOverlay text when hudOverlay is not defined.
 */
public class GUIWeaponHUD extends AGUIBase {

    // Cached references
    private PartGun activeGun;
    private JSONHUDOverlay hudDef;

    // Display components
    private GUIComponentItem weaponIcon;
    private GUIComponentLabel weaponName;
    private GUIComponentLabel ammoCount;
    private GUIComponentLabel backupAmmo;
    private GUIComponentLabel fireModeLabel;
    private List<GUIComponentTexture> attachmentSlots;
    private List<GUIComponentTexture> ammoTypeDots;
    private GUIComponentTexture ammoBar;
    private GUIComponentTexture heatBar;
    private GUIComponentTexture lockOnIndicator;

    @Override
    public void setupComponents() {
        super.setupComponents();
        // Components created dynamically based on hudDef
    }

    @Override
    public void setStates() {
        super.setStates();

        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();

        // Check for hand-held gun with new HUD
        EntityPlayerGun playerGun = EntityPlayerGun.playerClientGuns.get(player.getID());
        if (playerGun != null && playerGun.activeGun != null) {
            PartGun gun = playerGun.activeGun;
            JSONHUDOverlay overlay = gun.definition.hudOverlay;

            if (overlay != null && overlay.enabled) {
                // Use new HUD system
                updateNewHUD(gun, overlay);
                return;
            }
        }

        // Check for vehicle-controlled gun with new HUD
        AEntityB_Existing entityRiding = player.getEntityRiding();
        if (entityRiding instanceof PartSeat) {
            PartSeat seat = (PartSeat) entityRiding;
            if (seat.canControlGuns && seat.activeGun != null) {
                JSONHUDOverlay overlay = seat.activeGun.definition.hudOverlay;
                if (overlay != null && overlay.enabled) {
                    updateNewHUD(seat.activeGun, overlay);
                    return;
                }
            }
        }

        // No new HUD - hide all components
        hideAllComponents();
    }

    private void updateNewHUD(PartGun gun, JSONHUDOverlay overlay) {
        // Rebuild components if gun changed
        if (gun != activeGun) {
            activeGun = gun;
            hudDef = overlay;
            rebuildComponents();
        }

        // Update weapon display
        if (overlay.weaponDisplay != null) {
            updateWeaponDisplay(gun, overlay.weaponDisplay);
        }

        // Update ammo display
        if (overlay.ammoDisplay != null) {
            updateAmmoDisplay(gun, overlay.ammoDisplay);
        }

        // Update fire mode display
        if (overlay.fireModeDisplay != null && overlay.fireModeDisplay.enabled) {
            updateFireModeDisplay(gun, overlay.fireModeDisplay);
        }

        // Update attachment display
        if (overlay.attachmentDisplay != null && overlay.attachmentDisplay.enabled) {
            updateAttachmentDisplay(gun, overlay.attachmentDisplay);
        }

        // Update ammo type display
        if (overlay.ammoTypeDisplay != null && overlay.ammoTypeDisplay.enabled) {
            updateAmmoTypeDisplay(gun, overlay.ammoTypeDisplay);
        }

        // Update heat display
        if (overlay.heatDisplay != null && overlay.heatDisplay.enabled) {
            updateHeatDisplay(gun, overlay.heatDisplay);
        }

        // Update lock-on display
        if (overlay.lockOnDisplay != null && overlay.lockOnDisplay.enabled) {
            updateLockOnDisplay(gun, overlay.lockOnDisplay);
        }
    }

    private void updateAmmoDisplay(PartGun gun, AmmoDisplay config) {
        int ammo = gun.getOrCreateVariable("gun_ammo_count").intValue();
        int capacity = gun.definition.gun.capacity;

        // Update count label
        if (config.showCount) {
            ammoCount.text = String.valueOf(ammo);
            ammoCount.color = ammo > 0
                ? ColorRGB.fromHex(config.countColor)
                : ColorRGB.fromHex(config.countEmptyColor);
        }

        // Update bar
        if (config.showBar && ammoBar != null) {
            float percent = (float) ammo / capacity;
            ammoBar.width = (int) (config.barWidth * percent);
            // Lerp color from full to empty
            ammoBar.color = ColorRGB.lerp(
                ColorRGB.fromHex(config.barEmptyColor),
                ColorRGB.fromHex(config.barColor),
                percent
            );
        }

        // Update backup ammo
        if (config.showBackupAmmo) {
            int backup = getBackupAmmoCount(gun);
            backupAmmo.text = "∞" + backup;
        }
    }

    private void updateFireModeDisplay(PartGun gun, FireModeDisplay config) {
        int modeIndex = gun.getOrCreateVariable("gun_fire_mode").intValue();

        if (config.modes != null && modeIndex < config.modes.size()) {
            FireMode mode = config.modes.get(modeIndex);

            // Show mode text
            fireModeLabel.text = mode.displayText;

            // Show keybind if configured
            if (config.showKeybind) {
                String key = getKeybindName(config.keybindVariable);
                fireModeLabel.text = "[" + key + "] " + mode.displayText;
            }
        }
    }

    // ... other update methods

    @Override
    protected boolean canStayOpen() {
        return true;
    }

    @Override
    public boolean capturesPlayer() {
        return false;
    }

    @Override
    public boolean renderTranslucent() {
        return true;
    }
}
```

### Integration with GUIOverlay

Modify existing `GUIOverlay.java` to check for new HUD:

```java
// In GUIOverlay.setStates()
@Override
public void setStates() {
    super.setStates();
    IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();

    // Check if new weapon HUD is handling display
    gunLabel.visible = false;

    if (!InterfaceManager.clientInterface.isChatOpen()) {
        EntityPlayerGun playerGun = EntityPlayerGun.playerClientGuns.get(player.getID());
        if (playerGun != null && playerGun.activeGun != null) {
            // NEW: Check for new HUD system
            if (playerGun.activeGun.definition.hudOverlay == null
                || !playerGun.activeGun.definition.hudOverlay.enabled) {
                // Legacy display - no new HUD defined
                gunLabel.visible = true;
                gunLabel.text = "Gun:" + playerGun.activeGun.cachedItem.getItemName()
                              + " Loaded:" + playerGun.activeGun.getBulletText();
            }
            // If new HUD exists, GUIWeaponHUD handles display
        } else {
            // Vehicle gun handling (same pattern)
            AEntityB_Existing entityRiding = player.getEntityRiding();
            if (entityRiding instanceof PartSeat) {
                PartSeat seat = (PartSeat) entityRiding;
                if (seat.canControlGuns) {
                    // Check if active gun has new HUD
                    boolean hasNewHUD = seat.activeGun != null
                        && seat.activeGun.definition.hudOverlay != null
                        && seat.activeGun.definition.hudOverlay.enabled;

                    if (!hasNewHUD) {
                        // Legacy display
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

    // ... rest of existing code (mouseover, scanner, etc.)
}
```

---

## Variable System Extensions

### New Gun Variables

Add to `PartGun.java` `createComputedVariable()`:

```java
// Fire mode (for multi-mode weapons)
case ("gun_fire_mode"):
    return new ComputedVariable(this, variable,
        partialTicks -> currentFireModeIndex, false);

case ("gun_fire_mode_count"):
    return new ComputedVariable(this, variable,
        partialTicks -> definition.hudOverlay != null
            && definition.hudOverlay.fireModeDisplay != null
            && definition.hudOverlay.fireModeDisplay.modes != null
            ? definition.hudOverlay.fireModeDisplay.modes.size() : 1, false);

// Ammo type selection
case ("gun_ammo_type"):
    return new ComputedVariable(this, variable,
        partialTicks -> currentAmmoTypeIndex, false);

case ("gun_ammo_type_count"):
    return new ComputedVariable(this, variable,
        partialTicks -> loadedBullets.size(), false);

// Heat system (if implemented)
case ("gun_heat"):
    return new ComputedVariable(this, variable,
        partialTicks -> currentHeat, false);

case ("gun_heat_percent"):
    return new ComputedVariable(this, variable,
        partialTicks -> currentHeat / maxHeat, false);

case ("gun_overheated"):
    return new ComputedVariable(this, variable,
        partialTicks -> currentHeat >= maxHeat ? 1 : 0, false);

// Attachment detection
case ("gun_has_scope"):
    return new ComputedVariable(this, variable,
        partialTicks -> hasAttachment("scope") ? 1 : 0, false);

case ("gun_has_barrel"):
    return new ComputedVariable(this, variable,
        partialTicks -> hasAttachment("barrel") ? 1 : 0, false);

case ("gun_has_grip"):
    return new ComputedVariable(this, variable,
        partialTicks -> hasAttachment("grip") ? 1 : 0, false);

case ("gun_has_stock"):
    return new ComputedVariable(this, variable,
        partialTicks -> hasAttachment("stock") ? 1 : 0, false);

case ("gun_has_magazine"):
    return new ComputedVariable(this, variable,
        partialTicks -> hasAttachment("magazine") ? 1 : 0, false);
```

### Variable Reference Table

| Variable | Type | Description | Backwards Compatible |
|----------|------|-------------|---------------------|
| `gun_ammo_count` | int | Bullets loaded | Yes (existing) |
| `gun_ammo_percent` | float | 0.0-1.0 capacity | Yes (existing) |
| `gun_firing` | bool | Currently firing | Yes (existing) |
| `gun_reload` | bool | Reloading | Yes (existing) |
| `gun_fire_mode` | int | Current mode index | NEW |
| `gun_fire_mode_count` | int | Number of modes | NEW |
| `gun_ammo_type` | int | Selected ammo index | NEW |
| `gun_ammo_type_count` | int | Available ammo types | NEW |
| `gun_heat` | float | Current heat value | NEW |
| `gun_heat_percent` | float | 0.0-1.0 heat | NEW |
| `gun_overheated` | bool | Overheated state | NEW |
| `gun_has_scope` | bool | Scope attached | NEW |
| `gun_has_barrel` | bool | Barrel attached | NEW |
| `gun_has_grip` | bool | Grip attached | NEW |
| `gun_has_stock` | bool | Stock attached | NEW |
| `gun_has_magazine` | bool | Extended mag attached | NEW |

---

## Implementation Plan

### Phase 1: Core Infrastructure (Non-Breaking)
1. Add `JSONHUDOverlay.java` class with all schema definitions
2. Add `hudOverlay` field to `JSONPart.java` (optional, null by default)
3. Create `GUIWeaponHUD.java` renderer class
4. Register `GUIWeaponHUD` in client initialization

### Phase 2: GUIOverlay Integration (Non-Breaking)
1. Modify `GUIOverlay.setStates()` to check for new HUD
2. Hide legacy text when new HUD is active
3. Keep all legacy functionality for packs without `hudOverlay`

### Phase 3: Variable Extensions (Non-Breaking)
1. Add new computed variables to `PartGun.java`
2. Variables only used when referenced (no overhead otherwise)
3. Old packs don't reference new variables = no change

### Phase 4: Fire Mode System (Non-Breaking)
1. Add fire mode cycling keybind
2. Add `currentFireModeIndex` to `PartGun`
3. Modify firing logic to respect fire mode
4. Only activates if `fireModeDisplay.modes` defined

### Phase 5: Attachment System (Non-Breaking)
1. Add attachment slot support to gun parts
2. Attachment display only when slots defined
3. Purely visual initially (gameplay effects later)

### Phase 6: Default Textures
1. Create default HUD textures in MTS core
2. Packs can override with custom textures
3. `mts:textures/hud/` folder with defaults

---

## Migration Guide for Pack Makers

### Level 0: No Changes Required
Your existing packs work exactly as before. The legacy text display continues to function.

```json
{
  "generic": { "type": "gun_pistol" },
  "gun": { "capacity": 15, ... }
}
```
**Result:** Shows `"Gun:Pistol Loaded:15"` text in top-right

### Level 1: Enable New HUD with Defaults

Add minimal `hudOverlay` section:

```json
{
  "generic": { "type": "gun_pistol" },
  "gun": { "capacity": 15, ... },

  "hudOverlay": {
    "enabled": true
  }
}
```
**Result:** Modern HUD with auto-generated icon, white ammo count, bottom-right position

### Level 2: Custom Icon and Colors

```json
{
  "hudOverlay": {
    "enabled": true,
    "weaponDisplay": {
      "iconTexture": "mypack:textures/hud/pistol.png",
      "iconWidth": 48,
      "iconHeight": 24
    },
    "ammoDisplay": {
      "countColor": "00FF00",
      "showBar": true,
      "barColor": "00FF00"
    }
  }
}
```

### Level 3: Fire Modes

```json
{
  "hudOverlay": {
    "enabled": true,
    "fireModeDisplay": {
      "enabled": true,
      "showKeybind": true,
      "modes": [
        { "name": "SEMI", "displayText": "SEMI" },
        { "name": "AUTO", "displayText": "AUTO" }
      ]
    }
  }
}
```

### Level 4: Full Featured

```json
{
  "hudOverlay": {
    "enabled": true,
    "position": "bottom_right",
    "scale": 1.2,

    "weaponDisplay": {
      "iconTexture": "mypack:textures/hud/rifle.png",
      "showName": true
    },

    "ammoDisplay": {
      "showCount": true,
      "countScale": 2.0,
      "showBar": true,
      "showBackupAmmo": true
    },

    "fireModeDisplay": {
      "enabled": true,
      "modes": [
        { "name": "SEMI", "displayText": "S", "icon": "mypack:textures/hud/semi.png" },
        { "name": "BURST", "displayText": "B", "burstCount": 3 },
        { "name": "AUTO", "displayText": "A", "icon": "mypack:textures/hud/auto.png" }
      ]
    },

    "attachmentDisplay": {
      "enabled": true,
      "slots": [
        { "type": "scope", "position": [0, 0] },
        { "type": "barrel", "position": [16, 0] },
        { "type": "grip", "position": [32, 0] }
      ]
    },

    "heatDisplay": {
      "enabled": true,
      "overheatWarning": true
    },

    "lockOnDisplay": {
      "enabled": true,
      "showTargetName": true
    }
  }
}
```

---

## Visual Mockup

### Default Layout (bottom-right)

```
                                          ┌─────────────────────────────┐
                                          │  ┌────────┐                 │
                                          │  │ WEAPON │  Weapon Name    │
                                          │  │  ICON  │                 │
                                          │  └────────┘                 │
                                          │                             │
                                          │     30        [R] AUTO      │
                                          │    ════        ════         │
                                          │  ▓▓▓▓▓▓░░   [S][B][G][M]    │
                                          │                             │
                                          │  ∞120   ● ○ ○ (ammo types)  │
                                          └─────────────────────────────┘
```

### Minimal Layout

```
                                          ┌──────────────┐
                                          │ ┌────┐       │
                                          │ │ICON│  15   │
                                          │ └────┘       │
                                          └──────────────┘
```

---

## File Structure Summary

```
mccore/src/main/java/minecrafttransportsimulator/
├── guis/
│   ├── components/
│   │   └── GUIComponentTexture.java    # NEW: Texture component for bars/icons
│   └── instances/
│       ├── GUIOverlay.java             # MODIFIED: Add new HUD check
│       └── GUIWeaponHUD.java           # NEW: Modern weapon HUD
├── jsondefs/
│   ├── JSONPart.java                   # MODIFIED: Add hudOverlay field
│   └── JSONHUDOverlay.java             # NEW: HUD overlay schema
├── entities/instances/
│   └── PartGun.java                    # MODIFIED: Add new variables
└── rendering/
    └── RenderWeaponHUD.java            # NEW: HUD rendering utilities

mccore/src/main/resources/assets/mts/textures/hud/
├── slot_scope_empty.png                # Default attachment slot icons
├── slot_scope_filled.png
├── slot_barrel_empty.png
├── slot_barrel_filled.png
├── slot_grip_empty.png
├── slot_grip_filled.png
├── slot_stock_empty.png
├── slot_stock_filled.png
├── slot_mag_empty.png
├── slot_mag_filled.png
├── mode_semi.png                       # Default fire mode icons
├── mode_burst.png
├── mode_auto.png
├── ammo_bar.png                        # Default bar textures
├── heat_bar.png
├── locking.png                         # Lock-on indicators
└── locked.png
```

---

## Summary

This design provides:

1. **100% Backwards Compatibility** - Existing packs work unchanged
2. **Opt-in Modern HUD** - Add `hudOverlay` section to enable
3. **Progressive Enhancement** - Start simple, add features as needed
4. **Data-Driven** - All configuration in JSON
5. **Pack-Customizable** - Custom textures, colors, positions
6. **Superb Warfare Features** - Icons, fire modes, attachments, heat, lock-on

The key principle is: **if `hudOverlay` is not defined or `enabled` is false, the legacy system works exactly as before.**

---

*Document created for MTS development planning*
