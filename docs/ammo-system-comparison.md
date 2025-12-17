# Ammo System Comparison: Superb Warfare vs MTS

## Overview

This document compares how Superb Warfare and MTS handle ammunition systems, particularly for HUD display.

---

## Superb Warfare Ammo System

### Architecture

```mermaid
flowchart TB
    subgraph "Data Layer"
        GunItem["GunItem<br/>(extends Item)"]
        GunData["GunData<br/>(computed from ItemStack)"]
        DefaultGunData["DefaultGunData<br/>(JSON definition)"]
        AmmoConsumer["AmmoConsumer<br/>(defines ammo type)"]
    end

    subgraph "Runtime State"
        ItemStack["ItemStack<br/>(player's main hand)"]
        NBT["NBT Data<br/>- Ammo count<br/>- Selected ammo type<br/>- Fire mode"]
    end

    subgraph "HUD Display"
        AmmoBarOverlay["AmmoBarOverlay.kt"]
        RenderContext["RenderContext"]
    end

    GunItem --> GunData
    DefaultGunData --> GunData
    ItemStack --> GunData
    NBT --> ItemStack

    GunData --> AmmoConsumer
    AmmoConsumer -->|"stack()"| AmmoIcon["Ammo Item Icon"]
    AmmoConsumer -->|"playerAmmoType"| AmmoName["Ammo Display Name"]

    GunData --> AmmoBarOverlay
    AmmoBarOverlay --> RenderContext
```

### Key Data Flow

```mermaid
sequenceDiagram
    participant P as Player
    participant I as ItemStack
    participant G as GunData
    participant C as AmmoConsumer
    participant H as AmmoBarOverlay

    P->>I: mainHandItem
    I->>G: GunData.from(stack)
    G->>G: compute() - applies modifiers
    G->>C: selectedAmmoConsumer()

    Note over H: HUD Rendering
    H->>G: data.ammo.get()
    H->>C: consumer.stack()
    H->>C: consumer.playerAmmoType
    H-->>P: Renders HUD with:<br/>- Gun icon<br/>- Ammo count<br/>- Ammo type icon<br/>- Ammo type name
```

### Superb Warfare Ammo Sources

| Source | Description | Example |
|--------|-------------|---------|
| `GunData.ammo.get()` | Current magazine ammo count | `32` |
| `GunData.selectedAmmoConsumer()` | Current ammo type definition | Returns `AmmoConsumer` |
| `AmmoConsumer.stack()` | ItemStack for the ammo type | `ItemStack(rifle_ammo)` |
| `AmmoConsumer.playerAmmoType` | Enum for player ammo types | `Ammo.RIFLE` |
| `getAmmoDisplayName(data)` | Display name for HUD | `"5.56mm Rifle"` |

### Superb Warfare JSON Definition (gun)

```json
{
  "AmmoConsumers": [
    {
      "Ammo": "@Rifle",           // Player ammo type
      "AmmoSlot": "Default"
    },
    {
      "Ammo": "superbwarfare:ap_ammo",  // Specific item
      "AmmoSlot": "AP"
    }
  ]
}
```

---

## MTS Ammo System

### Architecture

```mermaid
flowchart TB
    subgraph "Data Layer"
        ItemPartGun["ItemPartGun<br/>(extends AItemPart)"]
        JSONPart["JSONPart<br/>(gun definition)"]
        ItemBullet["ItemBullet<br/>(ammo item)"]
    end

    subgraph "Runtime State - PartGun Entity"
        PartGun["PartGun<br/>(spawned entity)"]
        LoadedBullets["loadedBullets<br/>List&lt;ItemBullet&gt;"]
        LastLoadedBullet["lastLoadedBullet<br/>ItemBullet"]
        AmmoCount["gun_ammo_count<br/>ComputedVariable"]
    end

    subgraph "Runtime State - Item NBT"
        ItemStack["ItemStack<br/>(in player inventory)"]
        NBT["NBT Data<br/>- loadedBullet0, 1, 2...<br/>- loadedBulletsSize<br/>- lastLoadedBullet"]
    end

    subgraph "HUD Display"
        WeaponHUDOverlay["WeaponHUDOverlay.java"]
        GUIOverlay["GUIOverlay.java<br/>(legacy text)"]
    end

    ItemPartGun --> PartGun
    JSONPart --> ItemPartGun
    ItemBullet --> LoadedBullets

    PartGun --> LoadedBullets
    PartGun --> LastLoadedBullet
    PartGun --> AmmoCount

    ItemStack --> NBT
    NBT -->|"on spawn"| PartGun
    PartGun -->|"on save"| NBT

    PartGun --> WeaponHUDOverlay
    ItemStack --> WeaponHUDOverlay
    WeaponHUDOverlay --> GUIOverlay
```

### Key Data Flow

```mermaid
sequenceDiagram
    participant P as Player
    participant I as ItemStack (held)
    participant E as EntityPlayerGun
    participant G as PartGun
    participant H as WeaponHUDOverlay

    P->>I: getHeldItem()

    alt Instant Display (before entity spawns)
        I->>H: Read NBT directly
        H->>I: loadedBulletsSize
        H->>I: loadedBullet0.count
        H->>I: loadedBullet0.getPackItem()
        H->>I: lastLoadedBullet (if empty)
    end

    alt Full Display (after entity spawns)
        P->>E: EntityPlayerGun.playerClientGuns
        E->>G: activeGun
        G->>H: gun_ammo_count variable
        G->>H: lastLoadedBullet
    end

    H-->>P: Renders HUD with:<br/>- Gun icon<br/>- Ammo count<br/>- Bullet item icon<br/>- Bullet name
```

### MTS Ammo Sources

| Source | Description | Example |
|--------|-------------|---------|
| `gun.getOrCreateVariable("gun_ammo_count")` | Total ammo in gun | `500` |
| `gun.lastLoadedBullet` | Currently loaded bullet type | `ItemBullet` |
| `gun.loadedBullets` | List of loaded bullet types | `List<ItemBullet>` |
| `gun.loadedBulletCounts` | Count per bullet type | `List<Integer>` |
| NBT `loadedBullet{i}` | Serialized bullet data | `{packID, systemName, count}` |
| NBT `lastLoadedBullet` | Last used bullet (persists when empty) | `{packID, systemName}` |

### MTS JSON Definition (gun)

```json
{
  "gun": {
    "capacity": 500,
    "preloadedBullet": "mtsofficialpack:extinguisherfoam:1",
    "diameter": 10.01
  },
  "gunHUD": {
    "enabled": true,
    "weaponDisplay": {
      "showIcon": true,
      "iconTexture": "mtsofficialpack:textures/gun_icon/fireextinguisher_icon.png"
    },
    "ammoDisplay": {
      "showCount": true
    }
  }
}
```

---

## Key Differences

```mermaid
flowchart LR
    subgraph SW ["Superb Warfare"]
        SW_Item["ItemStack only<br/>(no entity needed)"]
        SW_Data["GunData wraps ItemStack"]
        SW_Consumer["AmmoConsumer defines<br/>ammo type + icon + name"]
        SW_Always["Always has ammo info<br/>(from definition)"]
    end

    subgraph MTS ["MTS"]
        MTS_Entity["PartGun entity<br/>(spawns after equip)"]
        MTS_NBT["NBT stores bullets"]
        MTS_Bullet["ItemBullet is the<br/>ammo definition"]
        MTS_Empty["Empty gun = no bullet info<br/>(unless lastLoadedBullet saved)"]
    end

    SW_Item -.->|"Simpler"| MTS_Entity
    SW_Consumer -.->|"vs"| MTS_Bullet
    SW_Always -.->|"vs"| MTS_Empty
```

### Comparison Table

| Aspect | Superb Warfare | MTS |
|--------|---------------|-----|
| **Ammo Definition** | `AmmoConsumer` in gun JSON | Separate `ItemBullet` items matched by diameter/caseLength |
| **Ammo Type Storage** | `selectedAmmoType` index | `loadedBullets` list |
| **Empty Gun Display** | Always shows ammo type (from definition) | Shows nothing unless `lastLoadedBullet` is saved |
| **Ammo Icon** | `AmmoConsumer.stack()` or player ammo type | `loadedBullet.getNewStack()` item texture |
| **Ammo Name** | `getAmmoDisplayName(data)` from consumer | `loadedBullet.getItemName()` |
| **Instant Display** | Yes - reads from ItemStack NBT | Partial - reads NBT but needs entity for full data |
| **Preloaded Ammo** | Defined in AmmoConsumer | `preloadedBullet` in gun JSON |

---

## The Problem with MTS Empty Gun Display

### Current Issue

When an MTS gun is **empty** (0 ammo), the HUD shows:
- ✅ Gun icon (from `iconTexture` or item texture)
- ✅ Gun name
- ✅ Ammo count = 0
- ❌ **No ammo type name** (because `loadedBullet` is null)
- ❌ **No ammo icon** (because no bullet to get item texture from)

### Why It Happens

```mermaid
flowchart TD
    A[Gun has ammo] -->|fires all ammo| B[Gun is empty]
    B --> C{loadedBullets.isEmpty?}
    C -->|Yes| D[loadedBullet = null]
    D --> E[HUD has no bullet info]

    C -->|No| F[loadedBullet = loadedBullets.get 0]
    F --> G[HUD shows bullet info]

    B --> H{lastLoadedBullet saved?}
    H -->|Yes| I[Can recover from NBT]
    H -->|No| J[Lost forever]
```

### Superb Warfare Solution

Superb Warfare **always** has ammo type info because:

1. `AmmoConsumer` is defined in the gun's JSON
2. `selectedAmmoConsumer()` always returns a valid consumer
3. Even with 0 ammo, `consumer.stack()` and `consumer.playerAmmoType` are available

```kotlin
// Superb Warfare - Always has ammo info
val consumer = data.selectedAmmoConsumer()  // Never null
val ammoStack = consumer.stack()            // Always valid
val ammoName = getAmmoDisplayName(data)     // Always returns something
```

### MTS Current Behavior

```java
// MTS - Can be null when empty
ItemBullet loadedBullet = gun.lastLoadedBullet;  // May be null!
String ammoName = loadedBullet != null ? loadedBullet.getItemName() : "";  // Empty string
```

---

## Recommended MTS Fixes

### Option 1: Use `preloadedBullet` as Fallback

When `lastLoadedBullet` is null, fall back to the gun's `preloadedBullet` definition:

```java
ItemBullet loadedBullet = gun.lastLoadedBullet;
if (loadedBullet == null && gun.definition.gun.preloadedBullet != null) {
    String[] parts = gun.definition.gun.preloadedBullet.split(":");
    loadedBullet = PackParser.getItem(parts[0], parts[1]);
}
```

### Option 2: Add `defaultAmmoType` to JSONGunHUD

Allow pack makers to specify a display-only ammo type:

```json
{
  "gunHUD": {
    "ammoDisplay": {
      "defaultAmmoType": "mtsofficialpack:extinguisherfoam"
    }
  }
}
```

### Option 3: Ensure `lastLoadedBullet` is Always Saved

Make sure PartGun saves `lastLoadedBullet` to NBT even after all ammo is consumed, and restores it on load.

---

## Summary

| Feature | Superb Warfare | MTS Current | MTS Recommended |
|---------|---------------|-------------|-----------------|
| Empty gun shows ammo type | ✅ Always | ❌ No | Use `preloadedBullet` fallback |
| Ammo icon when empty | ✅ Always | ❌ No | Use `preloadedBullet` fallback |
| Defined in gun JSON | ✅ AmmoConsumer | ⚠️ preloadedBullet only | Add to gunHUD or use existing |
| Multiple ammo types | ✅ AmmoConsumer list | ✅ loadedBullets list | Works |
| Instant HUD display | ✅ Yes | ✅ Yes (after fix) | Works |
