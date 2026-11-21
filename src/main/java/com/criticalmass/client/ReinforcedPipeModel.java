package com.criticalmass.client;

import com.criticalmass.content.ReinforcedPipeBlock;
import com.criticalmass.content.ReinforcedStraightPipeBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.model.BakedModelWrapperWithData;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public class ReinforcedPipeModel extends BakedModelWrapperWithData {
    private static final ModelProperty<Integer> DRAINS = new ModelProperty<>();
    private static final ModelProperty<Integer> RIMS = new ModelProperty<>();
    private static final ModelProperty<BlockState> BRACKET = new ModelProperty<>();

    public ReinforcedPipeModel(BakedModel template) {
        super(template);
    }

    @Override
    protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData blockEntityData) {
        int drains = 0;
        int rims = 0;
        for (Direction d : Direction.values()) {
            if (!rimmed(world, pos, state, d)) {
                continue;
            }
            rims |= 1 << d.get3DDataValue();
            if (drains(world, pos, d)) {
                drains |= 1 << d.get3DDataValue();
            }
        }
        BracketedBlockEntityBehaviour bracket = BlockEntityBehaviour.get(world, pos, BracketedBlockEntityBehaviour.TYPE);
        if (bracket != null && bracket.isBracketPresent()) {
            builder.with(BRACKET, bracket.getBracket());
        }
        return builder.with(DRAINS, drains).with(RIMS, rims);
    }

    private static boolean drains(BlockAndTintGetter world, BlockPos pos, Direction d) {
        BlockPos other = pos.relative(d);
        BlockState facing = world.getBlockState(other);
        if (facing.getBlock() instanceof PumpBlock || facing.getBlock() instanceof EncasedPipeBlock || AllBlocks.HOSE_PULLEY.has(facing)) {
            return false;
        }
        return FluidPropagator.hasFluidCapability(world, other, d.getOpposite());
    }

    private static boolean ours(BlockState state) {
        return state != null && (state.getBlock() instanceof ReinforcedPipeBlock || state.getBlock() instanceof ReinforcedStraightPipeBlock);
    }

    private static boolean rimmed(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction d) {
        if (state.getBlock() instanceof ReinforcedStraightPipeBlock) {
            Direction.Axis axis = state.getValue(ReinforcedStraightPipeBlock.AXIS);
            if (d.getAxis() != axis) {
                return false;
            }
            BlockState next = world.getBlockState(pos.relative(d));
            if (next.getBlock() instanceof ReinforcedStraightPipeBlock && next.getValue(ReinforcedStraightPipeBlock.AXIS) == axis) {
                return d.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            }
            return !(next.getBlock() instanceof FluidPipeBlock && FluidPipeBlock.isOpenAt(next, d.getOpposite()));
        }
        return state.getBlock() instanceof ReinforcedPipeBlock && state.getValue(ReinforcedPipeBlock.RIM.get(d));
    }

    private static PartialModel plate(BlockState state, ModelData data, Direction d) {
        Integer rims = data.get(RIMS);
        if (rims == null || (rims >> d.get3DDataValue() & 1) == 0) {
            return null;
        }
        Integer drains = data.get(DRAINS);
        boolean drain = drains != null && (drains >> d.get3DDataValue() & 1) != 0;
        return (drain ? CMPartialModels.REINFORCED_PIPE_DRAIN : CMPartialModels.REINFORCED_PIPE_RIM).get(d);
    }

    private static List<BakedModel> parts(BlockState state, ModelData data) {
        List<BakedModel> parts = new ArrayList<>();
        BlockState bracket = data.get(BRACKET);
        if (bracket != null) {
            parts.add(Minecraft.getInstance().getBlockRenderer().getBlockModel(bracket));
        }
        for (Direction d : Direction.values()) {
            PartialModel plate = plate(state, data, d);
            if (plate != null) {
                parts.add(plate.get());
            }
        }
        return parts;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        ChunkRenderTypeSet base = super.getRenderTypes(state, rand, data);
        if (!ours(state)) {
            return base;
        }
        List<BakedModel> parts = parts(state, data);
        if (parts.isEmpty()) {
            return base;
        }
        List<ChunkRenderTypeSet> sets = new ArrayList<>();
        sets.add(base);
        for (BakedModel part : parts) {
            sets.add(part.getRenderTypes(state, rand, data));
        }
        return ChunkRenderTypeSet.union(sets);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        if (!ours(state)) {
            return quads;
        }
        List<BakedModel> parts = parts(state, data);
        if (parts.isEmpty()) {
            return quads;
        }
        List<BakedQuad> all = new ArrayList<>(quads);
        for (BakedModel part : parts) {
            all.addAll(part.getQuads(state, side, rand, data, renderType));
        }
        return all;
    }
}
