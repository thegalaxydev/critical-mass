package com.criticalmass.content.radiation;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.fuel.ServicePortBlockEntity;
import com.criticalmass.content.fuel.ShieldedCaskBlockEntity;
import com.criticalmass.content.fuel.SpentFuelRackBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
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
        BoundingBox3d box = new BoundingBox3d(receiver.x - reach, receiver.y - reach, receiver.z - reach, receiver.x + reach, receiver.y + reach, receiver.z + reach);
        for (SubLevelAccess body : SableCompanion.INSTANCE.getAllIntersecting(level, box)) {
            around(level, cells, counted, body.logicalPose().transformPositionInverse(receiver), reach, body, out);
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
            BlockPos pos = be.getBlockPos();
            List<ItemStack> hot = List.of();
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
