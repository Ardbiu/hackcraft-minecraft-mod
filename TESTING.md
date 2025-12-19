# Testing Guide

> [!WARNING]
> **Java Runtime Missing**: My checks indicate you do not have a Java Runtime installed or configured on your path.
> You **must** install JDK 21 to build or run this mod.
> - **Mac**: `brew install openjdk@21` (if using Homebrew) or download from [Adoptium](https://adoptium.net/).

## 1. Launching the Game
Once Java is installed, run the following command in your terminal to start the Minecraft Client with your mod loaded:

```bash
./gradlew runClient
```

## 2. Setting Up
1. Create a **Singleplayer** world (Creative Mode is easiest).
2. Enable **Cheats** (Allow Cheats: ON).

## 3. Feature Verification

### 🧪 Glowstep Boots
**Command**: `/give @s modid:glowstep_boots`

1. **Equip**: Put the boots in your armor slot.
2. **Walk**: Move around.
3. **Verify**:
   - [ ] Do invisible lights appear at your feet? (Check F3 screen for "Light: 12" or similar).
   - [ ] Stop moving. Do lights stop appearing?
   - [ ] Wait 10 seconds. Do the lights disappear?

### 🎣 Grapple Hook
**Command**: `/give @s modid:grapple_hook`

1. **Aim**: Look at a block about 10-20 blocks away.
2. **Use**: Right-click.
3. **Verify**:
   - [ ] Do you hear a sound and see particles?
   - [ ] Are you pulled towards the block?
   - [ ] Do you stop before hitting the wall?
   - [ ] Does the cooldown (2 seconds) activate?

### 🗿 Recall Totem
**Command**: `/give @s modid:recall_totem`

1. **Save**: Stand in a specific spot. Right-click.
   - [ ] Message: "Recall point saved!"
2. **Move**: Fly/walk far away.
3. **Recall**: Right-click.
   - [ ] Do you teleport back to the EXACT saved spot?
   - [ ] Is your facing direction (yaw/pitch) restored?
4. **Clear**: Sneak + Right-click.
   - [ ] Message: "Recall point cleared."
