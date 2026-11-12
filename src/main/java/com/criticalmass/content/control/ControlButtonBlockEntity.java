package com.criticalmass.content.control;

import com.criticalmass.content.kinetics.GeneratorBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import java.util.List;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ControlButtonBlockEntity extends BoundPanelBlockEntity {
    public enum Function {
        SCRAM("scram", 0xFFE0352B),
        RESET("reset", 0xFF4CC24A),
        BANK_A_IN("bank_a_in", 0xFFE8E4D6),
        BANK_A_OUT("bank_a_out", 0xFFF2B33A),
        BANK_B_IN("bank_b_in", 0xFFE8E4D6),
        BANK_B_OUT("bank_b_out", 0xFFF2B33A),
        BANK_C_IN("bank_c_in", 0xFFE8E4D6),
        BANK_C_OUT("bank_c_out", 0xFFF2B33A),
        BANK_D_IN("bank_d_in", 0xFFE8E4D6),
        BANK_D_OUT("bank_d_out", 0xFFF2B33A),
        ALL_RODS_IN("all_rods_in", 0xFFE8E4D6),
        TURBINE_TRIP("turbine_trip", 0xFFE0352B),
        TURBINE_RESET("turbine_reset", 0xFF4CC24A),
        BREAKER_OPEN("breaker_open", 0xFFE0352B),
        BREAKER_CLOSE("breaker_close", 0xFF4CC24A),
        ACKNOWLEDGE("acknowledge", 0xFF4AA3E0);

        public final String key;
        public final int color;

        Function(String key, int color) {
            this.key = key;
            this.color = color;
        }

        public String langKey() {
            return "criticalmass.button." + key;
        }
    }

    private static final int PRESS_TICKS = 8;
    private ScrollValueBehaviour function;
    private int pressed;
    private boolean powered;
    private String lastResult = "";

    public ControlButtonBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        function = new ScrollValueBehaviour(Component.translatable("criticalmass.button.function"), this, new FunctionBox());
        function.between(0, Function.values().length - 1);
        function.value = 0;
        function.withFormatter(v -> Component.translatable(Function.values()[Math.floorMod(v, Function.values().length)].langKey()).getString());
        function.withCallback(v -> changed());
        behaviours.add(function);
    }

    public Function function() {
        return Function.values()[function == null ? 0 : Math.floorMod(function.getValue(), Function.values().length)];
    }

    public void setFunction(Function f) {
        function.setValue(f.ordinal());
        changed();
    }

    public boolean pressed() {
        return pressed > 0;
    }

    public String lastResult() {
        return lastResult;
    }

    public void signal(boolean nowPowered) {
        if (nowPowered && !powered) {
            press();
        }
        powered = nowPowered;
    }

    public String press() {
        if (level == null || level.isClientSide) {
            return "";
        }
        pressed = PRESS_TICKS;
        lastResult = act();
        level.playSound(null, worldPosition, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4f, 0.7f);
        changed();
        return lastResult;
    }

    private String act() {
        BlockEntity be = targetEntity();
        if (be == null) {
            return "criticalmass.button.result.unbound";
        }
        Function f = function();
        if (be instanceof ReactorControllerBlockEntity c) {
            switch (f) {
                case SCRAM -> {
                    c.scram();
                    return "criticalmass.button.result.scram";
                }
                case RESET -> {
                    return c.requestReset() ? "criticalmass.button.result.reset" : "criticalmass.button.result.reset_refused";
                }
                case ALL_RODS_IN -> {
                    for (int i = 0; i < CMThermal.ROD_BANKS; i++) {
                        c.setBankTarget(i, 1.0);
                    }
                    return "criticalmass.button.result.rods_in";
                }
                case BANK_A_IN, BANK_A_OUT, BANK_B_IN, BANK_B_OUT, BANK_C_IN, BANK_C_OUT, BANK_D_IN, BANK_D_OUT -> {
                    int index = f.ordinal() - Function.BANK_A_IN.ordinal();
                    c.jogBank(index / 2, index % 2 == 0 ? CMThermal.ROD_JOG : -CMThermal.ROD_JOG);
                    return "criticalmass.button.result.jog";
                }
                default -> {
                }
            }
        }
        if (be instanceof SteamTurbineBlockEntity t) {
            if (f == Function.TURBINE_TRIP) {
                t.manualTrip();
                return "criticalmass.button.result.turbine_trip";
            }
            if (f == Function.TURBINE_RESET) {
                t.resetTrip();
                return "criticalmass.button.result.turbine_reset";
            }
        }
        if (be instanceof GeneratorBlockEntity g) {
            if (f == Function.BREAKER_OPEN || f == Function.BREAKER_CLOSE) {
                g.setBreakerLatched(f == Function.BREAKER_OPEN);
                return f == Function.BREAKER_OPEN ? "criticalmass.button.result.breaker_open" : "criticalmass.button.result.breaker_close";
            }
        }
        if (be instanceof AnnunciatorBlockEntity a && f == Function.ACKNOWLEDGE) {
            a.acknowledge();
            return "criticalmass.button.result.acknowledged";
        }
        return "criticalmass.button.result.wrong_target";
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (pressed > 0 && --pressed == 0) {
            changed();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Pressed", pressed);
        tag.putBoolean("Powered", powered);
        tag.putString("Result", lastResult);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        pressed = tag.getInt("Pressed");
        powered = tag.getBoolean("Powered");
        lastResult = tag.getString("Result");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        header(tooltip, "criticalmass.goggles.button");
        tooltip.add(ThermalTooltips.line("criticalmass.button.does", ChatFormatting.AQUA, Component.translatable(function().langKey())));
        if (!lastResult.isEmpty()) {
            tooltip.add(ThermalTooltips.line(lastResult, ChatFormatting.GRAY));
        }
        tooltip.add(ThermalTooltips.line("criticalmass.button.hint", ChatFormatting.DARK_GRAY));
        return true;
    }

    static class FunctionBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 5, 3.5);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction == state.getValue(HorizontalDirectionalBlock.FACING);
        }
    }
}
