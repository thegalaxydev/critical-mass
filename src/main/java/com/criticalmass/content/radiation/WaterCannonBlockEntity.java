package com.criticalmass.content.radiation;

import com.criticalmass.content.effects.Effects;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMSounds;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import dev.ryanhcode.sable.companion.SableCompanion;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class WaterCannonBlockEntity extends KineticBlockEntity {
    public static final int REACH = 24;
    public static final int MIN_REACH = 4;
    public static final int DROP = 48;
    public static final int RISE = 12;
    public static final int TANK_MB = 4000;
    public static final int WATER_PER_BLOCK = 10;
    public static final int PER_TICK = 3;
    public static final float TURN = 0.075f;
    public static final double NOZZLE_OUT = 0.77;
    public static final double NOZZLE_UP = 1.27;
    private static final double ARC_STEP = 0.7;
    private static final int SPRAY_TICKS = 30;
    private static final int SYNC_TICKS = 5;
    private final FluidTank tank = new FluidTank(TANK_MB, stack -> stack.getFluid().isSame(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private float angle;
    private float lastAngle;
    private int spraying;
    private int syncIn;
    private long washed;
    private Vec3 target = Vec3.ZERO;
    private String status = "criticalmass.cannon.stopped";
    private ScrollValueBehaviour reach;

    public WaterCannonBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        reach = new ScrollValueBehaviour(Component.translatable("criticalmass.cannon.reach"), this, new ReachBox());
        reach.between(MIN_REACH, REACH);
        reach.value = REACH;
        behaviours.add(reach);
    }

    public int reach() {
        return reach.getValue();
    }

    public void setReach(int blocks) {
        reach.setValue(blocks);
    }

    public FluidTank tank() {
        return tank;
    }

    public String status() {
        return status;
    }

    public boolean spraying() {
        return spraying > 0;
    }

    public long washed() {
        return washed;
    }

    public float angle(float partialTicks) {
        return Mth.lerp(partialTicks, lastAngle, angle);
    }

    private void turn() {
        lastAngle = angle;
        angle += getSpeed() * TURN;
        if (angle >= 360 || angle < 0) {
            float wrap = angle >= 360 ? -360 : 360;
            angle += wrap;
            lastAngle += wrap;
        }
    }

    private Vec3 mount() {
        return SableCompanion.INSTANCE.projectOutOfSubLevel(level, Vec3.atBottomCenterOf(worldPosition));
    }

    private Vec3 nozzle(float degrees) {
        double a = Math.toRadians(degrees);
        return mount().add(-Math.sin(a) * NOZZLE_OUT, NOZZLE_UP, -Math.cos(a) * NOZZLE_OUT);
    }

    private boolean wash() {
        Vec3 origin = mount();
        float sweep = angle - lastAngle;
        int far = reach();
        int rays = Math.max(1, Mth.ceil(Math.abs(Math.toRadians(sweep)) * far / ARC_STEP));
        int budget = Math.min(PER_TICK, tank.getFluidAmount() / WATER_PER_BLOCK);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int ray = 1; ray <= rays && budget > 0; ray++) {
            double a = Math.toRadians(lastAngle + sweep * ray / rays);
            double dx = -Math.sin(a);
            double dz = -Math.cos(a);
            long last = Long.MIN_VALUE;
            for (double out = 1.5; out <= far && budget > 0; out += 0.5) {
                int x = Mth.floor(origin.x + dx * out);
                int z = Mth.floor(origin.z + dz * out);
                long column = BlockPos.asLong(x, 0, z);
                if (column == last || !level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                last = column;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                if (y > origin.y + RISE || y < origin.y - DROP) {
                    continue;
                }
                cursor.set(x, y, z);
                BlockState here = level.getBlockState(cursor);
                boolean fire = here.is(BlockTags.FIRE);
                if (!fire && !here.is(CMBlocks.FALLOUT_ASH.get())) {
                    continue;
                }
                level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                if (fire) {
                    level.playSound(null, cursor, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.4f);
                }
                tank.drain(WATER_PER_BLOCK, IFluidHandler.FluidAction.EXECUTE);
                target = Vec3.atBottomCenterOf(cursor);
                washed++;
                budget--;
                any = true;
            }
        }
        return any;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        turn();
        if (level.isClientSide) {
            if (spraying > 0) {
                spraying--;
                Effects.waterJet(level, nozzle(angle), target);
                Effects.loop(level, worldPosition, CMSounds.WATER_SPRAY.get(), 0.7f, 1f);
            }
            return;
        }
        String now;
        boolean hit = false;
        if (getSpeed() == 0) {
            now = "criticalmass.cannon.stopped";
        } else if (tank.getFluidAmount() < WATER_PER_BLOCK) {
            now = "criticalmass.cannon.dry";
        } else {
            hit = wash();
            now = null;
        }
        if (hit) {
            spraying = SPRAY_TICKS;
        } else if (spraying > 0) {
            spraying--;
        }
        if (now == null) {
            now = spraying > 0 ? "criticalmass.cannon.spraying" : "criticalmass.cannon.clear";
        } else {
            spraying = 0;
        }
        boolean changed = !now.equals(status);
        status = now;
        if (changed || (syncIn-- <= 0 && (hit || spraying > 0))) {
            syncIn = SYNC_TICKS;
            setChanged();
            sendData();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putFloat("Angle", angle);
        tag.putInt("Spraying", spraying);
        tag.putLong("Washed", washed);
        tag.putString("Status", status);
        tag.putDouble("TargetX", target.x);
        tag.putDouble("TargetY", target.y);
        tag.putDouble("TargetZ", target.z);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        tank.readFromNBT(registries, tag.getCompound("Tank"));
        float sent = tag.getFloat("Angle");
        if (!clientPacket || Math.abs(Mth.wrapDegrees(sent - angle)) > 20) {
            angle = sent;
            lastAngle = sent;
        }
        spraying = tag.getInt("Spraying");
        washed = tag.getLong("Washed");
        status = tag.contains("Status") ? tag.getString("Status") : "criticalmass.cannon.stopped";
        target = new Vec3(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ"));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.kinetic(tooltip, super.addToGoggleTooltip(tooltip, isPlayerSneaking));
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ThermalTooltips.status(tooltip, status, status.endsWith("spraying") ? ChatFormatting.GREEN : status.endsWith("clear") ? ChatFormatting.AQUA : ChatFormatting.GOLD);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.water");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.millibuckets", ChatFormatting.AQUA, ThermalTooltips.count(tank.getFluidAmount())));
        return true;
    }

    static class ReachBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 2.5, 16.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction.getAxis().isHorizontal();
        }
    }
}
