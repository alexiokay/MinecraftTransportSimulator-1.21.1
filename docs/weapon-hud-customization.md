# Weapon HUD Customization Guide

This guide explains how content pack creators can customize the weapon HUD overlay for handheld guns in MTS.

## Overview

The weapon HUD displays:
- Gun icon/silhouette
- Gun name
- Ammo count
- Ammo type name
- Ammo type icon (in brackets)
- Fire mode indicator

## HUD Layout Diagram

```
                                    ┌─────────────────────────┐
                                    │     Fire Extinguisher   │ ← Gun Name
                                    │     Extinguisher Foam   │ ← Ammo Type Name
                                    │                         │
                                    │                         │
    Gun Icon ──────────────────────►│  ┌───┐                  │
    (iconTexture)                   │  │ 🧯 │      500        │ ← Ammo Count
    64x16 pixels                    │  └───┘       ∞          │ ← Backup Ammo
                                    │                         │
                                    │                         │
                                    │  [N] ═    └[ 🪣 ]       │
                                    │   ▲           ▲         │
                                    │   │           │         │
                                    └───│───────────│─────────┘
                                        │           │
                                   Fire Mode    Bullet Icon
                                                (hudIcon)
                                                12x12 pixels
```

### Customizable Icons

| Icon | JSON Location | Property | Size | Fallback |
|------|---------------|----------|------|----------|
| **Gun Icon** | Gun JSON → `gunHUD.weaponDisplay` | `iconTexture` | 64x16 px | Item inventory texture |
| **Bullet Icon** | Bullet JSON → `bullet` | `hudIcon` | 12x12 px | Item inventory texture |

## Gun HUD Configuration

Add a `gunHUD` section to your gun part JSON to enable and customize the HUD.

### Basic Structure

```json
{
  "gun": {
    "handHeld": true,
    ...
  },
  "gunHUD": {
    "enabled": true,
    "weaponDisplay": {
      "showIcon": true,
      "iconTexture": "yourpack:textures/gun_icon/your_gun_icon.png",
      "showName": true,
      "nameColor": "FFFFFF"
    },
    "ammoDisplay": {
      "showCount": true,
      "countColor": "FFFFFF",
      "countEmptyColor": "FF0000",
      "showBar": false,
      "showBackupAmmo": true,
      "backupAmmoColor": "00FFFF"
    }
  }
}
```

### Gun Icon (`weaponDisplay.iconTexture`)

| Property | Description |
|----------|-------------|
| **Format** | `"packid:path/to/texture.png"` |
| **Size** | 64x16 pixels recommended |
| **Fallback** | If not specified, uses the gun's inventory item texture |

**Example:**
```json
"iconTexture": "mtsofficialpack:textures/gun_icon/fireextinguisher_icon.png"
```

**File location:**
```
assets/mtsofficialpack/textures/gun_icon/fireextinguisher_icon.png
```

## Bullet HUD Icon Configuration

Add a `hudIcon` field to your bullet JSON to customize how the ammo type appears in the HUD.

### Basic Structure

```json
{
  "bullet": {
    "types": ["water"],
    "diameter": 10.01,
    "quantity": 500,
    "hudIcon": "yourpack:textures/hud/your_bullet_icon"
  }
}
```

### Bullet HUD Icon (`bullet.hudIcon`)

| Property | Description |
|----------|-------------|
| **Format** | `"packid:path/to/texture"` (without .png extension) |
| **Size** | 12x12 pixels recommended |
| **Fallback** | If not specified, uses the bullet's inventory item texture |

**Example:**
```json
"hudIcon": "mtsofficialpack:textures/hud/foam_icon"
```

**File location:**
```
assets/mtsofficialpack/textures/hud/foam_icon.png
```

## When to Use Custom Icons

### Use Custom Gun Icon When:
- Your gun's inventory icon is too detailed for HUD display
- You want a stylized silhouette like modern FPS games
- The 3D item render doesn't look good at small sizes

### Use Custom Bullet Icon When:
- The bullet item texture is misleading (e.g., foam canister vs spray icon)
- You want a simplified icon for readability
- The inventory texture is too complex at 12x12 size

### Don't Need Custom Icons When:
- Your item textures already look good at small sizes
- You want consistency between inventory and HUD
- You're creating standard ammunition (bullets, shells, etc.)

## File Structure Example

```
assets/yourpack/
├── jsondefs/
│   ├── parts/
│   │   └── gun_pistol.json          # Gun with gunHUD config
│   └── bullets/
│       └── bullet_9mm.json          # Bullet with hudIcon config
└── textures/
    ├── gun_icon/
    │   └── pistol_icon.png          # 64x16 gun silhouette
    ├── hud/
    │   └── 9mm_icon.png             # 12x12 bullet icon
    └── item/
        ├── gun_pistol.png           # Standard inventory texture
        └── bullet_9mm.png           # Standard inventory texture
```

## Complete Example

### Gun JSON (gun_pistol.json)
```json
{
  "generic": {
    "type": "gun_hand_pistol"
  },
  "gun": {
    "handHeld": true,
    "capacity": 15,
    "preloadedBullet": "yourpack:bullet_9mm:1",
    "diameter": 9.0,
    "muzzleVelocity": 350
  },
  "gunHUD": {
    "enabled": true,
    "weaponDisplay": {
      "showIcon": true,
      "iconTexture": "yourpack:textures/gun_icon/pistol_icon.png",
      "showName": true,
      "nameColor": "FFFFFF"
    },
    "ammoDisplay": {
      "showCount": true,
      "countColor": "FFFFFF",
      "countEmptyColor": "FF0000"
    }
  }
}
```

### Bullet JSON (bullet_9mm.json)
```json
{
  "bullet": {
    "types": [],
    "diameter": 9.0,
    "caseLength": 19.0,
    "quantity": 30,
    "damage": 5.0,
    "hudIcon": "yourpack:textures/hud/9mm_icon"
  },
  "definitions": [
    {
      "subName": "",
      "name": "9mm Parabellum"
    }
  ]
}
```

## Texture Specifications

### Gun Icon Texture
| Property | Value |
|----------|-------|
| Dimensions | 64x16 pixels |
| Format | PNG with transparency |
| Style | Silhouette or simplified side view |
| Background | Transparent |

### Bullet HUD Icon Texture
| Property | Value |
|----------|-------|
| Dimensions | 12x12 pixels |
| Format | PNG with transparency |
| Style | Simple, recognizable shape |
| Background | Transparent |

## Tips

1. **Test at actual size** - Create your icons and view them at 100% zoom to ensure readability
2. **Use high contrast** - The HUD uses white text, so icons should be visible against various backgrounds
3. **Keep it simple** - Complex details are lost at small sizes
4. **Consistency** - Use similar styles across your pack's HUD icons
5. **Fallback testing** - Test without custom icons to ensure the fallback looks acceptable

## Troubleshooting

### Icon Not Showing
- Check file path matches JSON exactly
- Ensure PNG file exists in correct location
- Verify pack ID is correct in the path

### Icon Looks Stretched/Squished
- Gun icons should be 64x16 (4:1 ratio)
- Bullet icons should be 12x12 (1:1 ratio)

### Icon Has Wrong Colors
- Ensure PNG has proper transparency
- Check that colors are not inverted
