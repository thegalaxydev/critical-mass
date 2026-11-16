package com.criticalmass.content.kinetics;

import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.LoadedThermalBlocks;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMStress;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class ControlRodDriveBlockEntity extends KineticBlockEntity {
    private double insertion = 1.0;
    private double target = 1.0;
    private boolean moving;
    private boolean failSafe;
    private int reactorTripTicks;
    private int syncCooldown;
    private int bank;
    private BankChoice bankChoice;
    private int travel;
    private double bankCommand = Double.NaN;
    private int linkTicks;
    private int withdrawBlock;
    private double requested = 1.0;
    public float shownInsertion = 1.0f;

    public ControlRodDriveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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

    public double insertion() {
        return insertion;
    }

    public double target() {
        return target;
    }

    public boolean moving() {
        return moving;
    }

    public boolean failSafe() {
        return failSafe;
    }

    public void setInsertion(double value) {
        insertion = Math.max(0, Math.min(1, value));
    }

    public int bank() {
        return bank;
    }

    public void setBank(int value) {
        bank = Math.floorMod(value, CMThermal.ROD_BANKS);
        bankChoice.value = bank;
        setChanged();
        sendData();
    }

    public ScrollValueBehaviour bankChoice() {
        return bankChoice;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        bankChoice = new BankChoice(this);
        bankChoice.withCallback(this::setBank);
        behaviours.add(bankChoice);
    }

    public int travel() {
        return travel;
    }

    public void linkToReactor(int columnHeight, double command) {
        if (columnHeight != travel) {
            travel = columnHeight;
            syncCooldown = 0;
        }
        bankCommand = command;
        linkTicks = CMThermal.ROD_LINK_TICKS;
    }

    public void blockWithdrawal() {
        withdrawBlock = CMThermal.WITHDRAW_BLOCK_TICKS;
    }

    public double requested() {
        return requested;
    }

    public boolean withdrawalBlocked() {
        return withdrawBlock > 0;
    }

    public void reactorTripped() {
        reactorTripTicks = 3;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (reactorTripTicks > 0) {
            reactorTripTicks--;
        }
        if (linkTicks > 0) {
            linkTicks--;
        } else {
            bankCommand = Double.NaN;
        }
        target = Double.isNaN(bankCommand) ? 1.0 - level.getBestNeighborSignal(worldPosition) / 15.0 : bankCommand;
        requested = target;
        if (withdrawBlock > 0) {
            withdrawBlock--;
            target = Math.max(target, insertion);
        }
        boolean latched = getSpeed() != 0 && !isOverStressed() && reactorTripTicks == 0;
        boolean wasMoving = moving;
        failSafe = !latched && insertion < 1.0;
        if (!latched) {
            moving = false;
            if (insertion < 1.0) {
                insertion = Math.min(1.0, insertion + 1.0 / CMThermal.ROD_FAILSAFE_TICKS);
            }
        } else {
            double rate = Math.min(Math.abs(getSpeed()) * CMThermal.ROD_RATE_PER_RPM, CMThermal.ROD_MAX_RATE);
            double gap = target - insertion;
            moving = Math.abs(gap) > 1e-9;
            if (moving) {
                insertion += Math.signum(gap) * Math.min(Math.abs(gap), rate);
            }
        }
        if (wasMoving != moving && hasNetwork()) {
            getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
            getOrCreateNetwork().updateStress();
        }
        if (syncCooldown-- <= 0 || wasMoving != moving) {
            syncCooldown = moving || failSafe ? 2 : 10;
            setChanged();
            sendData();
        }
    }

    @Override
    public float calculateStressApplied() {
        float impact = moving ? (float) CMStress.CONTROL_ROD_DRIVE_IMPACT : 0f;
        lastStressApplied = impact;
        return impact;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("Insertion", insertion);
        tag.putDouble("Target", target);
        tag.putBoolean("Moving", moving);
        tag.putBoolean("FailSafe", failSafe);
        tag.putInt("Bank", bank);
        tag.putInt("Travel", travel);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        insertion = tag.contains("Insertion") ? tag.getDouble("Insertion") : 1.0;
        target = tag.contains("Target") ? tag.getDouble("Target") : 1.0;
        moving = tag.getBoolean("Moving");
        failSafe = tag.getBoolean("FailSafe");
        bank = Math.floorMod(tag.getInt("Bank"), CMThermal.ROD_BANKS);
        bankChoice.value = bank;
        travel = tag.getInt("Travel");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.kinetic(tooltip, super.addToGoggleTooltip(tooltip, isPlayerSneaking));
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        if (failSafe) {
            ThermalTooltips.status(tooltip, "criticalmass.rod.fail_safe", ChatFormatting.RED);
        }
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.insertion", letter(bank));
        MutableComponent position = ThermalTooltips.reading("criticalmass.unit.percent", ChatFormatting.AQUA, Math.round(insertion * 100));
        ThermalTooltips.value(tooltip, moving ? position.append(ThermalTooltips.aside("criticalmass.goggles.aside.target", Math.round(target * 100))) : position);
        return true;
    }

    private static String letter(int bank) {
        return String.valueOf((char) ('A' + bank));
    }

    static class BankChoice extends ScrollValueBehaviour {
        BankChoice(SmartBlockEntity be) {
            super(Component.translatable("criticalmass.rod.bank"), be, new BankBox());
            between(0, CMThermal.ROD_BANKS - 1);
            withFormatter(ControlRodDriveBlockEntity::letter);
        }

        @Override
        public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
            return new ValueSettingsBoard(label, max, 1, ImmutableList.of(Component.translatable("criticalmass.rod.bank_row")),
                    new ValueSettingsFormatter(settings -> Component.literal(letter(settings.value()))));
        }
    }

    static class BankBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 12, 13.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction.getAxis().isHorizontal();
        }
    }
}
