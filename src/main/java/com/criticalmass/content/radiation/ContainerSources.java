package com.criticalmass.content.radiation;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.fuel.ServicePortBlockEntity;
import com.criticalmass.content.fuel.ShieldedCaskBlockEntity;
import com.criticalmass.content.fuel.SpentFuelRackBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.mixin.ArmBlockEntityAccessor;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public final class ContainerSources {
    public static final int KEPT_TICKS = 20;
    private static final int MOST_CHUNKS = 4096;
    private static final Map<ServerLevel, Long2ObjectOpenHashMap<Cell>> SEEN = new WeakHashMap<>();

    private record Held(Vec3 pos, AABB box, List<ItemStack> stacks, @Nullable IFluidHandler liquid) {
    }

    private record Cell(long time, List<Held> held) {
    }

    private ContainerSources() {
    }

    public static double liquidStrength(IFluidHandler handler) {
        double mb = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            if (CMThermal.isContaminated(handler.getFluidInTank(tank))) {
                mb += handler.getFluidInTank(tank).getAmount();
            }
        }
        return mb * Radiation.LIQUID_WASTE_DOSE / 1000.0;
    }

    public static void near(ServerLevel level, Vec3 receiver, double reach, List<Radiation.Source> out) {
        Long2ObjectOpenHashMap<Cell> cells = SEEN.computeIfAbsent(level, l -> new Long2ObjectOpenHashMap<>());
        if (cells.size() > MOST_CHUNKS) {
            cells.clear();
        }
        Set<Object> counted = Collections.newSetFromMap(new IdentityHashMap<>());
        around(level, cells, counted, receiver, reach, null, out);
        carried(level, counted, receiver, reach, null, out);
        BoundingBox3d box = new BoundingBox3d(receiver.x - reach, receiver.y - reach, receiver.z - reach, receiver.x + reach, receiver.y + reach, receiver.z + reach);
        for (SubLevelAccess body : SableCompanion.INSTANCE.getAllIntersecting(level, box)) {
            Vec3 aboard = body.logicalPose().transformPositionInverse(receiver);
            around(level, cells, counted, aboard, reach, body, out);
            carried(level, counted, aboard, reach, body, out);
        }
    }

    private static void carried(ServerLevel level, Set<Object> counted, Vec3 receiver, double reach, @Nullable SubLevelAccess body, List<Radiation.Source> out) {
        AABB box = new AABB(receiver, receiver).inflate(reach);
        for (Entity e : level.getEntities((Entity) null, box, e -> e.isAlive() && !(e instanceof Player) && !(e instanceof ItemEntity))) {
            if (!counted.add(e)) {
                continue;
            }
            if (e instanceof AbstractContraptionEntity moving) {
                aboard(moving, counted, body, out);
                continue;
            }
            double strength = carriedBy(e);
            if (strength > 0) {
                out.add(new Radiation.Source(e.getBoundingBox().getCenter(), strength, null, body, null));
            }
        }
    }

    public static double carriedBy(Entity e) {
        if (e instanceof ItemFrame frame) {
            return Radiation.itemStrength(frame.getItem());
        }
        if (e instanceof ContainerEntity box) {
            return box.getLootTable() != null ? 0 : strength(box.getItemStacks());
        }
        double sum = 0;
        if (e instanceof LivingEntity living) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                sum += Radiation.itemStrength(living.getItemBySlot(slot));
            }
        }
        if (e instanceof AbstractHorse horse) {
            sum += strength(horse.getInventory());
        }
        if (e instanceof InventoryCarrier carrier) {
            sum += strength(carrier.getInventory());
        }
        if (e instanceof LivingEntity) {
            return sum;
        }
        if (e instanceof Container box) {
            return strength(box);
        }
        IItemHandler handler = e.getCapability(Capabilities.ItemHandler.ENTITY);
        if (handler != null) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                sum += Radiation.itemStrength(handler.getStackInSlot(slot));
            }
        }
        return sum;
    }

    private static double strength(Iterable<ItemStack> stacks) {
        double sum = 0;
        for (ItemStack stack : stacks) {
            sum += Radiation.itemStrength(stack);
        }
        return sum;
    }

    private static double strength(Container box) {
        double sum = 0;
        for (int slot = 0; slot < box.getContainerSize(); slot++) {
            sum += Radiation.itemStrength(box.getItem(slot));
        }
        return sum;
    }

    private static void aboard(AbstractContraptionEntity moving, Set<Object> counted, @Nullable SubLevelAccess body, List<Radiation.Source> out) {
        Contraption contraption = moving.getContraption();
        if (contraption == null) {
            return;
        }
        MountedStorageManager storage = contraption.getStorage();
        Map<BlockPos, MountedItemStorage> items;
        Map<BlockPos, MountedFluidStorage> liquids;
        try {
            items = storage.getAllItemStorages();
            liquids = storage.getFluids().storages;
        } catch (IllegalStateException unbuilt) {
            return;
        }
        for (Map.Entry<BlockPos, MountedItemStorage> at : items.entrySet()) {
            MountedItemStorage held = at.getValue();
            if (!counted.add(held)) {
                continue;
            }
            double strength = 0;
            for (int slot = 0; slot < held.getSlots(); slot++) {
                strength += Radiation.itemStrength(held.getStackInSlot(slot));
            }
            if (strength > 0) {
                out.add(new Radiation.Source(moving.toGlobalVector(Vec3.atCenterOf(at.getKey()), 1), strength, null, body, null));
            }
        }
        for (Map.Entry<BlockPos, MountedFluidStorage> at : liquids.entrySet()) {
            if (!counted.add(at.getValue())) {
                continue;
            }
            double strength = liquidStrength(at.getValue());
            if (strength > 0) {
                out.add(new Radiation.Source(moving.toGlobalVector(Vec3.atCenterOf(at.getKey()), 1), strength, null, body, null));
            }
        }
    }

    private static void around(ServerLevel level, Long2ObjectOpenHashMap<Cell> cells, Set<Object> counted, Vec3 receiver, double reach, @Nullable SubLevelAccess body, List<Radiation.Source> out) {
        long now = level.getGameTime();
        int x0 = Mth.floor(receiver.x - reach) >> 4;
        int x1 = Mth.floor(receiver.x + reach) >> 4;
        int z0 = Mth.floor(receiver.z - reach) >> 4;
        int z1 = Mth.floor(receiver.z + reach) >> 4;
        for (int cx = x0; cx <= x1; cx++) {
            for (int cz = z0; cz <= z1; cz++) {
                long key = ChunkPos.asLong(cx, cz);
                Cell cell = cells.get(key);
                if (cell == null || now < cell.time || now - cell.time >= KEPT_TICKS) {
                    cell = new Cell(now, scan(level, cx, cz));
                    cells.put(key, cell);
                }
                for (Held h : cell.held) {
                    double strength = 0;
                    for (ItemStack stack : h.stacks) {
                        if (counted.add(stack)) {
                            strength += Radiation.itemStrength(stack);
                        }
                    }
                    if (h.liquid != null && counted.add(h.liquid)) {
                        strength += liquidStrength(h.liquid);
                    }
                    if (strength > 0) {
                        out.add(new Radiation.Source(h.pos, strength, h.box, body, null));
                    }
                }
            }
        }
    }

    private static List<Held> scan(ServerLevel level, int cx, int cz) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
        if (chunk == null || chunk.getBlockEntities().isEmpty()) {
            return List.of();
        }
        List<Held> out = new ArrayList<>();
        for (BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values())) {
            if (be.isRemoved() || be instanceof SpentFuelRackBlockEntity || be instanceof ShieldedCaskBlockEntity || be instanceof ServicePortBlockEntity) {
                continue;
            }
            if (be instanceof RandomizableContainerBlockEntity unopened && unopened.getLootTable() != null) {
                continue;
            }
            BlockPos pos = be.getBlockPos();
            List<ItemStack> hot = List.of();
            if (be instanceof ArmBlockEntity arm) {
                ItemStack gripped = ((ArmBlockEntityAccessor) arm).criticalmass$heldItem();
                if (Radiation.itemStrength(gripped) > 0) {
                    hot = new ArrayList<>(List.of(gripped));
                }
            }
            IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, be.getBlockState(), be, null);
            if (items != null) {
                for (int slot = 0; slot < items.getSlots(); slot++) {
                    ItemStack stack = items.getStackInSlot(slot);
                    if (Radiation.itemStrength(stack) > 0) {
                        if (hot.isEmpty()) {
                            hot = new ArrayList<>();
                        }
                        hot.add(stack);
                    }
                }
            }
            IFluidHandler liquid = liquid(level, be);
            if (hot.isEmpty() && liquid == null) {
                continue;
            }
            if (liquid != null && be instanceof FluidTankBlockEntity tank) {
                double side = (tank.getWidth() - 1) / 2.0;
                double rise = (tank.getHeight() - 1) / 2.0;
                out.add(new Held(pos.getCenter().add(side, rise, side), new AABB(pos).expandTowards(tank.getWidth() - 1, tank.getHeight() - 1, tank.getWidth() - 1), hot, liquid));
            } else {
                out.add(new Held(pos.getCenter(), new AABB(pos), hot, liquid));
            }
        }
        return out;
    }

    @Nullable
    private static IFluidHandler liquid(ServerLevel level, BlockEntity be) {
        ResourceLocation type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType());
        if (type == null || type.getNamespace().equals(CriticalMass.MODID) || be instanceof FluidTankBlockEntity tank && !tank.isController()) {
            return null;
        }
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, be.getBlockPos(), be.getBlockState(), be, null);
        return handler != null && liquidStrength(handler) > 0 ? handler : null;
    }
}
