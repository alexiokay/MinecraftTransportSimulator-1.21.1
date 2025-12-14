# MTS Testing Strategy

This document outlines the automated testing strategy for MinecraftTransportSimulator (Immersive Vehicles).

## Table of Contents

- [Current State](#current-state)
- [Testing Architecture](#testing-architecture)
- [Test Categories](#test-categories)
- [Priority Test Targets](#priority-test-targets)
- [Implementation Guide](#implementation-guide)
- [CI/CD Pipeline](#cicd-pipeline)
- [What NOT to Test](#what-not-to-test)
- [Roadmap](#roadmap)

---

## Current State

| Aspect | Status |
|--------|--------|
| Test directories | None |
| Test dependencies | None |
| CI pipeline | None |
| Code coverage | 0% |

**Opportunity**: The `mccore` module (282 files) is cleanly separated from Minecraft dependencies, making it ideal for unit testing without spinning up a game instance.

---

## Testing Architecture

### Project Structure

```
MinecraftTransportSimulator-1.21.1/
├── mccore/
│   └── src/
│       ├── main/java/minecrafttransportsimulator/
│       └── test/java/minecrafttransportsimulator/    ← Unit Tests
│           ├── baseclasses/
│           │   ├── Point3DTest.java
│           │   ├── RotationMatrixTest.java
│           │   ├── BezierCurveTest.java
│           │   ├── ColorRGBTest.java
│           │   ├── BoundingBoxTest.java
│           │   └── TransformationMatrixTest.java
│           ├── packloading/
│           │   ├── JSONParserTest.java
│           │   ├── PackParserTest.java
│           │   └── LegacyCompatSystemTest.java
│           ├── packets/
│           │   ├── PacketSerializationTest.java
│           │   └── PacketRoundTripTest.java
│           ├── jsondefs/
│           │   └── JSONValidationTest.java
│           └── systems/
│               ├── ConfigSystemTest.java
│               └── LanguageSystemTest.java
├── neoforge/
│   └── src/
│       └── test/java/mcinterface1211/
│           └── gametest/                             ← GameTests
│               ├── VehicleSpawnTest.java
│               ├── FuelPumpTest.java
│               └── PacketDeliveryTest.java
└── build.gradle.kts
```

### Testing Layers

```
┌─────────────────────────────────────────────────────────────────┐
│                     Manual Testing                               │
│            (Rendering, Shaders, Gameplay Feel)                   │
├─────────────────────────────────────────────────────────────────┤
│                   GameTest Framework                             │
│     (Vehicle Physics, Block Interactions, Network Sync)          │
├─────────────────────────────────────────────────────────────────┤
│                  Integration Tests                               │
│        (Pack Loading, Config System, Resource Loading)           │
├─────────────────────────────────────────────────────────────────┤
│                     Unit Tests                                   │
│    (Math, JSON Parsing, Packet Serialization, Data Validation)   │
└─────────────────────────────────────────────────────────────────┘
          ▲                                              ▲
       Faster                                         Slower
       Cheaper                                     More Realistic
```

---

## Test Categories

### 1. Pure Java Unit Tests (No Minecraft)

**What to test:**
- Mathematical operations (vectors, matrices, rotations)
- JSON parsing and serialization
- Packet encoding/decoding
- Configuration logic
- Data validation

**Frameworks:**
- JUnit 5 (Jupiter)
- AssertJ (fluent assertions)
- Mockito (mocking mcinterface layer)

**Characteristics:**
- Fast execution (milliseconds)
- No Minecraft boot required
- Run on every commit
- High code coverage potential

### 2. Data-Driven Tests (JSON Validation)

**What to test:**
- Pack JSON schema compliance
- Required field presence
- Value range validation
- Cross-reference integrity

**Approach:**
- Load JSON with GSON
- Validate against expected structure
- Snapshot testing for complex definitions

### 3. GameTest Framework (In-Game Logic)

**What to test:**
- Vehicle spawning and physics
- Block tile entity interactions
- Fuel pump operations
- Network packet delivery
- Redstone integration

**Characteristics:**
- Real Minecraft environment
- Deterministic and repeatable
- CI-friendly (headless server)
- Slower than unit tests

### 4. Integration Tests

**What to test:**
- Full pack loading pipeline
- Config file loading/saving
- Resource loading with fallbacks
- Multi-mod compatibility

---

## Priority Test Targets

### Tier 1: Critical (Implement First)

| Target | Location | Why Critical |
|--------|----------|--------------|
| `Point3D` | `baseclasses/Point3D.java` | Used everywhere for positions, rotations |
| `RotationMatrix` | `baseclasses/RotationMatrix.java` | Core of vehicle orientation |
| `JSONParser` | `packloading/JSONParser.java` | All pack loading depends on this |
| `ColorRGB` | `baseclasses/ColorRGB.java` | Color parsing for customization |

**Tests to write:**
```java
// Point3DTest.java
- testAdd()
- testSubtract()
- testMultiply()
- testDotProduct()
- testCrossProduct()
- testNormalize()
- testLength()
- testRotateByMatrix()
- testDistanceTo()
- testEquals()

// RotationMatrixTest.java
- testSetToAngles()
- testMultiply()
- testInverse()
- testAnglesRoundTrip()
- testIdentityMatrix()

// JSONParserTest.java
- testPoint3DAdapter()
- testColorRGBAdapter()
- testRotationMatrixAdapter()
- testBooleanAdapter()
- testExportAndReimport()
- testMalformedJSONError()
```

### Tier 2: Important (Implement Second)

| Target | Location | Why Important |
|--------|----------|---------------|
| `BezierCurve` | `baseclasses/BezierCurve.java` | Road/path generation |
| `BoundingBox` | `baseclasses/BoundingBox.java` | Collision detection |
| Packet serialization | `packets/` | Network stability |
| `ConfigSystem` | `systems/ConfigSystem.java` | Settings persistence |

**Tests to write:**
```java
// BezierCurveTest.java
- testGetPointAt()
- testGetRotationAt()
- testCachedPointsConsistency()
- testOffsetCurveGeneration()

// BoundingBoxTest.java
- testIntersects()
- testContainsPoint()
- testUpdateToEntity()
- testGlobalCenter()

// PacketSerializationTest.java (for each packet type)
- testWriteAndReadRoundTrip()
- testBufferBoundaries()
- testNullHandling()
```

### Tier 3: Valuable (Implement Third)

| Target | Location | Why Valuable |
|--------|----------|--------------|
| `PackParser` | `packloading/PackParser.java` | Pack content registration |
| `LegacyCompatSystem` | `packloading/LegacyCompatSystem.java` | Backwards compatibility |
| JSON definitions | `jsondefs/*.java` | Data integrity |
| `LanguageSystem` | `systems/LanguageSystem.java` | Localization |

### Tier 4: GameTest (Implement Last)

| Target | What to Test |
|--------|--------------|
| Vehicle spawning | Entity creation, initial state |
| Fuel pump | Dispensing fuel to vehicles |
| Crafting benches | Recipe execution |
| Road blocks | Placement and connections |
| Packet delivery | Server-client synchronization |

---

## Implementation Guide

### Step 1: Add Test Dependencies

**mccore/build.gradle:**
```kotlin
plugins {
    id("java-library")
}

dependencies {
    // Existing dependencies...

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("io.netty:netty-buffer:4.1.9.Final")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
```

### Step 2: Create Test Directory Structure

```bash
mkdir -p mccore/src/test/java/minecrafttransportsimulator/baseclasses
mkdir -p mccore/src/test/java/minecrafttransportsimulator/packloading
mkdir -p mccore/src/test/java/minecrafttransportsimulator/packets
mkdir -p mccore/src/test/java/minecrafttransportsimulator/systems
mkdir -p mccore/src/test/resources
```

### Step 3: Example Test Implementations

**Point3DTest.java:**
```java
package minecrafttransportsimulator.baseclasses;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class Point3DTest {

    @Test
    @DisplayName("add() returns correct sum of two points")
    void addReturnsCorrectSum() {
        Point3D a = new Point3D(1, 2, 3);
        Point3D b = new Point3D(4, 5, 6);

        Point3D result = a.copy().add(b);

        assertThat(result.x).isEqualTo(5);
        assertThat(result.y).isEqualTo(7);
        assertThat(result.z).isEqualTo(9);
    }

    @Test
    @DisplayName("dotProduct() returns 0 for perpendicular vectors")
    void dotProductPerpendicularIsZero() {
        Point3D a = new Point3D(1, 0, 0);
        Point3D b = new Point3D(0, 1, 0);

        assertThat(a.dotProduct(b)).isEqualTo(0);
    }

    @Test
    @DisplayName("crossProduct() follows right-hand rule")
    void crossProductRightHandRule() {
        Point3D x = new Point3D(1, 0, 0);
        Point3D y = new Point3D(0, 1, 0);

        Point3D result = x.copy().crossProduct(y);

        assertThat(result.x).isEqualTo(0);
        assertThat(result.y).isEqualTo(0);
        assertThat(result.z).isEqualTo(1);
    }

    @Test
    @DisplayName("normalize() produces unit vector")
    void normalizeProducesUnitVector() {
        Point3D p = new Point3D(3, 4, 0);

        p.normalize();

        assertThat(p.length()).isCloseTo(1.0, within(0.0001));
    }

    @Test
    @DisplayName("rotation preserves vector length")
    void rotationPreservesLength() {
        Point3D p = new Point3D(1, 2, 3);
        double originalLength = p.length();
        RotationMatrix rotation = new RotationMatrix();
        rotation.setToAngles(new Point3D(45, 30, 15));

        p.rotate(rotation);

        assertThat(p.length()).isCloseTo(originalLength, within(0.0001));
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 0, 1, 1, 1, 1.732",
        "1, 0, 0, 0, 0, 0, 1.0",
        "3, 4, 0, 0, 0, 0, 5.0"
    })
    @DisplayName("distanceTo() calculates correctly")
    void distanceToCalculatesCorrectly(
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double expected) {
        Point3D a = new Point3D(x1, y1, z1);
        Point3D b = new Point3D(x2, y2, z2);

        assertThat(a.distanceTo(b)).isCloseTo(expected, within(0.001));
    }
}
```

**JSONParserTest.java:**
```java
package minecrafttransportsimulator.packloading;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

class JSONParserTest {

    @Test
    @DisplayName("Point3D serialization round-trips correctly")
    void point3DRoundTrip() {
        Point3D original = new Point3D(1.5, 2.5, 3.5);

        String json = JSONParser.exportStream(original);
        Point3D parsed = JSONParser.parseStream(json, Point3D.class, null, null);

        assertThat(parsed.x).isEqualTo(original.x);
        assertThat(parsed.y).isEqualTo(original.y);
        assertThat(parsed.z).isEqualTo(original.z);
    }

    @Test
    @DisplayName("ColorRGB parses hex string correctly")
    void colorRGBParsesHex() {
        String json = "{ \"color\": \"#FF5500\" }";

        // Test ColorRGB type adapter
        TestColorHolder holder = JSONParser.parseStream(json, TestColorHolder.class, null, null);

        assertThat(holder.color.red).isEqualTo(255);
        assertThat(holder.color.green).isEqualTo(85);
        assertThat(holder.color.blue).isEqualTo(0);
    }

    @Test
    @DisplayName("Malformed JSON throws with helpful message")
    void malformedJSONThrowsHelpful() {
        String badJson = "{ \"x\": 1, \"y\": }";

        assertThatThrownBy(() ->
            JSONParser.parseStream(badJson, Point3D.class, null, null)
        ).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Boolean adapter handles string 'true'")
    void booleanAdapterHandlesString() {
        String json = "{ \"enabled\": \"true\" }";

        TestBoolHolder holder = JSONParser.parseStream(json, TestBoolHolder.class, null, null);

        assertThat(holder.enabled).isTrue();
    }

    // Test helper classes
    static class TestColorHolder {
        ColorRGB color;
    }

    static class TestBoolHolder {
        Boolean enabled;
    }
}
```

**PacketSerializationTest.java:**
```java
package minecrafttransportsimulator.packets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

class PacketSerializationTest {

    @Test
    @DisplayName("String write/read round-trips correctly")
    void stringRoundTrip() {
        ByteBuf buf = Unpooled.buffer();
        String original = "test_variable_name";

        // Write
        writeString(buf, original);

        // Read
        String result = readString(buf);

        assertThat(result).isEqualTo(original);
        buf.release();
    }

    @Test
    @DisplayName("Double write/read preserves precision")
    void doublePreservesPrecision() {
        ByteBuf buf = Unpooled.buffer();
        double original = 3.141592653589793;

        buf.writeDouble(original);
        double result = buf.readDouble();

        assertThat(result).isEqualTo(original);
        buf.release();
    }

    @Test
    @DisplayName("Point3D coordinates serialize correctly")
    void point3DSerializes() {
        ByteBuf buf = Unpooled.buffer();

        // Write point data
        buf.writeDouble(1.5);
        buf.writeDouble(2.5);
        buf.writeDouble(3.5);

        // Read back
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();

        assertThat(x).isEqualTo(1.5);
        assertThat(y).isEqualTo(2.5);
        assertThat(z).isEqualTo(3.5);
        buf.release();
    }

    // Helper methods matching APacketBase implementation
    private void writeString(ByteBuf buf, String str) {
        byte[] bytes = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    private String readString(ByteBuf buf) {
        int length = buf.readInt();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
```

### Step 4: GameTest Setup (NeoForge)

**neoforge/build.gradle additions:**
```kotlin
runs {
    gameTestServer {
        workingDirectory = project.file("run")
        property("neoforge.enabledGameTestNamespaces", "mts")

        mods {
            mts {
                source(sourceSets.main.get())
            }
        }
    }
}
```

**Example GameTest:**
```java
package mcinterface1211.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder("mts")
public class VehicleSpawnTest {

    @GameTest(template = "mts:empty_3x3")
    public void vehicleSpawnsWithCorrectInitialState(GameTestHelper helper) {
        // Spawn a vehicle entity
        BlockPos spawnPos = new BlockPos(1, 1, 1);

        // Create vehicle through your spawn mechanism
        // EntityVehicleF_Physics vehicle = ...

        helper.runAfterDelay(20, () -> {
            // Assert vehicle exists and has correct state
            // helper.assertEntityPresent(MTSEntities.VEHICLE.get(), spawnPos);
            helper.succeed();
        });
    }

    @GameTest(template = "mts:empty_3x3")
    public void fuelPumpDispensesFuel(GameTestHelper helper) {
        BlockPos pumpPos = new BlockPos(1, 1, 1);

        // Place fuel pump block
        // helper.setBlock(pumpPos, MTSBlocks.FUEL_PUMP.get());

        helper.runAfterDelay(40, () -> {
            // Verify fuel pump tile entity exists
            // Verify fuel dispensing logic
            helper.succeed();
        });
    }
}
```

---

## CI/CD Pipeline

### GitHub Actions Workflow

**.github/workflows/test.yml:**
```yaml
name: Tests

on:
  push:
    branches: [master, dev]
  pull_request:
    branches: [master]

jobs:
  unit-tests:
    name: Unit Tests
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          cache-read-only: ${{ github.ref != 'refs/heads/master' }}

      - name: Run mccore tests
        run: ./gradlew :mccore:test

      - name: Upload test results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results
          path: mccore/build/reports/tests/

  gametest:
    name: GameTest Server
    runs-on: ubuntu-latest
    needs: unit-tests

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Run GameTests
        run: ./gradlew :neoforge:runGameTestServer
        timeout-minutes: 10

  build:
    name: Build
    runs-on: ubuntu-latest
    needs: [unit-tests]

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Build
        run: ./gradlew build

      - name: Upload artifacts
        uses: actions/upload-artifact@v4
        with:
          name: mod-jar
          path: neoforge/build/libs/*.jar
```

### Code Coverage (Optional)

**Add JaCoCo to mccore/build.gradle:**
```kotlin
plugins {
    id("jacoco")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
```

---

## What NOT to Test

| Category | Reason |
|----------|--------|
| Rendering/Shaders | GPU-dependent, visual verification needed |
| FMOD Audio | External native library |
| "Game Feel" | Subjective, requires playtesting |
| JEI Integration UI | Layout positioning is visual |
| Mixin Behavior | Tested through GameTest integration |
| Third-party Library Internals | Not our code |

---

## Roadmap

### Phase 1: Foundation (Week 1)
- [ ] Add test dependencies to `mccore/build.gradle`
- [ ] Create test directory structure
- [ ] Write `Point3DTest` (10+ tests)
- [ ] Write `RotationMatrixTest` (8+ tests)
- [ ] Set up GitHub Actions for unit tests

### Phase 2: Core Testing (Week 2)
- [ ] Write `JSONParserTest` (12+ tests)
- [ ] Write `ColorRGBTest` (6+ tests)
- [ ] Write `BezierCurveTest` (8+ tests)
- [ ] Write `BoundingBoxTest` (8+ tests)
- [ ] Add code coverage reporting

### Phase 3: Integration (Week 3)
- [ ] Write packet serialization tests (20+ tests)
- [ ] Write `ConfigSystemTest` (6+ tests)
- [ ] Write JSON validation tests (10+ tests)
- [ ] Set up GameTest framework in neoforge

### Phase 4: GameTest (Week 4)
- [ ] Create test world templates
- [ ] Write vehicle spawn tests
- [ ] Write fuel pump tests
- [ ] Write crafting bench tests
- [ ] Add GameTest to CI pipeline

### Phase 5: Maintenance (Ongoing)
- [ ] Add tests for new features
- [ ] Maintain >80% coverage on critical paths
- [ ] Review and update tests quarterly

---

## Running Tests

```bash
# Run all unit tests
./gradlew :mccore:test

# Run specific test class
./gradlew :mccore:test --tests "minecrafttransportsimulator.baseclasses.Point3DTest"

# Run with coverage report
./gradlew :mccore:test :mccore:jacocoTestReport

# Run GameTests (headless)
./gradlew :neoforge:runGameTestServer

# Run all checks
./gradlew check
```

---

## Test Naming Conventions

```
MethodName_StateUnderTest_ExpectedBehavior

Examples:
- add_twoPositivePoints_returnsSum()
- parse_malformedJSON_throwsException()
- serialize_emptyPacket_writesZeroLength()
```

---

## Resources

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Minecraft GameTest Framework](https://docs.neoforged.net/docs/misc/gametest/)
- [Mockito Documentation](https://site.mockito.org/)

---

## Contributing Tests

When adding new features:

1. Write unit tests for any new math/utility classes
2. Write JSON validation tests for new definition types
3. Write packet tests for new network messages
4. Consider GameTest for new block/entity interactions
5. Update this document with new test categories

**Test coverage goals:**
- `baseclasses/`: 90%+
- `packloading/`: 80%+
- `packets/`: 85%+
- `systems/`: 70%+
