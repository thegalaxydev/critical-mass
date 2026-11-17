package com.criticalmass.content.thermal;

import com.criticalmass.registry.CMSounds;
import com.criticalmass.registry.CMParticles;
import com.criticalmass.registry.CMBlockEntities;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMConfig;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;

public final class Corium {
    private Corium() {
    }

    public static boolean resists(BlockState state, Level level, BlockPos pos) {
        Block b = state.getBlock();
        return b == CMBlocks.SHIELDING_CONCRETE.get() || b == CMBlocks.CORIUM.get() || state.getDestroySpeed(level, pos) < 0 || state.is(Blocks.OBSIDIAN);
    }

    public static class CoriumBlock extends Block implements IBE<Entity_> {
        public static final BooleanProperty HOT = BooleanProperty.create("hot");

        public CoriumBlock(Properties properties) {
            super(properties.lightLevel(s -> s.getValue(HOT) ? 12 : 0));
            registerDefaultState(defaultBlockState().setValue(HOT, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(HOT);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            if (state.getValue(HOT) && entity instanceof LivingEntity && !entity.isSteppingCarefully()) {
                entity.hurt(level.damageSources().hotFloor(), 4.0f);
            }
            super.stepOn(level, pos, state, entity);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
            if (state.getBlock() != newState.getBlock()) {
                level.removeBlockEntity(pos);
            }
        }

        @Override
        public Class<Entity_> getBlockEntityClass() {
            return Entity_.class;
        }

        @Override
        public BlockEntityType<? extends Entity_> getBlockEntityType() {
            return CMBlockEntities.CORIUM.get();
        }
    }

    public static class Entity_ extends SmartBlockEntity implements IHaveGoggleInformation {
        private double heat;
        private double decay;
        private int mass = 1;
        private int depth;
        private int clock;

        public Entity_(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }

        @Override
        public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
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

        public void fill(double heat, double decay, int mass, int depth) {
            this.heat = heat;
            this.decay = decay;
            this.mass = Math.max(1, mass);
            this.depth = depth;
            clock = 0;
            setChanged();
        }

        public double heat() {
            return heat;
        }

        public double decay() {
            return decay;
        }

        public int mass() {
            return mass;
        }

        public int depth() {
            return depth;
        }

        public double temperature() {
            return CMThermal.AMBIENT + heat / (CMThermal.FUEL_HEAT_CAPACITY * mass);
        }

        public double strength() {
            return CMThermal.CORIUM_DOSE * mass + CMThermal.CORIUM_DECAY_DOSE * decay;
        }

        @Override
        public void tick() {
            super.tick();
            if (level == null || level.isClientSide || clock-- > 0) {
                return;
            }
            clock = CMThermal.CORIUM_TICKS - 1;
            heat += decay * CMThermal.CORIUM_TICKS;
            decay *= Math.pow(CMThermal.DECAY_FACTOR, CMThermal.CORIUM_TICKS);
            if (decay < 1e-6) {
                decay = 0;
            }
            boolean wet = false;
            for (Direction d : Direction.values()) {
                if (level.getFluidState(worldPosition.relative(d)).is(Fluids.WATER) || level.getFluidState(worldPosition.relative(d)).is(Fluids.FLOWING_WATER)) {
                    wet = true;
                }
            }
            double t = temperature();
            heat -= heat * Math.min(1, CMThermal.CORIUM_COOLING * CMThermal.CORIUM_TICKS * (wet ? CMThermal.CORIUM_WATER_BONUS : 1));
            if (wet && t > CMThermal.BOILING && level instanceof ServerLevel server) {
                server.sendParticles(CMParticles.STEAM.get(), worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.05);
                server.sendParticles(CMParticles.EMBER.get(), worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0.04);
                level.playSound(null, worldPosition, CMSounds.STEAM_VENT.get(), SoundSource.BLOCKS, 0.7f, 1.2f);
            }
            boolean hot = temperature() > CMThermal.CORIUM_SOLID_TEMPERATURE;
            BlockState state = getBlockState();
            if (state.getValue(CoriumBlock.HOT) != hot) {
                level.setBlock(worldPosition, state.setValue(CoriumBlock.HOT, hot), Block.UPDATE_ALL);
            }
            setChanged();
            sendData();
            if (temperature() > CMThermal.CORIUM_SINK_TEMPERATURE) {
                sink();
            }
        }

        private void sink() {
            BlockPos below = worldPosition.below();
            if (!level.isLoaded(below) || level.isOutsideBuildHeight(below)) {
                return;
            }
            BlockState under = level.getBlockState(below);
            boolean open = under.isAir() || under.canBeReplaced();
            if (!open && (depth >= CMConfig.coriumDepth() || Corium.resists(under, level, below))) {
                return;
            }
            double h = heat;
            double d = decay;
            int m = mass;
            int nextDepth = open ? depth : depth + 1;
            BlockState state = getBlockState().setValue(CoriumBlock.HOT, true);
            level.removeBlock(worldPosition, false);
            level.setBlock(below, state, Block.UPDATE_ALL);
            if (level.getBlockEntity(below) instanceof Entity_ moved) {
                moved.fill(h, d, m, nextDepth);
            }
            level.playSound(null, below, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 0.6f);
        }

        @Override
        protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
            super.write(tag, registries, clientPacket);
            tag.putDouble("Heat", heat);
            tag.putDouble("Decay", decay);
            tag.putInt("Mass", mass);
            tag.putInt("Depth", depth);
        }

        @Override
        protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
            super.read(tag, registries, clientPacket);
            heat = tag.getDouble("Heat");
            decay = tag.getDouble("Decay");
            mass = Math.max(1, tag.getInt("Mass"));
            depth = tag.getInt("Depth");
        }

        @Override
        public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
            ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
            ThermalTooltips.label(tooltip, "criticalmass.goggles.label.temperature");
            ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.celsius", temperature() > CMThermal.CORIUM_SOLID_TEMPERATURE ? ChatFormatting.RED : ChatFormatting.GOLD, ThermalTooltips.number(temperature())));
            return true;
        }
    }
}
