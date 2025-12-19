# Handheld Gun First-Person Rendering Analysis

## Current Implementation Overview

MTS uses a custom rendering approach for handheld guns that differs from vanilla Minecraft item rendering.

### Key Components

| Component | File | Purpose |
|-----------|------|---------|
| EntityPlayerGun | `entities/instances/EntityPlayerGun.java` | Invisible wrapper entity that positions the gun |
| HumanoidModelMixin | `mcinterface1211/mixin/client/HumanoidModelMixin.java` | Adjusts player arm rotation |
| InterfaceEventsEntityRendering | `mcinterface1211/InterfaceEventsEntityRendering.java` | Disables vanilla hand/arm rendering |

### Positioning Flow (EntityPlayerGun.java:237-275)

```
1. Choose offset: handHeldNormalOffset OR handHeldAimedOffset (from gun JSON)
2. Add handHeldModelOffset (optional custom positioning)
3. Mirror X-axis if left-handed
4. Apply PITCH rotation (X-axis)
5. Adjust to arm center: (-0.3125 X, -0.375 Y from shoulder)
6. Apply YAW rotation
7. Scale by player height
8. Add player's head position
```

---

## Known Issues

### 1. No Visible Arms in First-Person

**Location:** `InterfaceEventsEntityRendering.java:148-162`

Both `RenderHandEvent` and `RenderArmEvent` are cancelled when holding a gun:
```java
if ((entity != null && entity.isValid && entity.activeGun != null) || CameraSystem.activeCamera != null) {
    event.setCanceled(true);
}
```

**Result:** Player sees only the gun model, no arm/hand visible.

### 2. Gun Drifts When Looking Up/Down

**Location:** `EntityPlayerGun.java:253-259`

```java
// Pitch rotation applied BEFORE arm offset
handRotation.setToZero().rotateX(playerRotation.angles.x);
position.rotate(handRotation);  // Rotates everything by pitch
position.add(-0.3125, -0.375, 0);  // Arm offset added AFTER
```

**Problem:** The gun position is rotated by pitch before the arm offset is applied. This causes the gun to shift away from the expected position when looking up or down.

**Expected:** Pitch should affect arm rotation only, not gun position relative to hand.

### 3. View Bobbing Not Separated

**Location:** `EntityPlayerGun.java:275`

```java
position.add(player.getHeadPosition());
```

**Problem:** Gun position directly follows head position. When player view bobs (walking), the gun bobs identically, which looks unnatural.

**Expected:** Gun should have slight independent motion or reduced bobbing.

### 4. Arm Animation Uses Trigonometric Approximation

**Location:** `HumanoidModelMixin.java:74-95`

```java
double armPitchOffset = Math.toRadians(-90 + entity.getXRot())
                      - Math.asin(heldVector.y / heldVectorLength);
double armYawOffset = -Math.atan2(heldVector.x / heldVectorLength,
                                   heldVector.z / heldVectorLength);
```

**Problem:** Arm rotation is calculated mathematically from the gun offset vector, not using proper inverse kinematics. This can result in unnatural arm poses.

### 5. Clipping with Seated Vehicles

When player sits in a vehicle while holding a handheld gun, the gun model can clip through the seated entity. No special handling exists for this case.

---

## Comparison with Modern Gun Mods (e.g., Superb Warfare)

| Feature | MTS Current | Superb Warfare |
|---------|-------------|----------------|
| Visible arms in first-person | No | Yes |
| Arm animation | Trigonometric approximation | Proper IK/FK |
| View bob handling | Gun follows head directly | Separated |
| Pitch handling | Gun position rotates | Arm rotates, gun stays in hand |
| Two-handed support | Basic (both arms rotated) | Full hand positioning |

---

## Potential Improvements

### Priority 1: Fix Pitch Rotation (Medium Effort)

Change the order of operations so pitch affects arm rotation, not gun position:

```java
// Apply arm offset FIRST
position.add(player.isRightHanded() ? -0.3125 : 0.3125, -0.375, 0);
// THEN apply pitch rotation to the arm position
handRotation.setToZero().rotateX(playerRotation.angles.x);
position.rotate(handRotation);
```

### Priority 2: Re-enable Arm Rendering (Medium Effort)

Instead of cancelling arm events, position the vanilla arm to match the gun:
- Calculate proper arm bone positions
- Allow arm to render with custom rotation
- Apply gun offset to hand position

### Priority 3: Separate View Bobbing (Low Effort)

Add configurable view bob dampening:
```java
// Reduce bob effect on gun
Point3D headPos = player.getHeadPosition();
Point3D dampedPos = headPos.copy().multiply(0.3); // 30% of head movement
position.add(dampedPos);
```

### Priority 4: Seat Clipping Prevention (Medium Effort)

Check if player is seated and adjust gun position:
```java
if (player.isSeated()) {
    // Move gun forward/up to prevent clipping
    position.add(0, 0.2, 0.3);
}
```

---

## JSON Configuration Reference

### Current Offset Fields

```json
{
  "gun": {
    "handHeld": true,
    "handHeldNormalOffset": [-0.1, 0.1, 0.7],
    "handHeldAimedOffset": [0.26, 0.0, 0.5],
    "handHeldModelOffset": [0, 0, 0]
  }
}
```

| Field | Purpose |
|-------|---------|
| `handHeldNormalOffset` | Gun position when hip-firing [X, Y, Z] |
| `handHeldAimedOffset` | Gun position when aiming down sights |
| `handHeldModelOffset` | Additional offset for model alignment |

### Offset Coordinate System

- **X**: Left/Right (negative = right for right-handed)
- **Y**: Up/Down (positive = up)
- **Z**: Forward/Back (positive = forward, toward crosshair)

---

## Related Files

- `mccore/src/main/java/minecrafttransportsimulator/entities/instances/EntityPlayerGun.java`
- `mccore/src/main/java/minecrafttransportsimulator/entities/instances/PartGun.java`
- `neoforge/src/main/java/mcinterface1211/mixin/client/HumanoidModelMixin.java`
- `neoforge/src/main/java/mcinterface1211/InterfaceEventsEntityRendering.java`
- `mccore/src/main/java/minecrafttransportsimulator/systems/CameraSystem.java`
