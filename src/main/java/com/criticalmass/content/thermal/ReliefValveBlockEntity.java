package com.criticalmass.content.thermal;

import com.criticalmass.content.effects.Effects;
import com.criticalmass.registry.CMSounds;
import net.minecraft.world.phys.Vec3;
import com.criticalmass.content.FacingBlock;
import com.criticalmass.content.ReliefValveBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ReliefValveBlockEntity extends SingleBufferNodeBlockEntity {
    private double circuitPressure;
    private CoolantBuffer worst;
    private int circuitTicks;
    private boolean pipesChecked;

    public ReliefValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, CMThermal.RELIEF_CAPACITY);
    }

    private Direction vent() {
        return getBlockState().getValue(FacingBlock.FACING);
    }

    public boolean venting() {
        return getBlockState().getValue(ReliefValveBlock.VENTING);
    }

    @Override
    @Nullable
    public Direction passThroughExit(Direction enteredFrom) {
        return enteredFrom.getAxis() != vent().getAxis() ? enteredFrom.getOpposite() : null;
    }

    @Override
    public void setCircuitPressure(double pressure, @Nullable CoolantBuffer worst) {
        this.circuitPressure = pressure;
        this.worst = worst;
        this.circuitTicks = 3;
    }

    public double sensedPressure() {
        return Math.max(buffer.pressure(), circuitTicks > 0 ? circuitPressure : 0);
    }

    @Override
    protected void serverTick() {
        if (!pipesChecked && level.hasChunksAt(worldPosition.offset(-1, -1, -1), worldPosition.offset(1, 1, 1))) {
            pipesChecked = true;
            BlockState joined = ReliefValveBlock.withPipes(level, worldPosition, getBlockState());
            if (joined != getBlockState()) {
                level.setBlock(worldPosition, joined, Block.UPDATE_ALL);
            }
        }
        if (circuitTicks > 0) {
            circuitTicks--;
        } else {
            worst = null;
        }
        boolean open = level.getBlockState(worldPosition.relative(vent())).isAir();
        boolean venting = venting();
        double p = sensedPressure();
        boolean next;
        if (!open) {
            next = false;
            setStatus("criticalmass.relief.blocked");
        } else if (venting) {
            next = p >= CMThermal.RESEAT_PRESSURE && buffer.amount() > 0;
        } else {
            next = p >= CMThermal.RELIEF_PRESSURE;
        }
        if (next) {
            CoolantBuffer from = worst != null && worst.pressure() > buffer.pressure() && worst.amount() > 0 ? worst : buffer;
            sinks.vent(from.take(CMThermal.RELIEF_VENT_RATE));
            Direction v = vent();
            if (level.getGameTime() % 20 == 0 || !venting) {
                Vec3 mouth = Vec3.atCenterOf(worldPosition).add(v.getStepX() * 0.6, v.getStepY() * 0.6, v.getStepZ() * 0.6);
                Effects.steamJet(level, mouth, new Vec3(v.getStepX(), v.getStepY(), v.getStepZ()), 1.0f);
                level.playSound(null, worldPosition, CMSounds.STEAM_VENT.get(), SoundSource.BLOCKS, 1.2f, 0.95f + level.random.nextFloat() * 0.1f);
            }
            setStatus("criticalmass.relief.venting");
        } else if (open) {
            setStatus("criticalmass.relief.closed");
        }
        if (next != venting) {
            level.setBlock(worldPosition, getBlockState().setValue(ReliefValveBlock.VENTING, next), Block.UPDATE_ALL);
        }
    }
}
