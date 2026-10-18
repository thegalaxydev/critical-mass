package com.criticalmass.registry;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.AlarmSirenBlock;
import com.criticalmass.content.CoolantPortBlock;
import com.criticalmass.content.DockingConnectorBlock;
import com.criticalmass.content.FacingBlock;
import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.content.PipeConnectableBlocks;
import com.criticalmass.content.ReinforcedPipeBlock;
import com.criticalmass.content.ReliefValveBlock;
import com.criticalmass.content.WallPanelBlock;
import com.criticalmass.content.kinetics.CirculationPumpBlock;
import com.criticalmass.content.kinetics.ControlRodDriveBlock;
import com.criticalmass.content.kinetics.SteamTurbineBlock;
import com.criticalmass.content.thermal.ReactorControllerBlock;
import java.util.function.Function;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CMBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CriticalMass.MODID);

    public static final DeferredBlock<Block> REACTOR_VESSEL_CASING = register("reactor_vessel_casing", Block::new, metal(5.0f));
    public static final DeferredBlock<ReactorControllerBlock> REACTOR_CONTROLLER = register("reactor_controller", ReactorControllerBlock::new, metal(5.0f));
    public static final DeferredBlock<RotatedPillarBlock> FUEL_CHANNEL = register("fuel_channel", RotatedPillarBlock::new, metal(4.0f));
    public static final DeferredBlock<RotatedPillarBlock> CONTROL_CHANNEL = register("control_channel", RotatedPillarBlock::new, metal(4.0f));
    public static final DeferredBlock<ControlRodDriveBlock> CONTROL_ROD_DRIVE = register("control_rod_drive", ControlRodDriveBlock::new, metal(3.5f).noOcclusion());
    public static final DeferredBlock<RotatedPillarBlock> COOLANT_CHANNEL = register("coolant_channel", RotatedPillarBlock::new, metal(4.0f));
    public static final DeferredBlock<FacingBlock> REACTOR_SERVICE_PORT = register("reactor_service_port", FacingBlock::new, metal(5.0f));
    public static final DeferredBlock<CoolantPortBlock> PRIMARY_COOLANT_PORT = register("primary_coolant_port", CoolantPortBlock::new, metal(5.0f));
    public static final DeferredBlock<ReinforcedPipeBlock> REINFORCED_PIPE = register("reinforced_pipe", ReinforcedPipeBlock::new, metal(3.0f).noOcclusion());
    public static final DeferredBlock<CirculationPumpBlock> CIRCULATION_PUMP = register("circulation_pump", CirculationPumpBlock::new, metal(3.5f).noOcclusion());
    public static final DeferredBlock<PipeConnectableBlocks.Horizontal> HEAT_EXCHANGER = register("heat_exchanger", PipeConnectableBlocks.Horizontal::new, metal(4.0f));
    public static final DeferredBlock<SteamTurbineBlock> STEAM_TURBINE = register("steam_turbine", SteamTurbineBlock::new, metal(4.0f).noOcclusion());
    public static final DeferredBlock<PipeConnectableBlocks.Horizontal> CONDENSER = register("condenser", PipeConnectableBlocks.Horizontal::new, metal(4.0f));
    public static final DeferredBlock<Block> COOLING_TOWER_PACKING = register("cooling_tower_packing", Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<HorizontalBlock> RADIATOR_PANEL = register("radiator_panel", p -> new HorizontalBlock(p, Block.box(0, 0, 12, 16, 16, 16), true), metal(2.5f).noOcclusion());
    public static final DeferredBlock<PipeConnectableBlocks.Simple> EMERGENCY_COOLANT_TANK = register("emergency_coolant_tank", PipeConnectableBlocks.Simple::new, metal(4.0f));
    public static final DeferredBlock<ReliefValveBlock> RELIEF_VALVE = register("relief_valve", ReliefValveBlock::new, metal(3.0f).noOcclusion());
    public static final DeferredBlock<PipeConnectableBlocks.Horizontal> STEAM_BYPASS_VALVE = register("steam_bypass_valve", p -> new PipeConnectableBlocks.Horizontal(p, Shapes.or(Block.box(3, 3, 3, 13, 13, 13), Block.box(0, 4, 4, 16, 12, 12), Block.box(4, 4, 0, 12, 12, 3), Block.box(4, 13, 4, 12, 16, 12))), metal(3.0f).noOcclusion());
    public static final DeferredBlock<PipeConnectableBlocks.Horizontal> BOILER_HEATING_PLATE = register("boiler_heating_plate", p -> new PipeConnectableBlocks.Horizontal(p, Block.box(0, 0, 0, 16, 6, 16)), metal(3.0f).noOcclusion());
    public static final DeferredBlock<Block> SHIELDING_CONCRETE = register("shielding_concrete", Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(6.0f, 12.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<TransparentBlock> SHIELDED_GLASS = register("shielded_glass", TransparentBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(2.5f, 8.0f).sound(SoundType.GLASS).noOcclusion().isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false));
    public static final DeferredBlock<TrapDoorBlock> SHIELDED_HATCH = register("shielded_hatch", p -> new TrapDoorBlock(BlockSetType.IRON, p), metal(6.0f).noOcclusion());
    public static final DeferredBlock<HorizontalBlock> FUEL_ASSEMBLY_BENCH = register("fuel_assembly_bench", p -> new HorizontalBlock(p, Shapes.or(Block.box(0, 10, 0, 16, 16, 16), Block.box(1, 0, 1, 15, 10, 15)), false), metal(3.0f).noOcclusion());
    public static final DeferredBlock<Block> SPENT_FUEL_RACK = register("spent_fuel_rack", Block::new, metal(4.0f).noOcclusion());
    public static final DeferredBlock<Block> SHIELDED_CASK = register("shielded_cask", ShieldedCaskShape::new, metal(6.0f).noOcclusion());
    public static final DeferredBlock<WallPanelBlock> INSTRUMENTATION_PANEL = register("instrumentation_panel", WallPanelBlock::new, metal(2.0f).noOcclusion());
    public static final DeferredBlock<HorizontalBlock> REACTOR_SIGNAL_INTERFACE = register("reactor_signal_interface", HorizontalBlock::new, metal(4.0f));
    public static final DeferredBlock<AlarmSirenBlock> ALARM_SIREN = register("alarm_siren", AlarmSirenBlock::new, metal(2.0f).noOcclusion());
    public static final DeferredBlock<DockingConnectorBlock> DOCKING_SERVICE_CONNECTOR = register("docking_service_connector", DockingConnectorBlock::new, metal(4.0f));

    private CMBlocks() {
    }

    static BlockBehaviour.Properties metal(float strength) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(strength, strength * 1.5f).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops();
    }

    static <B extends Block> DeferredBlock<B> register(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties) {
        DeferredBlock<B> block = BLOCKS.register(name, () -> factory.apply(properties));
        CMItems.blockItem(name, block);
        return block;
    }

    static final class ShieldedCaskShape extends Block {
        private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 13, 14), Block.box(1, 13, 1, 15, 16, 15));

        ShieldedCaskShape(Properties properties) {
            super(properties);
        }

        @Override
        protected net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
            return SHAPE;
        }
    }
}
