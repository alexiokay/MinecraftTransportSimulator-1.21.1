# In-Car Player Animation Fixes - What We Learned

## Problem Statement
Player in MTS vehicles:
- Can turn 360° with mouse/camera (should be locked facing forward)
- No sitting position (legs should be bent 90°)
- No steering animations (arms should move with A/D keys)
- Should behave like original mod with proper driving position

## What We Tried

### 1. PlayerAnimator Approach (FAILED)
- **What**: Used PlayerAnimator 2.0.1+1.21.1 API with animation layers
- **Implementation**:
  - Body lock animation (priority 2000) to override camera rotation
  - Sitting animation (priority 1900) for leg/arm positioning
  - Steering animation (priority 1700) for arm movement
  - Head constraint (priority 1800) for ±80° head rotation limit
- **Why it failed**:
  - Mixin conflicts with other mods (PlayerAnimator vs FirstPerson mod)
  - PlayerAnimator overrides weren't actually preventing camera rotation
  - Animations were being applied but not visually effective
  - Some attempts made player invisible or upside-down
- **Evidence**:
  - Logs showed steering input detection working
  - Animation layer calls were happening
  - But no visual changes in-game

### 2. MTS HumanoidModelMixin Approach (FAILED)
- **What**: Used the original MTS animation system via HumanoidModelMixin.java
- **Implementation**:
  - Found existing HumanoidModelMixin with correct animation code:
    - `model.rightArm.xRot = Math.toRadians(-75 + turningAngle)`
    - `model.leftArm.xRot = Math.toRadians(-75 - turningAngle)`
    - `model.leftLeg.xRot = Math.toRadians(-90)` for sitting
  - Added HumanoidModelMixin to mts.mixins.json client list
  - Removed PlayerAnimator dependency completely
- **Why it failed**:
  - Mixin wasn't being loaded (no debug messages in logs)
  - `playerTweaks` config was true but some mod was auto-disabling it
  - Even forcing the condition with `if (true ||` didn't work
  - Mixin registration issue or NeoForge 1.21.1 incompatibility

## Key Discoveries

### 1. Original System Exists
- MTS already has a complete animation system in `HumanoidModelMixin.java`
- The code looks correct and should work
- Includes debug logging that never appeared in logs

### 2. Config Issues
- `playerTweaks` setting controls whether animations work
- Config shows `true` but comment says "Automatically set to false if some mods are detected"
- Some other mod is likely disabling player animations

### 3. Mixin Loading Problems
- Added `"client.HumanoidModelMixin"` to `mts.mixins.json`
- Mixin never appeared in startup logs
- No debug messages from mixin ever showed up
- Suggests fundamental mixin loading issue

### 4. Partial Success Mentioned
User mentioned: "many prompts ago we achieved something player model was still and the head rotation was blocked to 40 degrees or something. before we decided to give a try to playeranimator"

**This suggests there WAS a working approach earlier that we abandoned!**

## What Needs Investigation

### 1. Find the Working Approach
- Look back at earlier attempts before PlayerAnimator
- What made the "player model still and head rotation blocked to 40 degrees"?
- Was it a different mixin approach?
- Was it a different configuration?

### 2. Debug Mixin Loading
- Why isn't HumanoidModelMixin being loaded?
- Is there a NeoForge 1.21.1 compatibility issue?
- Are there missing dependencies for mixin loading?

### 3. Mod Conflict Resolution
- What mod is auto-disabling `playerTweaks`?
- Can we force enable it or work around the conflict?
- Are there other animation mods interfering?

## Current Status
- PlayerAnimator completely removed
- HumanoidModelMixin added to mixins but not loading
- Need to investigate the earlier working approach
- Player animations still not working at all

## Next Steps
1. Find what was working "many prompts ago" before PlayerAnimator attempt
2. Debug why HumanoidModelMixin isn't loading
3. Investigate mod conflicts affecting `playerTweaks`
4. Consider alternative animation approaches if mixins fundamentally broken