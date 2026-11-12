package com.criticalmass.content.thermal;

import com.criticalmass.client.VesselHighlighter;
import com.criticalmass.content.control.PlantDisplaySources;
import com.criticalmass.content.fuel.FuelAssemblyItem;
import com.criticalmass.content.fuel.FuelState;
import com.criticalmass.content.radiation.ContaminationData;
import com.criticalmass.content.kinetics.ControlRodDriveBlockEntity;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.companion.SableCompanion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.Block;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ReactorControllerBlockEntity extends SmartBlockEntity implements ThermalNode, IHaveGoggleInformation, Clearable {
    private final CoolantBuffer coolant = new CoolantBuffer(CMThermal.CONTROLLER_CAPACITY, this::markCoolantChanged);
    private final ReactorCore core = new ReactorCore();
    private VesselStructure structure;
    private VesselStructure lastValid;
    private boolean structureDirty = true;
    private int revalidateIn;
    private int signal;
    private boolean reacting;
    private boolean tripped;
    private boolean frozen;
    private boolean startingUp;
    private int noFlowTicks;
    private double manualInsertion = 1.0;
    private double boron;
    private double clientNeutron;
    private double clientReactivity;
    private double clientStartupRate;
    private double clientXenon;
    private String tripReason = "";
    private final ThermalSinks sinks = new ThermalSinks();
    private final Map<Long, Double> integrity = new HashMap<>();
    private int damageClock;
    private double clientLowestIntegrity = 1.0;
    private int clientDamaged;
    private double clientResidual;
    private double clientFission;
    private int clientDrives;
    private double clientDriveInsertion = 1;
    private int clientAssemblies;
    private int clientActive;
    private double clientBurnup;
    private double clientCondition = 1;
    private double heatAdded;
    private int flowAccum;
    private int lastTickFlow;
    private int effectiveFlow;
    private boolean syncPending;
    private int syncCooldown;
    private int liveSync;

    private String status = "criticalmass.reactor.status.unchecked";
    private String sizeError;
    private int[] dims = new int[3];
    private final List<BlockPos> problemPos = new ArrayList<>();
    private final List<String> problemReason = new ArrayList<>();
    private final List<BlockPos> warningPos = new ArrayList<>();
    private int comparator;
    private final List<String> warningReason = new ArrayList<>();
    private final List<String> missing = new ArrayList<>();
    private int fuelColumns;
    private int controlColumns;
    private int coolantColumns;
    private double clientOutput;
    private double clientExtracted;
    private double clientMaxTemp = CMThermal.AMBIENT;
    private int clientHotSpots;

    private final double[] bankTargets = {Double.NaN, Double.NaN, Double.NaN, Double.NaN};
    private String startBlock = "";
    private int coriumFloor = Integer.MIN_VALUE;
    private double primaryPressure = CMThermal.ATMOSPHERIC;
    private int pressureLink;
    private boolean flashing;
    private double flashedVolume;
    private int visualClock;

    public ReactorControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        LoadedThermalBlocks.add(this);
    }

    @Override
    public void invalidate() {
        LoadedThermalBlocks.remove(this);
        super.invalidate();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public CoolantBuffer coolant() {
        return coolant;
    }

    public ReactorCore core() {
        return core;
    }

    @Nullable
    public VesselStructure structure() {
        return structure;
    }

    @Override
    @Nullable
    public Direction passThroughExit(Direction enteredFrom) {
        return null;
    }

    public boolean isReacting() {
        return reacting;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public boolean isTripped() {
        return tripped;
    }

    public double heatAdded() {
        return heatAdded;
    }

    public String status() {
        return status;
    }

    public List<BlockPos> problemPositions() {
        return problemPos;
    }

    public List<String> problemReasons() {
        return problemReason;
    }

    public List<BlockPos> warningPositions() {
        return warningPos;
    }

    public List<String> warningReasons() {
        return warningReason;
    }

    public int columnHeight() {
        return Math.max(1, dims[2] - 2);
    }

    public int comparator() {
        return comparator;
    }

    public static int comparatorFor(double temperature) {
        double fraction = (Math.max(CMThermal.AMBIENT, temperature) - CMThermal.AMBIENT) / (CMThermal.TRIP_TEMPERATURE - CMThermal.AMBIENT);
        return (int) Math.round(15 * Math.max(0, Math.min(1, fraction)));
    }

    public List<String> missing() {
        return missing;
    }

    public double insertion() {
        return manualInsertion;
    }

    public double boron() {
        return boron;
    }

    public void setManualInsertion(double value) {
        manualInsertion = Math.max(0, Math.min(1, value));
    }

    public void setBoron(double ppm) {
        boron = Math.max(0, ppm);
        syncPending = true;
        setChanged();
    }

    public int effectiveFlow() {
        return effectiveFlow;
    }

    private long rel(BlockPos abs) {
        return abs.subtract(worldPosition).asLong();
    }

    public boolean owns(BlockPos pos) {
        return structure != null && !structure.unloaded() && structure.onShell(pos);
    }

    public double integrity(BlockPos abs) {
        return integrity.getOrDefault(rel(abs), 1.0);
    }

    public void setIntegrity(BlockPos abs, double value) {
        double v = Math.max(0, Math.min(1, value));
        long k = rel(abs);
        if (v >= 1) {
            integrity.remove(k);
        } else {
            integrity.put(k, v);
        }
        BlockState s = level.getBlockState(abs);
        if (s.getBlock() instanceof VesselCasingBlock) {
            int stage = VesselCasingBlock.stageFor(v);
            if (s.getValue(VesselCasingBlock.CRACK) != stage) {
                level.setBlock(abs, s.setValue(VesselCasingBlock.CRACK, stage), Block.UPDATE_ALL);
            }
        }
        setChanged();
        syncPending = true;
    }

    public boolean repair(BlockPos abs, double amount) {
        if (!(level.getBlockState(abs).getBlock() instanceof VesselCasingBlock)) {
            return false;
        }
        double current = integrity(abs);
        if (current >= 1) {
            return false;
        }
        setIntegrity(abs, current + amount);
        return true;
    }

    public double lowestIntegrity() {
        double low = 1.0;
        for (double v : integrity.values()) {
            low = Math.min(low, v);
        }
        return low;
    }

    public int damagedBlocks() {
        return integrity.size();
    }

    public boolean isDamaged() {
        return lowestIntegrity() < CMThermal.DAMAGED_THRESHOLD;
    }

    private void ruinColumn(long k, @Nullable VesselStructure shape) {
        if (shape == null || level == null) {
            return;
        }
        int x = worldPosition.getX() + ReactorCore.keyX(k);
        int z = worldPosition.getZ() + ReactorCore.keyZ(k);
        for (int y = shape.min.getY() + 1; y < shape.max.getY(); y++) {
            BlockPos p = new BlockPos(x, y, z);
            if (level.getBlockState(p).getBlock() instanceof ChannelBlocks.Fuel) {
                level.setBlock(p, CMBlocks.RUINED_CHANNEL.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        coriumFloor = shape.min.getY();
        level.playSound(null, new BlockPos(x, shape.min.getY() + 1, z), net.minecraft.sounds.SoundEvents.LAVA_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 0.5f);
        if (level instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, x + 0.5, shape.max.getY() + 1.2, z + 0.5, 20, 0.5, 0.3, 0.5, 0.02);
        }
        setChanged();
    }

    private void attackFloor() {
        if (core.coriumMass() == 0 || level == null || coriumFloor == Integer.MIN_VALUE) {
            return;
        }
        double t = core.coriumTemperature();
        if (t < CMThermal.CORIUM_ATTACK_TEMPERATURE) {
            return;
        }
        double loss = CMThermal.CORIUM_ATTACK_PER_SECOND * t / CMThermal.MELT_TEMPERATURE;
        for (long k : new ArrayList<>(core.coriumColumns())) {
            BlockPos floor = new BlockPos(worldPosition.getX() + ReactorCore.keyX(k), coriumFloor, worldPosition.getZ() + ReactorCore.keyZ(k));
            if (!(level.getBlockState(floor).getBlock() instanceof VesselCasingBlock)) {
                continue;
            }
            double left = integrity(floor) - loss;
            setIntegrity(floor, left);
            if (left <= 0) {
                releaseCorium(floor);
                return;
            }
        }
    }

    private void releaseCorium(BlockPos floor) {
        double[] c = core.takeCorium();
        level.setBlock(floor, CMBlocks.CORIUM.get().defaultBlockState().setValue(Corium.CoriumBlock.HOT, true), Block.UPDATE_ALL);
        if (level.getBlockEntity(floor) instanceof Corium.Entity_ lump) {
            lump.fill(c[0], c[1], (int) c[2], 0);
        }
        if (level instanceof ServerLevel server) {
            ContaminationData.get(server).add(floor, CMThermal.CORIUM_SPILL * c[2] * CMConfig.get(CMConfig.CORIUM_CONTAMINATION));
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5, 30, 0.4, 0.2, 0.4, 0.1);
        }
        level.playSound(null, floor, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 0.5f);
        coriumFloor = Integer.MIN_VALUE;
        setChanged();
    }

    private void damageCasing() {
        attackFloor();
        if (structure == null || !structure.valid()) {
            return;
        }
        BlockPos min = structure.min;
        BlockPos max = structure.max;
        Map<BlockPos, Double> loss = new HashMap<>();
        for (ReactorCore.FuelColumn f : core.columns()) {
            double t = f.temperature();
            if (t <= CMThermal.HOT_SPOT_TEMPERATURE) {
                continue;
            }
            double amount = CMThermal.CASING_DAMAGE_PER_SECOND * (t - CMThermal.HOT_SPOT_TEMPERATURE) / CMThermal.HOT_SPOT_TEMPERATURE;
            int x = worldPosition.getX() + f.dx();
            int z = worldPosition.getZ() + f.dz();
            Set<BlockPos> touched = new HashSet<>();
            touched.add(new BlockPos(x, min.getY(), z));
            touched.add(new BlockPos(x, max.getY(), z));
            for (int y = min.getY() + 1; y < max.getY(); y++) {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos n = new BlockPos(x, y, z).relative(d);
                    if (structure.onShell(n)) {
                        touched.add(n);
                    }
                }
            }
            for (BlockPos p : touched) {
                if (level.getBlockState(p).getBlock() instanceof VesselCasingBlock) {
                    loss.merge(p, amount, Double::sum);
                }
            }
        }
        for (Map.Entry<BlockPos, Double> e : loss.entrySet()) {
            setIntegrity(e.getKey(), integrity(e.getKey()) - e.getValue());
        }
    }

    private void trip(String reason) {
        tripped = true;
        tripReason = reason;
        syncPending = true;
    }

    public void scram() {
        trip("manual");
    }

    public String tripReason() {
        return tripReason;
    }

    @Override
    public ThermalSinks sinks() {
        return sinks;
    }

    public boolean isBreached() {
        for (double v : integrity.values()) {
            if (v <= 0) {
                return true;
            }
        }
        return false;
    }

    public String startBlock() {
        return startBlock;
    }

    public void notePressure(double bar) {
        primaryPressure = bar;
        pressureLink = CMThermal.PRESSURE_LINK_TICKS;
    }

    public double primaryPressure() {
        return primaryPressure;
    }

    public double boilingPoint() {
        return PressurizerBlockEntity.boilingPoint(primaryPressure);
    }

    public boolean flashing() {
        return flashing;
    }

    public double flashedVolume() {
        return flashedVolume;
    }

    private void flashCoolant() {
        if (pressureLink > 0) {
            pressureLink--;
        } else {
            primaryPressure = CMThermal.ATMOSPHERIC;
        }
        boolean now = false;
        if (coolant.amount() > 0) {
            double limit = boilingPoint();
            double over = coolant.temperature() - limit;
            if (over > 0) {
                double excess = over * coolant.amount() * CMThermal.HEAT_CAPACITY;
                int mb = (int) Math.min(coolant.amount(), Math.max(1, Math.ceil(excess * CMThermal.FLASH_SHARE / CMThermal.LATENT_HEAT)));
                CoolantBuffer.Packet steam = coolant.take(mb);
                sinks.vent(steam);
                sinks.ventedHeat += coolant.removeHeat(Math.min(excess * CMThermal.FLASH_SHARE, mb * CMThermal.LATENT_HEAT));
                flashedVolume += mb;
                now = true;
                if (level instanceof ServerLevel server && level.getGameTime() % 10 == 0) {
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, 6, 0.4, 0.2, 0.4, 0.05);
                }
            }
        }
        if (now != flashing) {
            flashing = now;
            syncPending = true;
        }
    }

    public double bankTarget(int bank) {
        return bankTargets[bank];
    }

    public void setBankTarget(int bank, double insertion) {
        bankTargets[bank] = Double.isNaN(insertion) ? Double.NaN : Math.max(0, Math.min(1, insertion));
        setChanged();
    }

    public void jogBank(int bank, double delta) {
        double from = bankTargets[bank];
        if (Double.isNaN(from)) {
            double now = bankInsertion(bank);
            from = Double.isNaN(now) ? 1.0 : now;
        }
        setBankTarget(bank, Math.round((from + delta) / CMThermal.ROD_JOG) * CMThermal.ROD_JOG);
    }

    public boolean requestReset() {
        for (int i = 0; i < bankTargets.length; i++) {
            bankTargets[i] = 1.0;
        }
        setChanged();
        if (!tripped) {
            return true;
        }
        if (signal != 0) {
            return false;
        }
        boolean canRun = structure != null && structure.complete() && !isDamaged() && !isBreached();
        if (!canRun) {
            return false;
        }
        tripped = false;
        tripReason = "";
        syncPending = true;
        return true;
    }

    public double bankInsertion(int bank) {
        double sum = 0;
        int n = 0;
        for (ControlRodDriveBlockEntity d : findDrives().values()) {
            if (d.bank() == bank) {
                sum += d.insertion();
                n++;
            }
        }
        return n == 0 ? Double.NaN : sum / n;
    }

    private void updateChannelVisuals(@Nullable VesselStructure shape, Map<Long, ControlRodDriveBlockEntity> drives, double fallback) {
        if (shape == null || level == null) {
            return;
        }
        int top = shape.max.getY() - 1;
        int height = shape.interiorHeight;
        for (long k : core.controlColumns()) {
            ControlRodDriveBlockEntity d = drives.get(k);
            double insertion = d != null ? d.insertion() : fallback;
            for (int i = 0; i < height; i++) {
                BlockPos p = new BlockPos(worldPosition.getX() + ReactorCore.keyX(k), top - i, worldPosition.getZ() + ReactorCore.keyZ(k));
                BlockState s = level.getBlockState(p);
                int rod = (int) Math.round(ChannelBlocks.ROD_LEVELS * Math.max(0, Math.min(1, insertion * height - i)));
                if (s.getBlock() instanceof ChannelBlocks.Control && s.getValue(ChannelBlocks.Control.ROD) != rod) {
                    level.setBlock(p, s.setValue(ChannelBlocks.Control.ROD, rod), Block.UPDATE_CLIENTS);
                }
            }
        }
        for (ReactorCore.FuelColumn f : core.columns()) {
            int glow = core.glow(f);
            for (int i = 0; i < height; i++) {
                BlockPos p = new BlockPos(worldPosition.getX() + f.dx(), top - i, worldPosition.getZ() + f.dz());
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof ChannelBlocks.Fuel && s.getValue(ChannelBlocks.Fuel.GLOW) != glow) {
                    level.setBlock(p, s.setValue(ChannelBlocks.Fuel.GLOW, glow), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    public Map<Long, ControlRodDriveBlockEntity> findDrives() {
        Map<Long, ControlRodDriveBlockEntity> out = new HashMap<>();
        VesselStructure layout = structure != null && structure.valid() ? structure : lastValid;
        if (layout == null || level == null) {
            return out;
        }
        for (long k : core.controlColumns()) {
            BlockPos p = new BlockPos(worldPosition.getX() + ReactorCore.keyX(k), layout.max.getY() + 1, worldPosition.getZ() + ReactorCore.keyZ(k));
            if (level.getBlockEntity(p) instanceof ControlRodDriveBlockEntity d) {
                out.put(k, d);
            }
        }
        return out;
    }

    public boolean repairAllowed() {
        return !isBreached() || core.residualPower() <= CMThermal.REPAIR_RESIDUAL_LIMIT;
    }

    @Nullable
    public String refuelBlocker() {
        if (frozen) {
            return "criticalmass.service.frozen";
        }
        if (reacting || startingUp) {
            return "criticalmass.service.running";
        }
        if (structure == null || !structure.valid()) {
            return "criticalmass.service.invalid";
        }
        if (core.freeColumn() == null) {
            return "criticalmass.service.no_channel";
        }
        return null;
    }

    public boolean canRefuel(FuelState state) {
        return state != null && !state.damaged() && refuelBlocker() == null;
    }

    public boolean insertFuel(FuelState state) {
        if (!canRefuel(state)) {
            return false;
        }
        Long k = core.freeColumn();
        if (k == null || !core.insert(k, state)) {
            return false;
        }
        syncPending = true;
        setChanged();
        return true;
    }

    public boolean defuelAllowed() {
        return !frozen && !reacting && !startingUp;
    }

    @Nullable
    public Long nextDischarge(boolean everything) {
        if (!defuelAllowed()) {
            return null;
        }
        for (Map.Entry<Long, FuelState> e : core.assemblies().entrySet()) {
            if (everything || !e.getValue().active() || !core.hasColumn(e.getKey())) {
                return e.getKey();
            }
        }
        return null;
    }

    @Nullable
    public FuelState discharge(long key) {
        if (!defuelAllowed()) {
            return null;
        }
        FuelState s = core.remove(key);
        if (s != null) {
            syncPending = true;
            setChanged();
        }
        return s;
    }

    public void loadFuel(int dx, int dz, FuelState state) {
        core.setAssembly(ReactorCore.key(dx, dz), state);
        syncPending = true;
        setChanged();
    }

    @Override
    public void destroy() {
        super.destroy();
        if (level == null || level.isClientSide) {
            return;
        }
        if (core.coriumMass() > 0) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, new ItemStack(CMBlocks.CORIUM.get(), core.coriumMass()));
            core.takeCorium();
        }
        for (long k : new ArrayList<>(core.assemblies().keySet())) {
            FuelState s = core.remove(k);
            if (s != null) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, FuelAssemblyItem.stackFor(s));
            }
        }
    }

    @Nullable
    public Vec3 coreCenter() {
        VesselStructure layout = structure != null && structure.valid() ? structure : lastValid;
        if (layout == null) {
            return null;
        }
        return new Vec3((layout.min.getX() + layout.max.getX() + 1) / 2.0, (layout.min.getY() + layout.max.getY() + 1) / 2.0, (layout.min.getZ() + layout.max.getZ() + 1) / 2.0);
    }

    @Nullable
    public AABB coreInterior() {
        VesselStructure layout = structure != null && structure.valid() ? structure : lastValid;
        if (layout == null) {
            return null;
        }
        return new AABB(layout.min.getX() + 1, layout.min.getY() + 1, layout.min.getZ() + 1, layout.max.getX(), layout.max.getY(), layout.max.getZ());
    }

    private void spill(CoolantBuffer.Packet leak) {
        if (!(level instanceof ServerLevel server) || leak.isEmpty() || structure == null) {
            return;
        }
        List<BlockPos> holes = new ArrayList<>();
        for (Map.Entry<Long, Double> e : integrity.entrySet()) {
            if (e.getValue() <= 0) {
                holes.add(worldPosition.offset(BlockPos.of(e.getKey())));
            }
        }
        if (holes.isEmpty()) {
            return;
        }
        double amount = leak.amount() * (CMThermal.isContaminated(leak.fluid()) ? CMThermal.CONTAMINATED_SPILL : CMThermal.CLEAN_SPILL) / holes.size();
        ContaminationData data = ContaminationData.get(server);
        for (BlockPos hole : holes) {
            List<BlockPos> outside = new ArrayList<>();
            for (Direction d : Direction.values()) {
                BlockPos n = hole.relative(d);
                if (!structure.contains(n, 0) && !level.getBlockState(n).isSolidRender(level, n)) {
                    outside.add(n);
                }
            }
            if (outside.isEmpty()) {
                outside.add(hole);
            }
            for (BlockPos n : outside) {
                data.add(BlockPos.containing(SableCompanion.INSTANCE.projectOutOfSubLevel(level, n.getCenter())), amount / outside.size());
            }
        }
    }

    public void noteFlow(int amount) {
        flowAccum += amount;
    }

    public void markStructureDirty() {
        structureDirty = true;
    }

    public boolean watches(BlockPos pos) {
        return structure == null || structure.contains(pos, 1) || pos.distManhattan(worldPosition) <= 2;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            if (!problemPos.isEmpty() || !warningPos.isEmpty()) {
                VesselHighlighter.show(this);
            }
            return;
        }
        int nowComparator = comparatorFor(core.maxTemperature());
        if (nowComparator != comparator) {
            comparator = nowComparator;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        if (structure != null && !structure.unloaded() && !level.hasChunksAt(structure.min, structure.max)) {
            frozen = true;
            return;
        }
        if (structureDirty || revalidateIn-- <= 0) {
            revalidate();
        }
        frozen = structure.unloaded();
        if (frozen) {
            if (!status.equals("criticalmass.reactor.status.frozen")) {
                status = "criticalmass.reactor.status.frozen";
                sendData();
            }
            return;
        }
        int newSignal = level.getBestNeighborSignal(worldPosition);
        if (newSignal != signal) {
            signal = newSignal;
            syncPending = true;
        }
        if (++damageClock >= 20) {
            damageClock = 0;
            damageCasing();
        }
        effectiveFlow = Math.max(flowAccum, lastTickFlow);
        lastTickFlow = flowAccum;
        flowAccum = 0;
        Map<Long, ControlRodDriveBlockEntity> drives = findDrives();
        boolean breached = isBreached();
        boolean damaged = isDamaged();
        boolean canRun = structure != null && structure.complete() && !damaged && !breached;
        boolean demand = signal > 0;
        for (ControlRodDriveBlockEntity d : drives.values()) {
            if (d.requested() < 1 || d.insertion() < 1) {
                demand = true;
            }
        }
        int failures = core.takeFailures();
        if (reacting) {
            if (failures > 0) {
                trip("fuel_failure");
            } else if (!canRun) {
                trip(breached ? "breach" : damaged ? "damaged" : "invalid");
            } else if (core.maxTemperature() >= CMThermal.TRIP_TEMPERATURE) {
                trip("overtemp");
            } else if (core.neutronPower() >= CMThermal.HIGH_FLUX_TRIP) {
                trip("high_flux");
            } else if (core.startupRate() > 1 / CMThermal.PERIOD_TRIP_SECONDS) {
                trip("short_period");
            } else if (effectiveFlow == 0) {
                if (++noFlowTicks >= CMThermal.LOW_FLOW_TRIP_TICKS) {
                    trip("low_flow");
                }
            } else {
                noFlowTicks = 0;
            }
        } else {
            noFlowTicks = 0;
        }
        if (tripped && signal == 0) {
            boolean rodsCommandedIn = true;
            for (ControlRodDriveBlockEntity d : drives.values()) {
                if (d.requested() < 1) {
                    rodsCommandedIn = false;
                }
            }
            if (rodsCommandedIn) {
                tripped = false;
            }
        }
        if (tripped || !canRun) {
            for (ControlRodDriveBlockEntity d : drives.values()) {
                d.reactorTripped();
            }
        }
        boolean permitted = reacting || (effectiveFlow > 0 && coolant.amount() >= coolant.capacity() * CMThermal.START_COOLANT_SHARE);
        String block = !demand || !canRun || tripped || permitted ? "" : effectiveFlow == 0 ? "flow" : "coolant";
        if (!block.equals(startBlock)) {
            startBlock = block;
            syncPending = true;
        }
        if (!permitted) {
            for (ControlRodDriveBlockEntity d : drives.values()) {
                d.blockWithdrawal();
            }
        }
        reacting = canRun && !tripped && demand && permitted;
        boolean dropped = tripped || !canRun;
        double wanted = dropped ? 1.0 : 1.0 - signal / 15.0;
        if (!permitted && wanted < manualInsertion) {
            wanted = manualInsertion;
        }
        if (wanted > manualInsertion) {
            manualInsertion = Math.min(wanted, manualInsertion + (dropped ? 1.0 / CMThermal.ROD_FAILSAFE_TICKS : CMThermal.ROD_MANUAL_RATE));
        } else {
            manualInsertion = Math.max(wanted, manualInsertion - CMThermal.ROD_MANUAL_RATE);
        }
        double fallback = manualInsertion;
        VesselStructure shape = structure != null && structure.valid() ? structure : lastValid;
        for (ControlRodDriveBlockEntity d : drives.values()) {
            d.linkToReactor(shape == null ? 0 : shape.interiorHeight, bankTargets[d.bank()]);
        }
        if (visualClock-- <= 0) {
            visualClock = CMThermal.CHANNEL_VISUAL_TICKS;
            updateChannelVisuals(shape, drives, fallback);
        }
        heatAdded += core.tick(k -> {
            ControlRodDriveBlockEntity d = drives.get(k);
            return d != null ? d.insertion() : fallback;
        }, effectiveFlow, coolant, boron);
        if (failures > 0) {
            coolant.contaminate();
        }
        for (long k : core.takeMelts()) {
            ruinColumn(k, shape);
        }
        flashCoolant();
        if (breached && coolant.amount() > 0) {
            CoolantBuffer.Packet leak = coolant.take(CMThermal.BREACH_LEAK_RATE);
            sinks.vent(leak);
            spill(leak);
        }
        startingUp = false;
        for (ControlRodDriveBlockEntity d : drives.values()) {
            if (d.moving() && d.target() < d.insertion()) {
                startingUp = true;
            }
        }
        String newStatus = statusKey(canRun);
        if (!newStatus.equals(status)) {
            status = newStatus;
            syncPending = true;
        }
        if (liveSync-- <= 0) {
            liveSync = 20;
            syncPending = true;
        }
        if (syncCooldown > 0) {
            syncCooldown--;
        } else if (syncPending) {
            syncPending = false;
            syncCooldown = 10;
            setChanged();
            sendData();
        }
    }

    private String statusKey(boolean canRun) {
        if (structure == null) {
            return "criticalmass.reactor.status.unchecked";
        }
        if (!structure.valid()) {
            return "criticalmass.reactor.status.invalid";
        }
        if (!structure.complete()) {
            return "criticalmass.reactor.status.incomplete";
        }
        if (isBreached()) {
            return "criticalmass.reactor.status.breached";
        }
        if (isDamaged()) {
            return "criticalmass.reactor.status.damaged";
        }
        if (tripped) {
            return "criticalmass.reactor.status.tripped";
        }
        if (!startBlock.isEmpty()) {
            return "criticalmass.reactor.status.blocked_" + startBlock;
        }
        if (core.activeAssemblies() == 0 && core.residualPower() <= CMThermal.RESIDUAL_COLD_LIMIT && core.maxTemperature() <= CMThermal.COLD_TEMPERATURE) {
            return "criticalmass.reactor.status.unfueled";
        }
        if (reacting && startingUp) {
            return "criticalmass.reactor.status.starting";
        }
        if (reacting && core.lastFission() > 0) {
            return "criticalmass.reactor.status.running";
        }
        if (reacting) {
            return "criticalmass.reactor.status.source_range";
        }
        if (core.residualPower() > CMThermal.RESIDUAL_COLD_LIMIT || core.maxTemperature() > CMThermal.COLD_TEMPERATURE) {
            return "criticalmass.reactor.status.cooling_down";
        }
        return "criticalmass.reactor.status.cold";
    }

    public void revalidate() {
        structureDirty = false;
        revalidateIn = CMThermal.REVALIDATE_INTERVAL;
        structure = VesselStructure.scan(level, worldPosition);
        if (structure.valid()) {
            lastValid = structure;
        }
        core.rebuild(structure, worldPosition);
        if (structure.unloaded()) {
            revalidateIn = 20;
            return;
        }
        for (Map.Entry<BlockPos, Integer> c : structure.cracked.entrySet()) {
            integrity.putIfAbsent(rel(c.getKey()), VesselCasingBlock.integrityFor(c.getValue()));
        }
        integrity.keySet().removeIf(k -> {
            BlockPos abs = worldPosition.offset(BlockPos.of(k));
            return !structure.onShell(abs) || !(level.getBlockState(abs).getBlock() instanceof VesselCasingBlock);
        });
        for (Map.Entry<Long, Double> e : integrity.entrySet()) {
            BlockPos abs = worldPosition.offset(BlockPos.of(e.getKey()));
            BlockState s = level.getBlockState(abs);
            int stage = VesselCasingBlock.stageFor(e.getValue());
            if (s.getBlock() instanceof VesselCasingBlock && s.getValue(VesselCasingBlock.CRACK) != stage) {
                level.setBlock(abs, s.setValue(VesselCasingBlock.CRACK, stage), Block.UPDATE_ALL);
            }
        }
        problemPos.clear();
        problemReason.clear();
        for (VesselStructure.Problem p : structure.problems) {
            problemPos.add(p.pos());
            problemReason.add(p.reason());
        }
        warningPos.clear();
        warningReason.clear();
        for (VesselStructure.Problem p : structure.warnings) {
            warningPos.add(p.pos());
            warningReason.add(p.reason());
        }
        missing.clear();
        missing.addAll(structure.missing);
        sizeError = structure.sizeError;
        dims = new int[]{structure.width(), structure.length(), structure.height()};
        fuelColumns = (int) structure.count(VesselStructure.Kind.FUEL);
        controlColumns = (int) structure.count(VesselStructure.Kind.CONTROL);
        coolantColumns = (int) structure.count(VesselStructure.Kind.COOLANT);
        syncPending = true;
    }

    private void markCoolantChanged() {
        syncPending = true;
        setChanged();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Coolant", coolant.save(registries));
        tag.put("Core", core.save());
        tag.putDouble("HeatAdded", heatAdded);
        tag.putBoolean("Tripped", tripped);
        tag.putBoolean("Reacting", reacting);
        tag.putString("TripReason", tripReason);
        tag.putDouble("PrimaryPressure", primaryPressure);
        tag.putBoolean("Flashing", flashing);
        tag.putString("StartBlock", startBlock);
        if (coriumFloor != Integer.MIN_VALUE) {
            tag.putInt("CoriumFloor", coriumFloor);
        }
        tag.putDouble("Flashed", flashedVolume);
        for (int i = 0; i < bankTargets.length; i++) {
            if (!Double.isNaN(bankTargets[i])) {
                tag.putDouble("Bank" + i, bankTargets[i]);
            }
        }
        tag.put("Sinks", sinks.save());
        ListTag damage = new ListTag();
        for (Map.Entry<Long, Double> e : integrity.entrySet()) {
            CompoundTag d = new CompoundTag();
            BlockPos r = BlockPos.of(e.getKey());
            d.putIntArray("P", new int[]{r.getX(), r.getY(), r.getZ()});
            d.putDouble("V", e.getValue());
            damage.add(d);
        }
        tag.put("Integrity", damage);
        if (!clientPacket) {
            return;
        }
        tag.putDouble("LowestIntegrity", lowestIntegrity());
        tag.putDouble("Residual", core.residualPower());
        tag.putDouble("Fission", core.lastFission());
        Map<Long, ControlRodDriveBlockEntity> drives = findDrives();
        double sum = 0;
        for (ControlRodDriveBlockEntity d : drives.values()) {
            sum += d.insertion();
        }
        tag.putInt("Drives", drives.size());
        tag.putDouble("DriveInsertion", drives.isEmpty() ? 1 : sum / drives.size());
        tag.putInt("Damaged", damagedBlocks());
        double burn = 0;
        double cond = 1;
        for (FuelState a : core.assemblies().values()) {
            burn += 1 - a.remaining();
            cond = Math.min(cond, a.condition());
        }
        tag.putInt("Assemblies", core.assemblies().size());
        tag.putInt("ActiveFuel", core.activeAssemblies());
        tag.putDouble("Burnup", core.assemblies().isEmpty() ? 0 : burn / core.assemblies().size());
        tag.putDouble("FuelCondition", cond);
        tag.putString("Status", status);
        if (sizeError != null) {
            tag.putString("SizeError", sizeError);
        }
        tag.putIntArray("Dims", dims);
        ListTag problems = new ListTag();
        for (int i = 0; i < problemPos.size(); i++) {
            CompoundTag p = new CompoundTag();
            BlockPos rel = problemPos.get(i).subtract(worldPosition);
            p.putIntArray("P", new int[]{rel.getX(), rel.getY(), rel.getZ()});
            p.putString("R", problemReason.get(i));
            problems.add(p);
        }
        tag.put("Problems", problems);
        ListTag warnings = new ListTag();
        for (int i = 0; i < warningPos.size(); i++) {
            CompoundTag p = new CompoundTag();
            BlockPos rel = warningPos.get(i).subtract(worldPosition);
            p.putIntArray("P", new int[]{rel.getX(), rel.getY(), rel.getZ()});
            p.putString("R", warningReason.get(i));
            warnings.add(p);
        }
        tag.put("Warnings", warnings);
        ListTag miss = new ListTag();
        for (String m : missing) {
            miss.add(StringTag.valueOf(m));
        }
        tag.put("Missing", miss);
        tag.putIntArray("Columns", new int[]{fuelColumns, controlColumns, coolantColumns});
        tag.putInt("Signal", signal);
        tag.putDouble("Output", core.lastOutput());
        tag.putDouble("Extracted", core.lastExtracted());
        tag.putDouble("MaxTemp", core.maxTemperature());
        tag.putInt("HotSpots", core.hotSpots());
        tag.putDouble("Manual", manualInsertion);
        tag.putDouble("Boron", boron);
        tag.putDouble("NeutronPower", core.neutronPower());
        tag.putDouble("Reactivity", core.reactivity());
        tag.putDouble("StartupRate", core.startupRate());
        tag.putDouble("XenonLevel", core.xenon());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        coolant.load(tag.getCompound("Coolant"), registries);
        heatAdded = tag.getDouble("HeatAdded");
        tripped = tag.getBoolean("Tripped");
        reacting = tag.getBoolean("Reacting");
        tripReason = tag.getString("TripReason");
        primaryPressure = tag.contains("PrimaryPressure") ? tag.getDouble("PrimaryPressure") : CMThermal.ATMOSPHERIC;
        flashing = tag.getBoolean("Flashing");
        startBlock = tag.getString("StartBlock");
        manualInsertion = tag.contains("Manual") ? tag.getDouble("Manual") : 1.0;
        boron = tag.getDouble("Boron");
        coriumFloor = tag.contains("CoriumFloor") ? tag.getInt("CoriumFloor") : Integer.MIN_VALUE;
        flashedVolume = tag.getDouble("Flashed");
        for (int i = 0; i < bankTargets.length; i++) {
            bankTargets[i] = tag.contains("Bank" + i) ? tag.getDouble("Bank" + i) : Double.NaN;
        }
        sinks.load(tag.getCompound("Sinks"));
        if (!clientPacket) {
            integrity.clear();
            for (Tag t : tag.getList("Integrity", Tag.TAG_COMPOUND)) {
                CompoundTag d = (CompoundTag) t;
                int[] r = d.getIntArray("P");
                if (r.length == 3) {
                    integrity.put(new BlockPos(r[0], r[1], r[2]).asLong(), d.getDouble("V"));
                }
            }
            core.load(tag.getCompound("Core"));
            structureDirty = true;
            return;
        }
        status = tag.getString("Status");
        sizeError = tag.contains("SizeError") ? tag.getString("SizeError") : null;
        int[] d = tag.getIntArray("Dims");
        dims = d.length == 3 ? d : new int[3];
        problemPos.clear();
        problemReason.clear();
        for (Tag t : tag.getList("Problems", Tag.TAG_COMPOUND)) {
            CompoundTag p = (CompoundTag) t;
            int[] rel = p.getIntArray("P");
            if (rel.length == 3) {
                problemPos.add(worldPosition.offset(rel[0], rel[1], rel[2]));
                problemReason.add(p.getString("R"));
            }
        }
        warningPos.clear();
        warningReason.clear();
        for (Tag t : tag.getList("Warnings", Tag.TAG_COMPOUND)) {
            CompoundTag p = (CompoundTag) t;
            int[] rel = p.getIntArray("P");
            if (rel.length == 3) {
                warningPos.add(worldPosition.offset(rel[0], rel[1], rel[2]));
                warningReason.add(p.getString("R"));
            }
        }
        missing.clear();
        for (Tag t : tag.getList("Missing", Tag.TAG_STRING)) {
            missing.add(t.getAsString());
        }
        int[] c = tag.getIntArray("Columns");
        if (c.length == 3) {
            fuelColumns = c[0];
            controlColumns = c[1];
            coolantColumns = c[2];
        }
        signal = tag.getInt("Signal");
        clientOutput = tag.getDouble("Output");
        clientExtracted = tag.getDouble("Extracted");
        clientMaxTemp = tag.getDouble("MaxTemp");
        clientHotSpots = tag.getInt("HotSpots");
        clientNeutron = tag.getDouble("NeutronPower");
        clientReactivity = tag.getDouble("Reactivity");
        clientStartupRate = tag.getDouble("StartupRate");
        clientXenon = tag.getDouble("XenonLevel");
        clientLowestIntegrity = tag.contains("LowestIntegrity") ? tag.getDouble("LowestIntegrity") : 1.0;
        clientDamaged = tag.getInt("Damaged");
        clientResidual = tag.getDouble("Residual");
        clientFission = tag.getDouble("Fission");
        clientDrives = tag.getInt("Drives");
        clientDriveInsertion = tag.getDouble("DriveInsertion");
        clientAssemblies = tag.getInt("Assemblies");
        clientActive = tag.getInt("ActiveFuel");
        clientBurnup = tag.getDouble("Burnup");
        clientCondition = tag.contains("FuelCondition") ? tag.getDouble("FuelCondition") : 1;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("criticalmass.goggles.controller")));
        boolean running = status.endsWith("running") || status.endsWith("starting");
        boolean healthy = running || status.endsWith("cold") || status.endsWith("cooling_down") || status.endsWith("unfueled") || status.endsWith("source_range");
        tooltip.add(ThermalTooltips.line(status, running ? ChatFormatting.GREEN : healthy ? ChatFormatting.GRAY : ChatFormatting.RED));
        if (status.endsWith("tripped") && !tripReason.isEmpty()) {
            tooltip.add(ThermalTooltips.line("criticalmass.reactor.trip." + tripReason, ChatFormatting.RED));
        }
        if (sizeError != null) {
            tooltip.add(ThermalTooltips.line(sizeError, ChatFormatting.RED, dims[0], dims[1], dims[2]));
        } else if (dims[0] > 0) {
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.vessel", ChatFormatting.GRAY, dims[0], dims[1], dims[2], fuelColumns, controlColumns, coolantColumns));
        }
        if (!problemPos.isEmpty()) {
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.problems", ChatFormatting.RED, problemPos.size()));
            for (int i = 0; i < Math.min(3, problemPos.size()); i++) {
                BlockPos p = problemPos.get(i);
                tooltip.add(Component.literal("       " + p.getX() + " " + p.getY() + " " + p.getZ() + ": ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.translatable(problemReason.get(i)).withStyle(ChatFormatting.GOLD)));
            }
        }
        for (String m : missing) {
            tooltip.add(ThermalTooltips.line(m, ChatFormatting.GOLD));
        }
        if (!warningPos.isEmpty()) {
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.warnings", ChatFormatting.GOLD, warningPos.size()));
            for (int i = 0; i < Math.min(4, warningPos.size()); i++) {
                BlockPos p = warningPos.get(i);
                tooltip.add(Component.literal("       " + p.getX() + " " + p.getZ() + ": ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.translatable(warningReason.get(i)).withStyle(ChatFormatting.GOLD)));
            }
        }
        if (clientDrives > 0) {
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.drive_insertion", ChatFormatting.GRAY, clientDrives, Math.round(clientDriveInsertion * 100)));
        } else {
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.insertion", ChatFormatting.GRAY, Math.round(insertion() * 100)));
        }
        if (clientResidual > 0.05) {
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.residual", ChatFormatting.GOLD, ThermalTooltips.number(clientResidual)));
        }
        if (fuelColumns > 0) {
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.fuel_loaded", clientActive > 0 ? ChatFormatting.GRAY : ChatFormatting.GOLD, clientAssemblies, fuelColumns, clientActive));
            if (clientAssemblies > 0) {
                tooltip.add(ThermalTooltips.line("criticalmass.goggles.fuel_burnup", clientCondition < 0.5 ? ChatFormatting.RED : ChatFormatting.GRAY,
                        Math.round(clientBurnup * 100), Math.round(clientCondition * 100)));
            }
            String state = clientReactivity > CMThermal.CRITICAL_BAND ? "supercritical" : clientReactivity < -CMThermal.CRITICAL_BAND ? "subcritical" : "critical";
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.neutron." + state, clientReactivity > CMThermal.CRITICAL_BAND ? ChatFormatting.GOLD : ChatFormatting.GRAY,
                    PlantDisplaySources.number(clientNeutron * 100), Math.round(clientReactivity * 1.0e5)));
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.period", clientStartupRate > 1 / (2 * CMThermal.PERIOD_TRIP_SECONDS) ? ChatFormatting.RED : ChatFormatting.GRAY,
                    Math.abs(clientStartupRate) < 1e-3 ? "\u221e" : PlantDisplaySources.number(1 / clientStartupRate)));
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.poisons", ChatFormatting.GRAY, Math.round(clientXenon * 100), Math.round(boron)));
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.reaction", clientFission > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY, ThermalTooltips.number(clientFission)));
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.extracted", ChatFormatting.GRAY, ThermalTooltips.number(clientExtracted)));
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.core_temp", clientMaxTemp >= CMThermal.HOT_SPOT_TEMPERATURE ? ChatFormatting.RED : ChatFormatting.GOLD, ThermalTooltips.number(clientMaxTemp)));
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.comparator", ChatFormatting.DARK_GRAY, comparatorFor(clientMaxTemp)));
            if (clientHotSpots > 0) {
                tooltip.add(ThermalTooltips.line("criticalmass.goggles.hot_spots", ChatFormatting.RED, clientHotSpots));
            }
        }
        if (clientDamaged > 0) {
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.integrity", clientLowestIntegrity < CMThermal.DAMAGED_THRESHOLD ? ChatFormatting.RED : ChatFormatting.GOLD,
                    Math.round(clientLowestIntegrity * 100), clientDamaged));
        }
        ThermalTooltips.coolant(tooltip, coolant);
        return true;
    }

    @Override
    public void clearContent() {
        core.assemblies().clear();
    }
}
