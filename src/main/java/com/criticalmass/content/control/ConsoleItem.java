package com.criticalmass.content.control;

import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ConsoleItem extends BlockItem {
    public ConsoleItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        if (result == InteractionResult.FAIL && context.getLevel().isClientSide) {
            showBounds(context);
        }
        return result;
    }

    @OnlyIn(Dist.CLIENT)
    private void showBounds(BlockPlaceContext context) {
        if (!(context.getPlayer() instanceof LocalPlayer player)) {
            return;
        }
        BlockPos master = context.getClickedPos();
        Direction facing = context.getHorizontalDirection().getOpposite();
        AABB box = null;
        for (BlockPos p : ConsoleBlock.positions(master, facing)) {
            box = box == null ? new AABB(p) : box.minmax(new AABB(p));
        }
        Outliner.getInstance().showAABB("criticalmass_console_" + master.asLong(), box).colored(0xFF5D6C).lineWidth(1 / 16f);
        player.displayClientMessage(Component.translatable("criticalmass.console.no_space").withStyle(s -> s.withColor(0xFF5D6C)), true);
    }
}
