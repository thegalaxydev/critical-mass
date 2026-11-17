package com.criticalmass.content.radiation;

import com.criticalmass.content.fuel.FuelAssemblyItem;
import com.criticalmass.content.fuel.FuelState;
import com.criticalmass.content.fuel.SealedWasteCanisterBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.VesselStructure;
import com.criticalmass.content.thermal.Corium;
import com.criticalmass.content.thermal.VesselCasingBlock;
import com.criticalmass.content.fuel.ServicePortBlockEntity;
import com.criticalmass.content.fuel.ShieldedCaskBlockEntity;
import com.criticalmass.content.fuel.SpentFuelRackBlockEntity;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.thermal.LoadedThermalBlocks;
import com.criticalmass.content.thermal.PipeCoolant;
import com.criticalmass.content.thermal.ThermalNode;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMConfig;
import com.criticalmass.registry.CMItems;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class Radiation {
    public static final double REACTOR_DOSE = 1.0;
    public static final double FRESH_DOSE = 0.5;
    public static final double SPENT_DOSE = 5.0;
    public static final double RESIDUAL_DOSE = 20.0;
    public static final double CANISTER_DOSE = 3.0;
    public static final double CASK_TRANSMISSION = 0.01;
    public static final double OPEN_CASK_TRANSMISSION = 0.5;
    public static final double FLOODED_RACK_TRANSMISSION = 0.1;
    public static final double PORT_TRANSMISSION = 0.5;
    public static final double FIELD_DOSE = 1.0;
    public static final double SKIN_DOSE = 0.5;
    public static final double GEAR_DOSE = 0.1;
    public static final double RANGE = 48;
    public static final double STEP = 0.25;

    public record Source(Vec3 pos, double strength, @Nullable AABB ignore, @Nullable SubLevelAccess body, @Nullable Player holder) {
    }

    private Radiation() {
    }

    public static double itemStrength(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        FuelState s = stack.get(CMComponents.FUEL_STATE.get());
        double residual = s == null ? 0 : s.residual();
        if (stack.getItem() instanceof FuelAssemblyItem a) {
            return stack.getCount() * switch (a.form()) {
                case FRESH -> FRESH_DOSE + RESIDUAL_DOSE * residual;
                case SPENT -> SPENT_DOSE + RESIDUAL_DOSE * residual;
                case DAMAGED -> 2 * (SPENT_DOSE + RESIDUAL_DOSE * residual);
            };
        }
        if (stack.is(CMBlocks.CORIUM.get().asItem())) {
            return stack.getCount() * CMThermal.CORIUM_ITEM_DOSE;
        }
        if (stack.is(CMBlocks.RUINED_CHANNEL.get().asItem())) {
            return stack.getCount() * CMThermal.RUINED_ITEM_DOSE;
        }
        if (stack.is(CMItems.SEALED_WASTE_CANISTER.get())) {
            return stack.getCount() * (CANISTER_DOSE + RESIDUAL_DOSE * residual);
        }
        if (stack.is(CMItems.INCOMPLETE_WASTE_CANISTER.get())) {
            return stack.getCount() * (s == null ? CANISTER_DOSE : 2 * (SPENT_DOSE + RESIDUAL_DOSE * residual));
        }
        return 0;
    }

    public static double inventoryStrength(Player player) {
        double sum = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            sum += itemStrength(player.getInventory().getItem(i));
        }
        return sum;
    }

    public static double mu(Level level, BlockPos pos, BlockState state) {
        if (state.is(CMBlocks.SHIELDING_CONCRETE.get())) {
            return CMConfig.get(CMConfig.SHIELD_CONCRETE);
        }
        if (state.getBlock() instanceof VesselCasingBlock) {
            return CMConfig.get(CMConfig.SHIELD_CASING);
        }
        if (state.is(CMBlocks.SHIELDED_GLASS.get())) {
            return CMConfig.get(CMConfig.SHIELD_GLASS);
        }
        if (state.is(CMBlocks.SHIELDED_HATCH.get())) {
            return state.getValue(TrapDoorBlock.OPEN) ? CMConfig.get(CMConfig.SHIELD_HATCH_OPEN) : CMConfig.get(CMConfig.SHIELD_HATCH_CLOSED);
        }
        if (state.getFluidState().is(FluidTags.WATER)) {
            return CMConfig.get(CMConfig.SHIELD_WATER);
        }
        if (state.isSolidRender(level, pos) || VesselStructure.channelKind(state.getBlock()) != VesselStructure.Kind.NONE || state.is(CMBlocks.RUINED_CHANNEL.get())) {
            return CMConfig.get(CMConfig.SHIELD_SOLID);
        }
        return 0;
    }

    public static double transmission(Level level, Vec3 from, Vec3 to, @Nullable AABB ignore) {
        double length = from.distanceTo(to);
        if (length < 1e-6) {
            return 1;
        }
        int steps = (int) Math.ceil(length / STEP);
        double ds = length / steps;
        Vec3 dir = to.subtract(from).scale(1.0 / steps);
        Map<Long, Double> cache = new HashMap<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        double sum = 0;
        for (int i = 0; i < steps; i++) {
            Vec3 p = from.add(dir.scale(i + 0.5));
            if (ignore != null && ignore.contains(p)) {
                continue;
            }
            m.set(p.x, p.y, p.z);
            long key = m.asLong();
            Double mu = cache.get(key);
            if (mu == null) {
                mu = level.isLoaded(m) ? mu(level, m, level.getBlockState(m)) : 0;
                cache.put(key, mu);
            }
            sum += mu * ds;
        }
        return Math.exp(-sum);
    }

    @Nullable
    private static SubLevelAccess body(BlockEntity be) {
        return SableCompanion.INSTANCE.getContaining(be);
    }

    public static List<Source> blockSources(ServerLevel level) {
        List<Source> out = new ArrayList<>();
        for (BlockEntity be : LoadedThermalBlocks.snapshot()) {
            if (be.getLevel() != level) {
                continue;
            }
            Vec3 c = be.getBlockPos().getCenter();
            if (be instanceof ReactorControllerBlockEntity r) {
                Vec3 center = r.coreCenter();
                double s = r.core().lastOutput() * REACTOR_DOSE;
                if (center != null && s > 0) {
                    out.add(new Source(center, s, r.coreInterior(), body(be), null));
                }
            } else if (be instanceof SpentFuelRackBlockEntity rack) {
                double s = 0;
                for (ItemStack st : rack.slots()) {
                    s += itemStrength(st);
                }
                if (s > 0) {
                    out.add(new Source(c, s * (rack.coolant().amount() > 0 ? FLOODED_RACK_TRANSMISSION : 1), null, body(be), null));
                }
            } else if (be instanceof ShieldedCaskBlockEntity cask) {
                double s = 0;
                for (ItemStack st : cask.slots()) {
                    s += itemStrength(st);
                }
                if (s > 0) {
                    out.add(new Source(c, s * (cask.sealed() ? CASK_TRANSMISSION : OPEN_CASK_TRANSMISSION), null, body(be), null));
                }
            } else if (be instanceof Corium.Entity_ lump) {
                out.add(new Source(c, lump.strength(), null, body(be), null));
            } else if (be instanceof SealedWasteCanisterBlockEntity canister) {
                out.add(new Source(c, canister.strength(), null, body(be), null));
            } else if (be instanceof ServicePortBlockEntity port) {
                double s = itemStrength(port.output()) * PORT_TRANSMISSION;
                if (s > 0) {
                    out.add(new Source(c, s, null, body(be), null));
                }
            } else if (be instanceof CirculationPumpBlockEntity pump) {
                for (ThermalNode node : pump.pipeContents()) {
                    if (node instanceof PipeCoolant pipe && !pipe.reinforced() && pipe.coolant().amount() > 0 && CMThermal.isPrimaryMedium(pipe.coolant().fluid())) {
                        double dose = CMThermal.isContaminated(pipe.coolant().fluid()) ? CMThermal.CONTAMINATED_PIPE_DOSE : CMThermal.PLAIN_PIPE_DOSE;
                        out.add(new Source(pipe.pos().getCenter(), dose * pipe.coolant().amount() / CMThermal.PIPE_CAPACITY, null, body(be), null));
                    }
                }
            }
        }
        return out;
    }

    public static List<Source> sourcesNear(ServerLevel level, Vec3 receiver, List<Source> blocks) {
        List<Source> out = new ArrayList<>(blocks);
        AABB box = new AABB(receiver, receiver).inflate(RANGE / 2);
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, box)) {
            double s = itemStrength(e.getItem());
            if (s > 0) {
                out.add(new Source(e.position().add(0, 0.25, 0), s, null, null, null));
            }
        }
        for (Player p : level.getEntitiesOfClass(Player.class, box)) {
            double s = inventoryStrength(p);
            if (s > 0) {
                out.add(new Source(p.position().add(0, 1, 0), s, null, null, p));
            }
        }
        return out;
    }

    public static double fromSources(Level level, Vec3 receiver, List<Source> sources, @Nullable Player self) {
        double rate = 0;
        for (Source s : sources) {
            if (s.holder() != null && s.holder() == self) {
                continue;
            }
            Vec3 r = s.body() != null ? s.body().logicalPose().transformPositionInverse(receiver) : receiver;
            double d2 = s.pos().distanceToSqr(r);
            if (d2 > RANGE * RANGE) {
                continue;
            }
            rate += s.strength() * transmission(level, s.pos(), r, s.ignore()) / (1 + d2);
        }
        return rate;
    }

    public static double fieldDose(ServerLevel level, Vec3 receiver) {
        return ContaminationData.get(level).near(receiver, 3) * FIELD_DOSE;
    }

    public static double ambientRate(ServerLevel level, Vec3 receiver) {
        return fromSources(level, receiver, sourcesNear(level, receiver, blockSources(level)), null) + fieldDose(level, receiver) + Fallout.rate(level, receiver);
    }
}
