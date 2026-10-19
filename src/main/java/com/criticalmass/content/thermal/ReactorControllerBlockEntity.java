package com.criticalmass.content.thermal;

import com.criticalmass.client.VesselHighlighter;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
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
        boolean canRun = structure != null && structure.complete();
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
        if (!canRun) {
            return "criticalmass.reactor.status.incomplete";
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
        if (!clientPacket) {
            return;
        }
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
        ThermalTooltips.coolant(tooltip, coolant);
        return true;
    }
}
