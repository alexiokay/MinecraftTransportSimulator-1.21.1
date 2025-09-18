# Minecraft Transport Simulator - Forge to NeoForge 1.21.1 Migration Strategy

## Project Overview

**Current State**: Minecraft Transport Simulator mod for Minecraft 1.20.1 using Forge
**Target**: Minecraft 1.21.1 using NeoForge
**Module**: `mcinterfaceneoforge1211`

## Migration Scope Analysis

### Project Statistics
- **Total Java Files**: 47 files
- **Total Lines of Code**: 8,010 lines
- **Files Requiring Changes**: 16 files (~34% of files)
- **API References to Update**: ~77 direct references
- **Estimated Lines to Modify**: 600-900 lines (~10-15% of codebase)

### Affected Systems Breakdown

| System | Files Affected | Complexity | Priority | Estimated Hours |
|--------|---------------|------------|----------|-----------------|
| **Event System** | 9 files | LOW | HIGH | 1-2 hours |
| **Registry Objects** | 7 files | MEDIUM | HIGH | 2-3 hours |
| **Capability System** | 5 files | HIGH | CRITICAL | 4-6 hours |
| **Energy/Fluid** | 3 files | HIGH | MEDIUM | 3-4 hours |
| **Network Packets** | 1 file | HIGH | LOW | 2-3 hours |

## Technical Requirements

### Build Infrastructure (✅ COMPLETED)
- [x] Gradle 8.10 (supports Java 21)
- [x] Java 21 (Temurin JDK)
- [x] NeoForge plugin (moddev 1.0.21)
- [x] NeoForge version: 21.1.77
- [x] JEI Integration: jei-1.21.1-neoforge:19.21.2.313
- [x] Package rename: mcinterface1201 → mcinterface1211

### API Migration Requirements

#### 1. Event System Migration
**OLD Forge:**
```java
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.level.ExplosionEvent;
```

**NEW NeoForge:**
```java
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
```

#### 2. Registry System Migration
**OLD Forge:**
```java
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

RegistryObject<BlockEntityType<T>> BLOCK_ENTITY;
```

**NEW NeoForge:**
```java
import net.minecraft.core.registries.Registries;
import java.util.function.Supplier;

Supplier<BlockEntityType<T>> BLOCK_ENTITY;
// or use Holder<T> for registry references
```

#### 3. Capability System → Data Attachments
**OLD Forge Capabilities:**
```java
@CapabilityInject(IEnergyStorage.class)
public static Capability<IEnergyStorage> ENERGY;

public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
    if (cap == ForgeCapabilities.ENERGY) {
        return energyHandler.cast();
    }
}
```

**NEW NeoForge Data Attachments:**
```java
// Define attachment type
public static final AttachmentType<EnergyData> ENERGY_DATA =
    AttachmentType.builder(() -> new EnergyData())
        .serialize(EnergyData.CODEC)
        .build();

// Use attachment
entity.getData(ENERGY_DATA);
entity.setData(ENERGY_DATA, energyData);
```

#### 4. Network Packet System
**OLD Forge:**
```java
public static SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
    new ResourceLocation(MODID, "main"),
    () -> PROTOCOL_VERSION,
    PROTOCOL_VERSION::equals,
    PROTOCOL_VERSION::equals
);
```

**NEW NeoForge:**
```java
// Use PlayPayloadHandler with CustomPacketPayload
public record MyPayload(String data) implements CustomPacketPayload {
    public static final Type<MyPayload> TYPE = new Type<>(new ResourceLocation(MODID, "my_payload"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

#### 5. Fluid/Energy Systems
- **Fluids**: NeoForge removed IFluidHandler - use vanilla fluid handling or custom implementation
- **Energy**: No standard energy API - options:
  - Create custom energy system
  - Use item-based energy storage
  - Implement compatibility layer

## Migration Strategy

### Phase 1: Event System and Annotations (Priority: HIGH)
**Effort**: 1-2 hours
**Actions**:
1. Update all event-related imports
2. Fix @SubscribeEvent annotations
3. Update EventBusSubscriber references
4. Test event registration

### Phase 2: Registry System (Priority: HIGH)
**Effort**: 2-3 hours
**Actions**:
1. Replace RegistryObject with Supplier/Holder
2. Update DeferredRegister usage
3. Fix registry references to use vanilla Registries
4. Verify entity/block/item registration

### Phase 3: Capability Removal (Priority: CRITICAL)
**Effort**: 4-6 hours
**Actions**:
1. Create compatibility wrappers for:
   - Energy storage
   - Fluid tanks
   - Inventory containers
2. Implement Data Attachments for persistent data
3. Update all getCapability() calls
4. Remove LazyOptional usage

### Phase 4: Fluid/Energy Systems (Priority: MEDIUM)
**Effort**: 3-4 hours
**Actions**:
1. Design energy storage solution:
   - Option A: Custom energy API
   - Option B: Item-based energy
2. Implement fluid handling:
   - Use vanilla fluid system where possible
   - Create custom fluid tank if needed
3. Update all energy/fluid interactions

### Phase 5: Network System (Priority: LOW)
**Effort**: 2-3 hours
**Actions**:
1. Migrate to CustomPacketPayload system
2. Update packet registration
3. Convert all packet classes
4. Test client-server communication

### Phase 6: Client Rendering (Priority: MEDIUM)
**Effort**: 2-3 hours
**Actions**:
1. Update render event handlers
2. Fix model loading if needed
3. Update shader registration
4. Test all rendering features

### Phase 7: Final Integration (Priority: HIGH)
**Effort**: 1-2 hours
**Actions**:
1. Fix mixin configuration
2. Update mod loading
3. Test all systems together
4. Performance optimization

## Implementation Approaches

### Option 1: Incremental Fix (Conservative)
- Fix compilation errors file by file
- Test each system as completed
- **Pros**: Safer, easier to debug
- **Cons**: Slower, may have temporary broken states
- **Time Estimate**: 20-25 hours

### Option 2: Stub and Replace (Aggressive)
- Comment out all broken code
- Get mod loading with minimal features
- Re-implement systems one by one
- **Pros**: Always have working build
- **Cons**: Features unavailable during migration
- **Time Estimate**: 15-20 hours

### Option 3: Compatibility Layer (Recommended)
- Create wrapper classes for Forge APIs
- Implement adapters for capabilities
- Minimal changes to core mod code
- **Pros**: Fewer changes, easier maintenance
- **Cons**: Additional abstraction layer
- **Time Estimate**: 12-15 hours

## Key Challenges

1. **Capability System Removal**: Biggest change, affects core functionality
2. **No Standard Energy API**: Need custom solution
3. **Network System Rewrite**: Complete paradigm shift
4. **Fluid Handling**: Limited vanilla support
5. **Mixin Compatibility**: May need adjustments

## Testing Strategy

### Unit Testing
1. Registry system - Verify all items/blocks/entities register
2. Event system - Confirm events fire correctly
3. Network packets - Test client-server sync
4. Energy/Fluid - Validate storage and transfer

### Integration Testing
1. World loading/saving
2. Multiplayer compatibility
3. JEI integration
4. Performance benchmarks

### Regression Testing
1. All vehicle functionality
2. GUI interactions
3. Crafting recipes
4. Configuration system

## Success Criteria

- [ ] Mod loads without crashes
- [ ] All items/blocks/entities available
- [ ] Vehicles spawn and function
- [ ] Energy/fluid systems operational
- [ ] Multiplayer compatible
- [ ] JEI integration working
- [ ] Performance comparable to 1.20.1 version

## Risk Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Data loss during capability migration | Medium | High | Create backup system, extensive testing |
| Network incompatibility | Low | High | Implement fallback protocol |
| Performance regression | Medium | Medium | Profile and optimize hot paths |
| Missing NeoForge APIs | Low | High | Create custom implementations |

## Timeline Estimate

**Conservative Estimate**: 20-25 hours
**Realistic Estimate**: 15-18 hours
**Optimistic Estimate**: 12-15 hours

## Next Steps

1. Begin with Phase 1 (Event System) - Quick wins
2. Implement compatibility wrappers for capabilities
3. Create stub implementations for complex systems
4. Iteratively replace stubs with full implementations
5. Comprehensive testing phase
6. Documentation update

## Resources

- [NeoForge Documentation](https://docs.neoforged.net/)
- [NeoForge GitHub](https://github.com/neoforged/NeoForge)
- [Migration Examples](https://github.com/neoforged/NeoForge/wiki/Migration-from-Forge)
- [Data Attachments Guide](https://docs.neoforged.net/docs/datastorage/attachments)

## Notes

- The majority of code (85-90%) will remain unchanged
- Focus on API boundary changes rather than core logic
- Consider maintaining both Forge and NeoForge versions if needed
- Document all non-obvious migrations for future reference

---

*Last Updated: September 18, 2025*
*Status: Build Infrastructure Complete, API Migration Pending*