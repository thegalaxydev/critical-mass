package com.criticalmass.content.power;

import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.content.power.TransferSwitchBlock.Position;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMSounds;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TransferSwitchBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, WireNode {
    public static final int NORMAL = 0;
    public static final int STANDBY = 1;
    public static final int LOAD = 2;
    private static final int CHECK_TICKS = 5;
    private final WireLinks wires = new WireLinks();
    private boolean calling;
    private double normalKv;
    private double standbyKv;
    private int lostFor;
    private int backFor;
    private int pause;
    private int checkIn;

    public TransferSwitchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
    public void wiresChanged() {
        setChanged();
        sendData();
    }

    private Direction normalSide() {
        return getBlockState().getValue(HorizontalBlock.FACING).getClockWise();
    }

    @Override
    public Vec3 terminal(int index) {
        if (index == LOAD) {
            return new Vec3(0.5, 1.0, 0.5);
        }
        Direction side = index == NORMAL ? normalSide() : normalSide().getOpposite();
        return new Vec3(0.5 + side.getStepX() * 0.3, 1.0, 0.5 + side.getStepZ() * 0.3);
    }

    @Override
    public int terminalAt(Vec3 local) {
        Direction side = normalSide();
        double along = (local.x - 0.5) * side.getStepX() + (local.z - 0.5) * side.getStepZ();
        return along > 0.17 ? NORMAL : along < -0.17 ? STANDBY : LOAD;
    }

    public Position position() {
        return getBlockState().getValue(TransferSwitchBlock.POSITION);
    }

    public boolean calling() {
        return calling;
    }

    public double normalKilovolts() {
        return normalKv;
    }

    public double standbyKilovolts() {
        return standbyKv;
    }

    private static boolean healthy(PowerNetwork.Island island, double kilovolts, net.minecraft.world.level.Level level) {
        boolean fed = !island.grids.isEmpty();
        for (GeneratorBlockEntity g : island.machines(level)) {
            fed |= g.online();
        }
        double hz = island.side(level).frequency();
        return fed && kilovolts > 0 && hz >= Power.SUPPLY_MIN_HZ && hz <= Power.SUPPLY_MAX_HZ;
    }

    private void move(Position to) {
        if (position() == to) {
            return;
        }
        level.setBlock(worldPosition, getBlockState().setValue(TransferSwitchBlock.POSITION, to), Block.UPDATE_ALL);
        PowerNetwork.changed(level);
        level.playSound(null, worldPosition, to == Position.OFF ? CMSounds.BREAKER_OPEN.get() : CMSounds.BREAKER_CLOSE.get(), SoundSource.BLOCKS, 1.0f, 1.1f);
        pause = Power.TRANSFER_PAUSE_TICKS;
        setChanged();
    }

    private void call(boolean now) {
        if (calling != now) {
            calling = now;
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            setChanged();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || checkIn-- > 0) {
            return;
        }
        checkIn = CHECK_TICKS;
        Position at = position();
        PowerNetwork.Island normal = PowerNetwork.scanAt(level, worldPosition, NORMAL);
        PowerNetwork.Island standby = PowerNetwork.scanAt(level, worldPosition, STANDBY);
        normalKv = normal.kilovoltsAt(level, worldPosition, NORMAL);
        standbyKv = standby.kilovoltsAt(level, worldPosition, STANDBY);
        boolean normalGood = healthy(normal, normalKv, level);
        boolean standbyGood = healthy(standby, standbyKv, level);
        lostFor = normalGood ? 0 : lostFor + CHECK_TICKS;
        backFor = normalGood ? backFor + CHECK_TICKS : 0;
        if (pause > 0) {
            pause -= CHECK_TICKS;
        } else if (at == Position.NORMAL) {
            if (lostFor >= Power.TRANSFER_LOSS_TICKS) {
                move(Position.OFF);
            }
        } else if (at == Position.STANDBY) {
            if (backFor >= Power.TRANSFER_RETURN_TICKS || !standbyGood) {
                move(Position.OFF);
            }
        } else if (normalGood && (backFor >= Power.TRANSFER_RETURN_TICKS || !calling)) {
            move(Position.NORMAL);
        } else if (standbyGood && (calling || !normalGood)) {
            move(Position.STANDBY);
        }
        call(lostFor >= Power.TRANSFER_LOSS_TICKS || (calling && backFor < Power.TRANSFER_RETURN_TICKS) || (calling && position() != Position.NORMAL));
        sendData();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Calling", calling);
        tag.putDouble("NormalKv", normalKv);
        tag.putDouble("StandbyKv", standbyKv);
        tag.putInt("LostFor", lostFor);
        tag.putInt("BackFor", backFor);
        tag.put("Wires", wires.save());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        calling = tag.getBoolean("Calling");
        normalKv = tag.getDouble("NormalKv");
        standbyKv = tag.getDouble("StandbyKv");
        lostFor = tag.getInt("LostFor");
        backFor = tag.getInt("BackFor");
        wires.load(tag, "Wires");
    }

    private static Component side(double kilovolts) {
        return kilovolts > 0 ? ThermalTooltips.reading("criticalmass.unit.kilovolts", ChatFormatting.AQUA, ThermalTooltips.number(kilovolts))
                : ThermalTooltips.reading("criticalmass.breaker.dead", ChatFormatting.GRAY);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        Position at = position();
        ThermalTooltips.status(tooltip, "criticalmass.transfer." + at.getSerializedName(), at == Position.NORMAL ? ChatFormatting.GREEN : at == Position.STANDBY ? ChatFormatting.GOLD : ChatFormatting.GRAY);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.normal_side");
        ThermalTooltips.value(tooltip, side(normalKv));
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.standby_side");
        ThermalTooltips.value(tooltip, side(standbyKv));
        return true;
    }
}
