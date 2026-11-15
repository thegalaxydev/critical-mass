package com.criticalmass.content.kinetics;

import com.criticalmass.client.TurbinePlacementOutline;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

public class SteamTurbineItem extends BlockItem {
    public SteamTurbineItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        if (result != InteractionResult.FAIL) {
            return result;
        }
        Direction face = context.getClickedFace();
        BlockPlaceContext further = BlockPlaceContext.at(context, context.getClickedPos().relative(face), face);
        result = super.place(further);
        if (result == InteractionResult.FAIL && context.getLevel().isClientSide()) {
            TurbinePlacementOutline.show(further.getClickedPos());
        }
        return result;
    }
}
