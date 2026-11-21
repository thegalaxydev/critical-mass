package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.FluidBucketItem;
import com.criticalmass.content.thermal.TankCoolant;
import com.criticalmass.registry.CMFluids;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class BucketTests {
    private static final String EMPTY = "empty";
    private static final BlockPos TANK = new BlockPos(1, 2, 1);
    private static final BlockPos FLOOR = new BlockPos(3, 2, 3);

    private BucketTests() {
    }

    private record Kind(CMFluids.Entry fluid, Item bucket) {
    }

    @GameTest(template = EMPTY)
    public static void everyLiquidHasABucketThatFillsTanksAndIsNotPouredOut(GameTestHelper h) {
        List<Kind> kinds = List.of(new Kind(CMFluids.PRIMARY_COOLANT, CMItems.PRIMARY_COOLANT_BUCKET.get()), new Kind(CMFluids.CONTAMINATED_COOLANT, CMItems.CONTAMINATED_COOLANT_BUCKET.get()),
                new Kind(CMFluids.BORIC_ACID, CMItems.BORIC_ACID_BUCKET.get()), new Kind(CMFluids.FUEL_OIL, CMItems.FUEL_OIL_BUCKET.get()));
        h.setBlock(FLOOR, Blocks.SHORT_GRASS);
        h.startSequence()
                .thenExecute(() -> h.setBlock(TANK, AllBlocks.FLUID_TANK.getDefaultState()))
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertTrue(CMFluids.STEAM.source.get().getBucket() == Items.AIR, "Steam is a gas and should have no bucket");
                    Player player = h.makeMockPlayer(GameType.SURVIVAL);
                    for (Kind kind : kinds) {
                        String name = kind.fluid().name;
                        FluidStack full = new FluidStack(kind.fluid().source.get(), 1000);
                        h.assertTrue(kind.fluid().source.get().getBucket() == kind.bucket() && FluidUtil.getFilledBucket(full).is(kind.bucket()), "A bucket of " + name + " should be its own bucket");
                        ItemStack held = new ItemStack(kind.bucket());
                        IFluidHandlerItem inside = held.getCapability(Capabilities.FluidHandler.ITEM);
                        h.assertTrue(inside != null && FluidStack.isSameFluid(inside.getFluidInTank(0), full) && inside.getFluidInTank(0).getAmount() == 1000, "The bucket should hold a bucket of " + name);

                        player.setItemInHand(InteractionHand.MAIN_HAND, held);
                        h.assertTrue(held.use(h.getLevel(), player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.PASS && player.getMainHandItem().is(kind.bucket()),
                                "Using a bucket of " + name + " in the open should do nothing");
                        h.assertTrue(!((FluidBucketItem) kind.bucket()).emptyContents(player, h.getLevel(), h.absolutePos(FLOOR), null, held), "A bucket of " + name + " should not pour onto the ground");
                        h.assertBlockPresent(Blocks.SHORT_GRASS, FLOOR);

                        IFluidHandler tank = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(TANK), Direction.NORTH);
                        FluidActionResult poured = FluidUtil.tryEmptyContainer(held, tank, 1000, null, true);
                        FluidTankBlockEntity be = h.getBlockEntity(TANK);
                        h.assertTrue(poured.isSuccess() && poured.getResult().is(Items.BUCKET) && FluidStack.isSameFluid(be.getTankInventory().getFluid(), full) && be.getTankInventory().getFluidAmount() == 1000,
                                "A bucket of " + name + " should empty into a Fluid Tank and leave a bucket: " + be.getTankInventory().getFluid());
                        if (kind.fluid() == CMFluids.CONTAMINATED_COOLANT) {
                            double share = TankCoolant.at(h.getLevel(), h.absolutePos(TANK)).coolant().activityShare();
                            h.assertTrue(share > 0.999, "A bucket keeps no record of how contaminated its coolant is, so it should count as fully contaminated: " + share);
                        }
                        FluidActionResult drawn = FluidUtil.tryFillContainer(new ItemStack(Items.BUCKET), tank, 1000, null, true);
                        h.assertTrue(drawn.isSuccess() && drawn.getResult().is(kind.bucket()) && be.getTankInventory().isEmpty(), "An empty bucket should draw " + name + " back out of the tank: " + drawn.getResult());
                    }
                })
                .thenSucceed();
    }
}
