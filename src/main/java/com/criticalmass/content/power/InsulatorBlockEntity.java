package com.criticalmass.content.power;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class InsulatorBlockEntity extends SmartBlockEntity implements WireNode {
    private final WireLinks wires = new WireLinks();

    public InsulatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public WireLinks wires() {
        return wires;
    }

    @Override
    public Vec3 terminal(int index) {
        Direction facing = getBlockState().getValue(InsulatorBlock.FACING);
        return new Vec3(0.5 + facing.getStepX() * 0.0625, 0.5 + facing.getStepY() * 0.0625, 0.5 + facing.getStepZ() * 0.0625);
    }

    @Override
    public void wiresChanged() {
        setChanged();
        sendData();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Wires", wires.save());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        wires.load(tag, "Wires");
    }
}
