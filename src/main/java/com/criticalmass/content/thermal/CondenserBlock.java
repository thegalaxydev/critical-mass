package com.criticalmass.content.thermal;

import com.criticalmass.registry.CMBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class CondenserBlock extends HeatExchangerBlock {
    public CondenserBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends HeatExchangerBlockEntity> getBlockEntityType() {
        return CMBlockEntities.CONDENSER.get();
    }
}
