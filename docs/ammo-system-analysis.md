# MTS Ammo System Analysis & Improvement Proposal

## Superb Warfare Control Reference

Based on [Superb Warfare Wiki](https://sbw.lq0.tech/en-us/about) and [GitHub](https://github.com/Mercurows/SuperbWarfare):

### Superb Warfare Keybinds
| Key | Action |
|-----|--------|
| `N` | Switch fire mode (Semi/Auto/Burst) |
| `Arrow Up/Down` | Cycle ammo types |
| `Arrow Left/Right` | Cycle attachments (scopes, etc.) |
| `H` | Enter customization mode |
| `Sneak + Left-click` | Cycle ammo categories |

### Superb Warfare HUD Elements
```
┌─────────────────────────────────────────┐
│   Gun Name                              │
│   Ammo Type Name                        │
│                                         │
│   ┌────┐                                │
│   │ 🔫 │     150                        │  ← Gun icon + ammo count
│   └────┘      ∞                         │  ← Backup ammo
│                                         │
│   [N] ═      [ 🔫 ] 50                  │  ← Fire mode + ammo bracket
│              ■ ■ □ □                    │  ← Selection squares (4 slots)
│              ▲                          │
│              └── Shows which ammo       │
│                  slot is selected       │
└─────────────────────────────────────────┘
```

### Selection Squares Explained
The squares `■ ■ □ □` under the ammo bracket show:
- **Filled squares (■)**: Ammo slots that have ammo loaded
- **Empty squares (□)**: Empty ammo slots
- **Current selection**: Highlighted/different color
- **Arrow keys**: Move selection left/right between slots

---

## Current MTS Ammo System

### How It Works Now

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        CURRENT MTS AMMO FLOW                                │
└─────────────────────────────────────────────────────────────────────────────┘

  Player Inventory                    Gun (PartGun)
  ┌─────────────┐                    ┌─────────────────────────────┐
  │ AP Rounds   │───── R key ───────►│ loadedBullets[0]: AP (15)   │ ← Fires first
  │ Incendiary  │     (reload)       │ loadedBullets[1]: INC (10)  │ ← Fires second
  │ Tracer      │                    │ loadedBullets[2]: TRC (5)   │ ← Fires third
  └─────────────┘                    └─────────────────────────────┘
                                              │
                                              ▼
                                     Fires in FIFO order
                                     (First In, First Out)
```

### Key Characteristics

| Feature | Current Behavior |
|---------|-----------------|
| **Loading** | Press R or auto-reload from inventory |
| **Multiple types** | Queued in `loadedBullets` list |
| **Fire order** | FIFO - first loaded fires first |
| **Switching** | NOT POSSIBLE - no keybind |
| **Ammo source** | Player inventory only |
| **Display** | Shows only first (active) type |

### Code Location

```java
// PartGun.java
private final List<ItemBullet> loadedBullets = new ArrayList<>();      // Ammo types
private final List<Integer> loadedBulletCounts = new ArrayList<>();    // Count per type

// Firing consumes from index 0
lastLoadedBullet = loadedBullets.get(0);
if (--count == 0) {
    loadedBullets.remove(0);  // Move to next type
}
```

### Reload Process

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         RELOAD SEQUENCE                                   │
└──────────────────────────────────────────────────────────────────────────┘

1. Player presses R (or auto-reload triggers)
         │
         ▼
2. tryToReload() searches inventory for matching bullet
   - Checks diameter match
   - Checks caseLength in range (minCaseLength to maxCaseLength)
         │
         ▼
3. If found and capacity available:
   - Add to reloadingBullets queue
   - Start reload animation (reloadStartTime → reloadTime → reloadEndTime)
         │
         ▼
4. After reload animation:
   - Move from reloadingBullets to loadedBullets
   - Ready to fire
```

---

## Problems with Current System

### 1. No Ammo Type Selection
- Player cannot choose which ammo type to use
- Must fire through entire queue to reach desired ammo
- Tactical disadvantage in combat

### 2. No External Ammo Sources
- Only loads from player inventory
- Cannot use ammo crates/boxes in world
- Vehicle-mounted ammo storage not utilized

### 3. Limited HUD Information
- Only shows first ammo type
- No visibility into queued ammo
- No way to know what's coming next

### 4. No Fire Mode Switching
- `isSemiAuto` is fixed in JSON
- Cannot toggle between semi/auto in-game

---

## Proposed Improvements

### Option A: Ammo Type Switching (Like Superb Warfare)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     PROPOSED: AMMO SWITCHING                                │
└─────────────────────────────────────────────────────────────────────────────┘

                         Press [N] to cycle
                              │
  ┌───────────────────────────┼───────────────────────────────┐
  │                           ▼                               │
  │   [ AP ]  ←──────►  [ INC ]  ←──────►  [ TRC ]           │
  │     ▲                                       │             │
  │     └───────────────────────────────────────┘             │
  │                                                           │
  │   Selected ammo fires, others remain queued               │
  └───────────────────────────────────────────────────────────┘

HUD Display:
┌─────────────────────────────┐
│  M1919 Machine Gun          │
│  Armor Piercing             │  ← Shows SELECTED type
│                             │
│  ┌───┐     150              │
│  │🔫│      ∞                │
│  └───┘                      │
│                             │
│  [N]  ═   [ AP ] 50         │  ← Selected: AP with count
│           [ INC ] 30        │  ← Queued: Incendiary
│           [ TRC ] 20        │  ← Queued: Tracer
└─────────────────────────────┘
```

#### Implementation Requirements

1. **New keybind**: `N` for ammo type cycling
2. **New variable**: `selectedAmmoIndex` in PartGun
3. **Modified firing**: Fire from `selectedAmmoIndex` instead of always index 0
4. **HUD update**: Show all loaded types with selection indicator
5. **Packet**: Sync ammo selection to server

#### Code Changes Needed

```java
// PartGun.java - New field
private int selectedAmmoIndex = 0;

// New method
public void cycleAmmoType() {
    if (loadedBullets.size() > 1) {
        selectedAmmoIndex = (selectedAmmoIndex + 1) % loadedBullets.size();
        // Send packet to sync
    }
}

// Modified firing - use selectedAmmoIndex instead of 0
lastLoadedBullet = loadedBullets.get(selectedAmmoIndex);
```

---

### Option B: Ammo Crate Integration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     PROPOSED: AMMO CRATE SYSTEM                             │
└─────────────────────────────────────────────────────────────────────────────┘

  World                              Gun
  ┌─────────────────┐               ┌─────────────────┐
  │   AMMO CRATE    │               │                 │
  │  ┌───────────┐  │   Interact    │  loadedBullets  │
  │  │ AP x100   │  │─────────────► │                 │
  │  │ INC x50   │  │   (E key?)    │                 │
  │  │ TRC x50   │  │               │                 │
  │  └───────────┘  │               └─────────────────┘
  └─────────────────┘
         │
         │  Player can also pick up
         ▼  ammo to inventory
  ┌─────────────────┐
  │ Player Inventory│
  └─────────────────┘
```

#### How It Would Work

1. **Ammo Crates** (already exist in MTS?)
   - Placed in world or on vehicles
   - Contains multiple ammo types
   - GUI to select which ammo to load

2. **Direct Loading**
   - Player near crate + holding gun
   - Press key to open ammo selection
   - Choose ammo type → starts reload

3. **Vehicle Integration**
   - Turret guns connected to vehicle ammo storage
   - Auto-reload from vehicle's ammo crates

---

### Option C: Full Superb Warfare Style

Combines both options with additional features:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     FULL SYSTEM OVERVIEW                                    │
└─────────────────────────────────────────────────────────────────────────────┘

  ┌─────────────┐     ┌─────────────┐     ┌─────────────────────┐
  │   Player    │     │  Ammo Crate │     │    Gun (PartGun)    │
  │  Inventory  │     │   in World  │     │                     │
  │ ┌─────────┐ │     │ ┌─────────┐ │     │  Selected: [AP]     │
  │ │ AP x60  │ │     │ │ AP x100 │ │     │  ┌─────────────┐    │
  │ │ INC x30 │ │     │ │ INC x50 │ │     │  │ AP: 50      │◄───┼── Fires this
  │ └─────────┘ │     │ └─────────┘ │     │  │ INC: 30     │    │
  └──────┬──────┘     └──────┬──────┘     │  │ TRC: 20     │    │
         │                   │            │  └─────────────┘    │
         │    R key          │  E key?    │                     │
         └───────────────────┴────────────┼──────────────────────
                                          │
                                    [N] cycles selection
```

#### Features

| Feature | Key | Description |
|---------|-----|-------------|
| Reload from inventory | R | Current behavior |
| Reload from crate | E (near crate) | New - direct crate loading |
| Cycle ammo type | N | New - switch between loaded types |
| Fire mode toggle | M | New - semi/auto/burst switching |

---

## HUD Display Comparison

### Current MTS HUD
```
┌─────────────────────┐
│  Fire Extinguisher  │
│  Extinguisher Foam  │
│                     │
│  ┌───┐     500      │
│  │🧯│      ∞        │
│  └───┘              │
│                     │
│  [N] ═   [ 🪣 ]     │  ← Only shows one type, no switching
└─────────────────────┘
```

### Proposed MTS HUD (Superb Warfare Style)
```
┌──────────────────────────────────────┐
│  M1919 Machine Gun                   │
│  Armor Piercing                      │  ← Shows SELECTED ammo name
│                                      │
│  ┌───┐     150                       │
│  │🔫│      ∞                         │
│  └───┘                               │
│                                      │
│  [N] ═    [ AP ] 50                  │  ← Selected ammo with count
│           ■ ■ □ □                    │  ← Ammo slot indicators
│           ▲                          │
│           └── Slot 1 selected        │
│                                      │
│  [↑↓] Ammo  [N] Semi                 │  ← Control hints
└──────────────────────────────────────┘

Selection Squares Detail:
■ = Slot has ammo (filled)
□ = Slot empty
Highlighted = Currently selected

Arrow Up/Down cycles through:
  Slot 1: [ AP ] 50    ← Selected
  Slot 2: [ INC ] 30
  Slot 3: (empty)
  Slot 4: (empty)
```

### Control Scheme Proposal for MTS
| Key | Action | Notes |
|-----|--------|-------|
| `Arrow Up` | Select previous ammo slot | Cycles through loaded ammo |
| `Arrow Down` | Select next ammo slot | Cycles through loaded ammo |
| `N` | Toggle fire mode | Only if gun supports multiple modes |
| `R` | Reload | Existing MTS keybind |

---

## Implementation Priority

### Phase 1: Basic Ammo Switching (Recommended First)
1. Add `selectedAmmoIndex` to PartGun
2. Add keybind for cycling ammo
3. Modify firing to use selected index
4. Update HUD to show all loaded types
5. Add selection indicator

### Phase 2: Enhanced HUD
1. Show all ammo types with counts
2. Highlight selected type
3. Show fire mode (read-only for now)

### Phase 3: Ammo Crate Integration
1. Add interaction with ammo crates
2. GUI for selecting ammo from crate
3. Vehicle ammo storage connection

### Phase 4: Fire Mode Switching

**Current State (FULLY IMPLEMENTED):**
- ✅ `fireModes` array added to `JSONPart.java` - defines available modes
- ✅ `defaultFireMode` added to `JSONPart.java` - starting mode
- ✅ `burstCount` added to `JSONPart.java` - shots per burst
- ✅ `WeaponHUDOverlay.java` displays correct icon based on fire mode
- ✅ Backward compatibility with legacy `isSemiAuto` boolean
- ✅ `currentFireModeIndex` added to PartGun (saved/loaded from NBT)
- ✅ `getCurrentFireMode()` and `cycleFireMode()` methods in PartGun
- ✅ `FIRE_MODE_CHANGE` packet added to PacketPartGun for server sync
- ✅ Keybind handler for N key connected
- ✅ All fire modes (semi/auto/burst) working correctly

---

## Gun Variable Compatibility with Fire Modes

All gun animation variables now work correctly in all fire modes. The core mod automatically handles per-shot pulsing for auto/burst modes.

### Variable Behavior by Fire Mode

| Variable | Semi-Auto | Auto | Burst | Behavior |
|----------|:---------:|:----:|:-----:|----------|
| `gun_firing` | ✅ | ✅ | ✅ | Pulses per-shot in auto/burst modes. In semi mode, returns 1 while firing state active. |
| `!gun_firing` | ✅ | ✅ | ✅ | Inverted version of `gun_firing`. Works for per-shot animations in all modes. |
| `gun_cooldown` | ✅ | ❌ | ✅ | Returns 1 while cooldown > 0. In auto mode, next shot fires before cooldown reaches 0, so never pulses. |
| `gun_fired` | ✅ | ✅ | ✅ | Returns 1 for exactly 1 tick when each shot fires. Equivalent to `gun_firing` in auto/burst. |

### How It Works

The core mod detects the current fire mode and adjusts `gun_firing` behavior:
- **Semi-auto**: Original behavior (1 while firing state is active)
- **Auto/Burst**: Pulses per-shot (same as `gun_fired`)

This ensures existing content packs work without any JSON changes.

### When to Use Each Variable

| Use Case | Recommended Variable | Notes |
|----------|---------------------|-------|
| Per-shot animation (recoil, hammer) | `gun_firing` or `!gun_firing` | Works all fire modes |
| Per-shot sound | `!gun_firing` | Works all fire modes |
| Per-shot particles (muzzle flash) | `!gun_firing` | Works all fire modes |
| Continuous effect while trigger held | N/A | Use `gun_firing` in semi mode only |
| Effect when NOT firing | `!gun_firing` | For idle/ready states (semi mode) |
| Reload animations | `gun_reload` | True during reload sequence |

**Available Fire Modes:**
| Mode | Value | Icon | Behavior |
|------|-------|------|----------|
| Semi-Automatic | `"semi"` | `semi.png` | One shot per click |
| Automatic | `"auto"` | `auto.png` | Continuous fire while holding |
| Burst | `"burst"` | `burst.png` | Fire `burstCount` shots per click |

**JSON Schema (NOW IMPLEMENTED):**
```json
{
  "gun": {
    "fireModes": ["semi", "auto", "burst"],  // Available modes - NOW SUPPORTED
    "defaultFireMode": "auto",                // Starting mode - NOW SUPPORTED
    "burstCount": 3,                          // Shots per burst - NOW SUPPORTED
    "isSemiAuto": true                        // DEPRECATED - still works for backward compat
  }
}
```

**Priority Order for Fire Mode Display:**
1. If `fireModes` is defined → use `defaultFireMode` (or first in list)
2. Else if `isSemiAuto: true` → display semi icon
3. Else → display auto icon (default)

**Still Needed:**
- `InterfaceInput.java`: Add keybind handler for `N` key to call `cycleFireMode()`
- `PartGun.java`: Modify firing logic to actually use `getCurrentFireMode()` for semi/auto/burst behavior
- Burst mode logic (fire X shots then stop)

---

## Questions to Decide

1. **Should ammo switching be instant or require reload animation?**
   - Instant: More arcade-like, faster gameplay
   - With reload: More realistic, tactical consideration

2. **Should we limit number of ammo types per gun?**
   - Unlimited: Maximum flexibility
   - Limited (e.g., 3): Prevents UI clutter, tactical choices

3. **How to handle ammo crates?**
   - Separate GUI: More control, slower
   - Quick-select: Faster, less precise

4. **Fire mode system?**
   - Per-gun in JSON: Current system
   - Player-switchable: New feature
   - Both: JSON defines available modes, player switches between them

---

## Summary

| System | Effort | Impact | Recommendation |
|--------|--------|--------|----------------|
| Ammo switching | Medium | High | **Do first** |
| Multi-ammo HUD | Low | Medium | Do with switching |
| Ammo crates | High | Medium | Future phase |
| Fire modes | Medium | Low | Optional/Future |

The ammo switching feature would bring MTS guns closer to modern FPS standards while maintaining compatibility with existing content packs.
