# Superb Warfare Weapon HUD System Analysis

This document analyzes how the Superb Warfare mod implements its weapon HUD system for potential adaptation to MTS content packs.

**Source Repository:** [github.com/Mercurows/SuperbWarfare](https://github.com/Mercurows/SuperbWarfare) (GPL-3.0)

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Data Layer - JSON Configuration](#data-layer---json-configuration)
3. [Item Layer - Weapon Classes](#item-layer---weapon-classes)
4. [Overlay Layer - HUD Rendering](#overlay-layer---hud-rendering)
5. [Screen Layer - Attachment Editing](#screen-layer---attachment-editing)
6. [Enums and Types](#enums-and-types)
7. [Complete Parameter Reference](#complete-parameter-reference)
8. [Implementation Notes for MTS](#implementation-notes-for-mts)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     SUPERB WARFARE ARCHITECTURE                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  DATA LAYER (JSON configs)                               │    │
│  │  Location: data/superbwarfare/sbw/guns/*.json            │    │
│  │  Defines: damage, fire modes, ammo, perks, reload, etc.  │    │
│  └─────────────────────────────────────────────────────────┘    │
│                              │                                   │
│                              ▼                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  ITEM LAYER (Kotlin/Java classes)                        │    │
│  │  Location: item/gun/**/*Item.java                        │    │
│  │  Overrides: hasCustomBarrel(), hasCustomScope(), etc.    │    │
│  └─────────────────────────────────────────────────────────┘    │
│                              │                                   │
│                              ▼                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  OVERLAY LAYER (Kotlin overlays)                         │    │
│  │  Location: client/overlay/*.kt                           │    │
│  │  Renders: AmmoBarOverlay, CrossHairOverlay, HeatBar...   │    │
│  └─────────────────────────────────────────────────────────┘    │
│                              │                                   │
│                              ▼                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  SCREEN LAYER (Java screens)                             │    │
│  │  Location: client/screens/*.java                         │    │
│  │  Provides: WeaponEditScreen for attachment modification  │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Data Layer - JSON Configuration

Weapon stats are defined in JSON files located at:
```
src/main/resources/data/superbwarfare/sbw/guns/<weapon_name>.json
```

### Example: AK-47 Configuration

```json
{
  "gunType": "RIFLE",
  "damage": 9,
  "headshotMultiplier": 2.0,
  "magazine": 30,
  "rpm": 600,
  "velocity": 36,
  "spread": 4,
  "bypassArmor": 0.2,
  "weight": 5,
  "soundRadius": 14,

  "fireModes": ["Semi", "Auto"],
  "defaultFireMode": "Auto",

  "reloadType": "Magazine",
  "normalReloadTime": 52,
  "emptyReloadTime": 65,

  "recoilX": 0.002,
  "recoilY": 0.012,

  "zoom": {
    "min": 1.25,
    "max": 6.0
  },

  "perks": [
    "kill_clip",
    "subsistence",
    "vorpal_weapon",
    "!micro_missile",
    "!butterfly_bullet"
  ]
}
```

### Example: Bocek Bow Configuration (Special Weapon)

```json
{
  "gunType": "SPECIAL",
  "damage": 48,
  "headshotMultiplier": 2.5,
  "magazine": 1,
  "spread": 4,
  "velocity": 24,
  "bypassArmor": 0.25,

  "reloadTime": 6,
  "autoReload": true,

  "damageDropoff": {
    "rate": 0.007,
    "startDistance": 100
  },

  "ammoType": "minecraft:arrow",

  "perks": [
    "cupid_arrow",
    "!micro_missile",
    "!butterfly_bullet"
  ],

  "customCrosshair": "textures/screens/bocek_crosshair.png",
  "icon": "textures/screens/icons/bocek.png"
}
```

---

## Item Layer - Weapon Classes

Each weapon has an item class that extends `GunItem` and defines attachment compatibility.

### Directory Structure

```
src/main/java/com/atsuishio/superbwarfare/item/gun/
├── GunItem.kt              # Base class
├── GunGeoItem.java         # GeckoLib integration
├── handgun/
│   ├── Glock17Item.java
│   ├── Glock18Item.java
│   └── M1911Item.java
├── rifle/
│   ├── AK47Item.java
│   ├── AK12Item.java
│   ├── HK416Item.java
│   └── M4Item.java
├── sniper/
│   ├── AWMItem.java
│   ├── M98BItem.java
│   └── SvdItem.java
├── shotgun/
│   ├── AA12Item.java
│   └── M870Item.java
├── smg/
│   ├── MP5Item.java
│   └── VectorItem.java
├── machinegun/
│   ├── M60Item.java
│   └── MinigunItem.java
├── launcher/
│   ├── RPGItem.java
│   └── JavelinItem.java
└── special/
    ├── BocekItem.java
    ├── TaserItem.java
    └── RepairToolItem.java
```

### Attachment Configuration Example

```java
public class AK47Item extends GunItem {

    // Enable attachment slots
    @Override
    public boolean hasCustomBarrel(GunData data) {
        return true;
    }

    @Override
    public boolean hasCustomScope(GunData data) {
        return true;
    }

    @Override
    public boolean hasCustomGrip(GunData data) {
        return true;
    }

    @Override
    public boolean hasCustomStock(GunData data) {
        return true;
    }

    @Override
    public boolean hasCustomMagazine(GunData data) {
        return true;
    }

    @Override
    public boolean canEditAttachments() {
        return true;
    }

    // Custom magazine capacities per attachment type
    @Override
    public int getCustomMagazine(int type) {
        return switch (type) {
            case 1 -> 15;   // Reduced mag
            case 2 -> 40;   // Extended mag
            default -> 30;  // Standard
        };
    }

    // Custom zoom levels per scope type
    @Override
    public float getCustomZoom(int type) {
        return switch (type) {
            case 1 -> 1.5f;   // Red dot
            case 2 -> 2.75f;  // ACOG
            case 3 -> 6.0f;   // Sniper scope
            default -> 1.25f; // Iron sights
        };
    }
}
```

### Base GunItem Methods

```kotlin
open class GunItem : Item {
    // Attachment availability (override in subclasses)
    open fun hasCustomBarrel(data: GunData): Boolean = false
    open fun hasCustomScope(data: GunData): Boolean = false
    open fun hasCustomGrip(data: GunData): Boolean = false
    open fun hasCustomStock(data: GunData): Boolean = false
    open fun hasCustomMagazine(data: GunData): Boolean = false

    // Valid attachment indices
    open val validBarrels: IntArray = intArrayOf()
    open val validScopes: IntArray = intArrayOf()
    open val validGrips: IntArray = intArrayOf()
    open val validStocks: IntArray = intArrayOf()
    open val validMagazines: IntArray = intArrayOf()

    // Special fire procedure (for bows, charged weapons)
    open fun useSpecialFireProcedure(): Boolean = false
}
```

---

## Overlay Layer - HUD Rendering

HUD overlays are registered in `ClientRenderHandler.kt` and rendered each frame.

### Overlay Registration

```kotlin
@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ClientRenderHandler {

    @SubscribeEvent
    fun registerGuiOverlays(event: RegisterGuiLayersEvent) {
        // Register overlays in layered hierarchy
        event.registerBelowAll(KillMessageOverlay.ID, KillMessageOverlay)
        event.registerBelow(loc(KillMessageOverlay.ID), ArmorPlateOverlay.ID, ArmorPlateOverlay)
        event.registerBelow(loc(ArmorPlateOverlay.ID), AmmoBarOverlay.ID, AmmoBarOverlay)
        event.registerBelow(loc(AmmoBarOverlay.ID), IFFOverlay.ID, IFFOverlay)
        event.registerBelow(loc(IFFOverlay.ID), VehicleTeamOverlay.ID, VehicleTeamOverlay)
        event.registerBelow(loc(VehicleTeamOverlay.ID), VehicleHudOverlay.ID, VehicleHudOverlay)
        event.registerBelow(loc(VehicleHudOverlay.ID), VehicleMainWeaponHudOverlay.ID, VehicleMainWeaponHudOverlay)
        event.registerBelow(loc(VehicleMainWeaponHudOverlay.ID), VehicleCrosshairOverlay.ID, VehicleCrosshairOverlay)
        event.registerBelow(loc(VehicleCrosshairOverlay.ID), CrossHairOverlay.ID, CrossHairOverlay)
        event.registerBelow(loc(CrossHairOverlay.ID), JavelinHudOverlay.ID, JavelinHudOverlay)
        event.registerBelow(loc(JavelinHudOverlay.ID), IglaHudOverlay.ID, IglaHudOverlay)
        event.registerBelow(loc(IglaHudOverlay.ID), StaminaOverlay.ID, StaminaOverlay)
        event.registerBelow(loc(StaminaOverlay.ID), HeatBarOverlay.ID, HeatBarOverlay)
        event.registerBelow(loc(HeatBarOverlay.ID), DroneHudOverlay.ID, DroneHudOverlay)
        event.registerBelow(loc(DroneHudOverlay.ID), MortarInfoOverlay.ID, MortarInfoOverlay)
        // ... more overlays
    }
}
```

### Available Overlay Classes

| Overlay | Purpose |
|---------|---------|
| `AmmoBarOverlay` | Weapon name, ammo count, fire mode, ammo type |
| `AmmoCountOverlay` | Backup ammo display |
| `ArmorPlateOverlay` | Armor status |
| `CrossHairOverlay` | Dynamic crosshair with hit indicators |
| `HeatBarOverlay` | Weapon heat/overheat display |
| `KillMessageOverlay` | Kill feed |
| `StaminaOverlay` | Stamina bar |
| `IFFOverlay` | Friend/foe identification |
| `JavelinHudOverlay` | Javelin missile lock-on |
| `IglaHudOverlay` | Igla missile lock-on |
| `DroneHudOverlay` | Drone camera view |
| `MortarInfoOverlay` | Mortar trajectory |
| `VehicleHudOverlay` | Vehicle status |
| `VehicleCrosshairOverlay` | Vehicle weapon crosshair |
| `VehicleMainWeaponHudOverlay` | Vehicle weapon ammo |
| `VehicleTeamOverlay` | Vehicle team indicators |

### AmmoBarOverlay Implementation

```kotlin
object AmmoBarOverlay : LayeredDraw.Layer {
    val ID = Mod.loc("ammo_bar")

    override fun render(gui: GuiGraphics, tickDelta: DeltaTracker) {
        val player = Minecraft.getInstance().player ?: return
        val stack = player.mainHandItem

        if (stack.item !is GunItem) return

        val data = GunData.from(stack)
        val screenWidth = gui.guiWidth()
        val screenHeight = gui.guiHeight()

        // Position in bottom-right corner
        val baseX = screenWidth - 120
        val baseY = screenHeight - 60

        // Render weapon icon
        val icon = data.icon
        RenderHelper.preciseBlit(gui, icon, baseX, baseY, 0f, 0f, 48f, 48f, 48f, 48f)

        // Render weapon name
        gui.drawString(font, data.displayName, baseX + 52, baseY, 0xFFFFFF)

        // Render fire mode indicator
        val fireMode = data.selectedFireModeInfo()
        val modeText = "[${KeyBindings.FIRE_MODE.key.displayName}] ${fireMode.mode.name}"
        gui.drawString(font, modeText, baseX + 52, baseY + 12, 0xAAAAAA)

        // Render ammo count (scaled 1.5x)
        val ammoCount = data.ammo.get()
        gui.pose().pushPose()
        gui.pose().scale(1.5f, 1.5f, 1f)
        gui.drawString(font, ammoCount.toString(), (baseX + 52) / 1.5f, (baseY + 24) / 1.5f, 0xFFFFFF)
        gui.pose().popPose()

        // Render ammo type indicators (if multiple types)
        val consumers = data.consumers
        if (consumers.size > 1) {
            for (i in consumers.indices) {
                val selected = i == data.selectedAmmoSlot
                val dotTexture = if (selected) SELECTED_DOT else UNSELECTED_DOT
                RenderHelper.preciseBlit(gui, dotTexture, baseX + 52 + (i * 8), baseY + 44, 0f, 0f, 4f, 4f, 4f, 4f)
            }
        }

        // Render backup ammo count
        if (DisplayConfig.ADVANCED_AMMO_HUD.get()) {
            val backupAmmo = data.getBackupAmmoCount(player)
            gui.drawString(font, "∞ $backupAmmo", baseX + 80, baseY + 24, 0x00FFFF)
        }
    }
}
```

---

## Screen Layer - Attachment Editing

The `WeaponEditScreen` provides a GUI for modifying weapon attachments.

### WeaponEditScreen Structure

```java
public class WeaponEditScreen extends Screen {

    // Texture resources for attachment slots
    private static final ResourceLocation BARREL = Mod.loc("textures/screens/edit/barrel.png");
    private static final ResourceLocation SCOPE = Mod.loc("textures/screens/edit/scope.png");
    private static final ResourceLocation GRIP = Mod.loc("textures/screens/edit/grip.png");
    private static final ResourceLocation STOCK = Mod.loc("textures/screens/edit/stock.png");
    private static final ResourceLocation MAGAZINE = Mod.loc("textures/screens/edit/magazine.png");
    private static final ResourceLocation AMMO = Mod.loc("textures/screens/edit/ammo.png");
    private static final ResourceLocation INVALID = Mod.loc("textures/screens/edit/invalid.png");
    private static final ResourceLocation BUTTON = Mod.loc("textures/screens/edit/button.png");
    private static final ResourceLocation BUTTON_HOVER = Mod.loc("textures/screens/edit/button_hover.png");
    private static final ResourceLocation SELECTED = Mod.loc("textures/screens/edit/selected.png");
    private static final ResourceLocation NOT_SELECTED = Mod.loc("textures/screens/edit/not_selected.png");

    @Override
    protected void init() {
        // Add navigation buttons for each attachment slot
        // Left/right buttons for: barrel, scope, grip, stock, magazine, ammo
        addRenderableWidget(new EditButton(/* barrel left */));
        addRenderableWidget(new EditButton(/* barrel right */));
        // ... 12 buttons total (2 per slot)
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderEdit(gui);
        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void renderEdit(GuiGraphics gui) {
        Player player = Minecraft.getInstance().player;
        ItemStack stack = player.getMainHandItem();

        if (!(stack.getItem() instanceof GunItem gunItem)) return;

        GunData data = GunData.from(stack);

        // Setup rendering state
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int x = this.width - 163;
        int y = this.height - 80;

        // Render semi-transparent background
        gui.fill(x - 10, y - 30, x + 150, y + 70, 0x80000000);

        // Render weapon name
        gui.drawString(font, data.getDisplayName(), x, y - 20, 0xFFFFFF);
        gui.hLine(x, x + 140, y - 10, 0xFFAAAAAA);

        // Render attachment slots in 2x3 grid
        int slotSize = 24;
        int spacing = 4;

        // Row 1: Barrel, Scope, Grip
        renderSlot(gui, BARREL, x, y, gunItem.hasCustomBarrel(data));
        renderSlot(gui, SCOPE, x + slotSize + spacing, y, gunItem.hasCustomScope(data));
        renderSlot(gui, GRIP, x + (slotSize + spacing) * 2, y, gunItem.hasCustomGrip(data));

        // Row 2: Stock, Magazine, Ammo
        renderSlot(gui, STOCK, x, y + slotSize + spacing, gunItem.hasCustomStock(data));
        renderSlot(gui, MAGAZINE, x + slotSize + spacing, y + slotSize + spacing, gunItem.hasCustomMagazine(data));
        renderAmmoSlot(gui, x + (slotSize + spacing) * 2, y + slotSize + spacing, data);

        // Restore rendering state
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private void renderSlot(GuiGraphics gui, ResourceLocation texture, int x, int y, boolean available) {
        RenderHelper.preciseBlit(gui, texture, x, y, 0, 0, 24, 24, 128, 128);
        if (!available) {
            RenderHelper.preciseBlit(gui, INVALID, x, y, 0, 0, 24, 24, 128, 128);
        }
    }

    private void renderAmmoSlot(GuiGraphics gui, int x, int y, GunData data) {
        RenderHelper.preciseBlit(gui, AMMO, x, y, 0, 0, 24, 24, 128, 128);

        List<AmmoConsumer> consumers = data.getConsumers();
        if (consumers.size() > 1) {
            int selected = data.getSelectedAmmoSlot();
            for (int i = 0; i < consumers.size(); i++) {
                ResourceLocation dot = (i == selected) ? SELECTED : NOT_SELECTED;
                RenderHelper.preciseBlit(gui, dot, x + 4 + (i * 5), y + 20, 0, 0, 4, 4, 16, 16);
            }
        }
    }

    // Nested button class for attachment navigation
    class EditButton extends Button {
        private final AttachmentType type;
        private final boolean isNext;

        @Override
        public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            ResourceLocation texture = isHovered ? BUTTON_HOVER : BUTTON;
            RenderHelper.preciseBlit(gui, texture, getX(), getY(), 0, 0, 16, 16, 64, 64);
        }

        @Override
        public void onPress() {
            // Send packet to server to change attachment
            PacketHandler.sendToServer(new ChangeAttachmentPacket(type, isNext));
        }
    }
}
```

---

## Enums and Types

### FireMode

```java
public enum FireMode {
    @SerializedName("Semi")
    SEMI("Semi"),

    @SerializedName("Burst")
    BURST("Burst"),

    @SerializedName("Auto")
    AUTO("Auto");

    private final String name;

    public static FireMode fromValue(String value) {
        for (FireMode mode : values()) {
            if (mode.name.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return SEMI; // Default
    }
}
```

### GunType

```java
public enum GunType {
    @SerializedName("Rifle")
    RIFLE,

    @SerializedName("Shotgun")
    SHOTGUN,

    @SerializedName("Sniper")
    SNIPER,

    @SerializedName("MachineGun")
    MACHINE_GUN,

    @SerializedName("Handgun")
    HANDGUN,

    @SerializedName("Smg")
    SMG,

    @SerializedName("DirectLauncher")
    DIRECT_LAUNCHER,

    @SerializedName("CurvedLauncher")
    CURVED_LAUNCHER,

    @SerializedName("Special")
    SPECIAL
}
```

### AttachmentType

```kotlin
enum class AttachmentType(val attachmentName: String) {
    Scope("scope"),
    Magazine("magazine"),
    Barrel("barrel"),
    Stock("stock"),
    Grip("grip")
}
```

### ReloadType

```java
public enum ReloadType {
    @SerializedName("Magazine")
    MAGAZINE,      // Standard magazine swap

    @SerializedName("Clip")
    CLIP,          // Stripper clip loading

    @SerializedName("Iterative")
    ITERATIVE      // Shell-by-shell (shotguns)
}
```

### AmmoConsumeType

```java
public enum AmmoConsumeType {
    INFINITE,      // Unlimited ammo (creative/special)
    EMPTY,         // No ammo needed (melee weapons)
    PLAYER_AMMO,   // Custom mod ammo system (@ammo_type in JSON)
    ITEM,          // Vanilla/mod items (minecraft:arrow)
    ENERGY         // Forge Energy (RF/FE power)
}
```

---

## Complete Parameter Reference

### Combat Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `damage` | float | 1.0 | Base damage per hit |
| `headshotMultiplier` | float | 1.5 | Damage multiplier for headshots |
| `spread` | float | 0.0 | Bullet spread (accuracy) |
| `velocity` | float | 10.0 | Projectile speed |
| `effectiveRange` | float | 128.0 | Max effective range (blocks) |
| `bypassArmor` | float | 0.0 | Armor penetration (0.0-1.0) |
| `projectileAmount` | int | 1 | Projectiles per shot (shotguns) |

### Firing Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `rpm` | int | 600 | Rounds per minute |
| `fireModes` | array | ["Semi"] | Available fire modes |
| `defaultFireMode` | string | "Semi" | Default selected mode |
| `burstAmount` | int | 3 | Shots per burst |
| `ammoCost` | int | 1 | Ammo consumed per shot |

### Ammunition Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `magazine` | int | 30 | Magazine capacity |
| `ammoType` | string | null | Item ID or @player_ammo |
| `consumers` | array | [] | Multiple ammo type configs |
| `autoReload` | boolean | false | Auto-reload when empty |

### Reload Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `reloadType` | string | "Magazine" | Magazine/Clip/Iterative |
| `normalReloadTime` | int | 40 | Reload time (ticks) |
| `emptyReloadTime` | int | 50 | Empty reload time |
| `boltActionTime` | int | 0 | Bolt action duration |
| `iterativeTime` | int | 10 | Per-shell load time |

### Recoil Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `recoilX` | float | 0.0 | Horizontal recoil |
| `recoilY` | float | 0.0 | Vertical recoil |
| `recoilYaw` | float | 0.0 | Rotational recoil (yaw) |
| `recoilPitch` | float | 0.0 | Rotational recoil (pitch) |

### Heat Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `heatPerShoot` | float | 0.0 | Heat generated per shot |
| `coolingPerTick` | float | 0.25 | Natural cooling rate |
| `waterCooling` | float | 1.0 | Cooling multiplier in water |
| `snowCooling` | float | 1.0 | Cooling multiplier in snow |
| `fireCooling` | float | -1.0 | Cooling (heating) near fire |
| `lavaCooling` | float | -2.0 | Cooling (heating) in lava |

### Visual Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `icon` | string | null | HUD icon texture path |
| `crosshair` | string | null | Custom crosshair texture |
| `zoom.min` | float | 1.0 | Minimum zoom level |
| `zoom.max` | float | 1.0 | Maximum zoom level |

### Audio Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `soundRadius` | float | 10.0 | Gunshot sound radius |
| `sounds.fire` | string | null | Fire sound event |
| `sounds.reload` | string | null | Reload sound event |
| `sounds.empty` | string | null | Empty click sound |

### Melee Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `meleeDamage` | float | 0.0 | Melee attack damage |
| `meleeDuration` | int | 0 | Melee animation duration |
| `meleeHitTime` | int | 0 | Frame when damage applies |

### Perk Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `perks` | array | [] | Available perks |
| `@perk_name` | - | - | Include perk |
| `!perk_name` | - | - | Exclude perk |

---

## Implementation Notes for MTS

### Adapting for MTS Content Packs

1. **JSON Schema Design**
   - Create a similar JSON structure for weapon definitions
   - Store in content pack directories
   - Parse during pack loading

2. **HUD Overlay System**
   - Use NeoForge's `RegisterGuiLayersEvent`
   - Create modular overlay components
   - Support per-weapon customization

3. **Attachment System**
   - Define attachment slots per weapon type
   - Allow content packs to specify valid attachments
   - Render attachment indicators dynamically

4. **Fire Mode System**
   - Implement fire mode cycling
   - Display current mode on HUD
   - Bind to configurable key

### Suggested MTS JSON Schema

```json
{
  "weapon": {
    "type": "gun",
    "category": "rifle",

    "stats": {
      "damage": 10,
      "rof": 600,
      "magazine": 30,
      "velocity": 40,
      "spread": 2
    },

    "fireModes": ["semi", "auto"],
    "defaultFireMode": "semi",

    "reload": {
      "type": "magazine",
      "time": 2.5,
      "emptyTime": 3.0
    },

    "ammo": {
      "type": "item",
      "item": "mts:rifle_ammo"
    },

    "attachments": {
      "scope": {
        "enabled": true,
        "valid": [0, 1, 2, 3]
      },
      "barrel": {
        "enabled": true,
        "valid": [0, 1]
      },
      "grip": {
        "enabled": false
      },
      "stock": {
        "enabled": true,
        "valid": [0, 1]
      },
      "magazine": {
        "enabled": true,
        "valid": [0, 1, 2]
      }
    },

    "hud": {
      "icon": "textures/hud/rifle_icon.png",
      "crosshair": "textures/hud/rifle_crosshair.png",
      "showAmmo": true,
      "showFireMode": true,
      "showAttachments": true
    }
  }
}
```

---

## References

- **Repository:** https://github.com/Mercurows/SuperbWarfare
- **License:** GPL-3.0
- **Minecraft Version:** 1.21.1 (NeoForge)
- **Dependencies:** GeckoLib, Cloth Config

---

*Document created for MTS development reference*
