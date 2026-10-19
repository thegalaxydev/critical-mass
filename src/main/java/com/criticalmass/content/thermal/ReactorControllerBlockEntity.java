package com.criticalmass.content.thermal;

import com.criticalmass.client.VesselHighlighter;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ReactorControllerBlockEntity extends SmartBlockEntity implements ThermalNode, IHaveGoggleInformation {
    private final CoolantBuffer coolant = new CoolantBuffer(CMThermal.CONTROLLER_CAPACITY, this::markCoolantChanged);
    private final ReactorCore core = new ReactorCore();
    private VesselStructure structure;
    private boolean structureDirty = true;
    private int revalidateIn;
    private int signal;
    private boolean reacting;
    private boolean tripped;
    private boolean frozen;
    private final Map<Long, Double> integrity = new HashMap<>();
    private int damageClock;
    private double clientLowestIntegrity = 1.0;
    private int clientDamaged;
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
    private final List<String> missing = new ArrayList<>();
    private int fuelColumns;
    private int controlColumns;
    private int coolantColumns;
    private double clientOutput;
    private double clientExtracted;
    private double clientMaxTemp = CMThermal.AMBIENT;
    private int clientHotSpots;

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

    public List<String> missing() {
        return missing;
    }

    public double insertion() {
        return 1.0 - signal / 15.0;
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

    private void damageCasing() {
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
            if (!problemPos.isEmpty()) {
                VesselHighlighter.show(this);
            }
            return;
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
        boolean canRun = structure != null && structure.complete() && !isDamaged();
        if (!canRun && reacting) {
            tripped = true;
        }
        if (signal == 0) {
            tripped = false;
        }
        reacting = canRun && !tripped && signal > 0;
        effectiveFlow = Math.max(flowAccum, lastTickFlow);
        lastTickFlow = flowAccum;
        flowAccum = 0;
        heatAdded += core.tick(reacting, insertion(), effectiveFlow, coolant);
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
        if (isDamaged()) {
            return "criticalmass.reactor.status.damaged";
        }
        if (tripped) {
            return "criticalmass.reactor.status.tripped";
        }
        return reacting ? "criticalmass.reactor.status.running" : "criticalmass.reactor.status.shutdown";
    }

    public void revalidate() {
        structureDirty = false;
        revalidateIn = CMThermal.REVALIDATE_INTERVAL;
        structure = VesselStructure.scan(level, worldPosition);
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
        problemPos.clear();
        problemReason.clear();
        for (VesselStructure.Problem p : structure.problems) {
            problemPos.add(p.pos());
            problemReason.add(p.reason());
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
        tag.putInt("Damaged", damagedBlocks());
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
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        coolant.load(tag.getCompound("Coolant"), registries);
        heatAdded = tag.getDouble("HeatAdded");
        tripped = tag.getBoolean("Tripped");
        reacting = tag.getBoolean("Reacting");
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
        clientLowestIntegrity = tag.contains("LowestIntegrity") ? tag.getDouble("LowestIntegrity") : 1.0;
        clientDamaged = tag.getInt("Damaged");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("criticalmass.goggles.controller")));
        boolean running = status.endsWith("running");
        boolean healthy = running || status.endsWith("shutdown");
        tooltip.add(ThermalTooltips.line(status, running ? ChatFormatting.GREEN : healthy ? ChatFormatting.GRAY : ChatFormatting.RED));
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
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.insertion", ChatFormatting.GRAY, Math.round(insertion() * 100)));
        if (fuelColumns > 0) {
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.reaction", clientOutput > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY, ThermalTooltips.number(clientOutput)));
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.extracted", ChatFormatting.GRAY, ThermalTooltips.number(clientExtracted)));
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.core_temp", clientMaxTemp >= CMThermal.HOT_SPOT_TEMPERATURE ? ChatFormatting.RED : ChatFormatting.GOLD, ThermalTooltips.number(clientMaxTemp)));
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
}
