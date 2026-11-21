package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.FluidBucketItem;
import com.criticalmass.content.radiation.ContaminationData;
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
import net.minecraft.world.InteractionResultHolder;
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
    public static void aDispenserSpillsABucketOrPoursItIntoATank(GameTestHelper h) {
        BlockPos open = new BlockPos(1, 2, 1);
        BlockPos fed = new BlockPos(1, 2, 4);
        BlockPos into = fed.east();
        h.setBlock(into, AllBlocks.FLUID_TANK.getDefaultState());
        for (BlockPos at : List.of(open, fed)) {
            h.setBlock(at, Blocks.DISPENSER.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.FACING, Direction.EAST));
            net.minecraft.world.level.block.entity.DispenserBlockEntity box = h.getBlockEntity(at);
            box.setItem(0, new ItemStack(CMItems.CONTAMINATED_COOLANT_BUCKET.get()));
        }
        ContaminationData floor = ContaminationData.get(h.getLevel());
        floor.clear(h.absolutePos(open.east()));
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    h.setBlock(open.above(), Blocks.REDSTONE_BLOCK);
                    h.setBlock(fed.above(), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    net.minecraft.world.level.block.entity.DispenserBlockEntity spilled = h.getBlockEntity(open);
                    h.assertTrue(spilled.getItem(0).is(Items.BUCKET), "A dispenser should be left with the empty bucket: " + spilled.getItem(0));
                    h.assertTrue(Math.abs(floor.clear(h.absolutePos(open.east())) - ((FluidBucketItem) CMItems.CONTAMINATED_COOLANT_BUCKET.get()).spillDirt()) < 1e-6, "A dispensed bucket should spill in front of the dispenser");
                    h.assertBlockPresent(Blocks.AIR, open.east());
                    net.minecraft.world.level.block.entity.DispenserBlockEntity poured = h.getBlockEntity(fed);
                    FluidTankBlockEntity tank = h.getBlockEntity(into);
                    h.assertTrue(poured.getItem(0).is(Items.BUCKET) && tank.getTankInventory().getFluidAmount() == 1000, "A dispenser facing a tank should pour the bucket into it: " + tank.getTankInventory().getFluid());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY)
    public static void charredSignsTakeText(GameTestHelper h) {
        BlockPos post = new BlockPos(1, 2, 1);
        BlockPos hung = new BlockPos(3, 2, 1);
        h.setBlock(post.below(), Blocks.STONE);
        h.setBlock(post, com.criticalmass.registry.CMBlocks.CHARRED_SIGN.get());
        h.setBlock(hung.above(), Blocks.STONE);
        h.setBlock(hung, com.criticalmass.registry.CMBlocks.CHARRED_HANGING_SIGN.get());
        h.succeedWhen(() -> {
            h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(post)) instanceof net.minecraft.world.level.block.entity.SignBlockEntity, "A Charred Sign should be a sign");
            h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(hung)) instanceof net.minecraft.world.level.block.entity.HangingSignBlockEntity, "A Charred Hanging Sign should be a hanging sign");
        });
    }

    @GameTest(template = EMPTY)
    public static void everyLiquidHasABucketThatFillsTanksAndSpillsOnTheGround(GameTestHelper h) {
        List<Kind> kinds = List.of(new Kind(CMFluids.PRIMARY_COOLANT, CMItems.PRIMARY_COOLANT_BUCKET.get()), new Kind(CMFluids.CONTAMINATED_COOLANT, CMItems.CONTAMINATED_COOLANT_BUCKET.get()),
                new Kind(CMFluids.BORIC_ACID, CMItems.BORIC_ACID_BUCKET.get()), new Kind(CMFluids.FUEL_OIL, CMItems.FUEL_OIL_BUCKET.get()));
        h.setBlock(FLOOR.below(), Blocks.STONE);
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

                        BlockPos tankTop = h.absolutePos(TANK).above();
                        player.moveTo(tankTop.getX() + 0.5, tankTop.getY(), tankTop.getZ() + 0.5, 0, 90);
                        player.setItemInHand(InteractionHand.MAIN_HAND, held);
                        h.assertTrue(held.use(h.getLevel(), player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.PASS && player.getMainHandItem().is(kind.bucket()),
                                "A bucket of " + name + " used on a tank that will not take it by hand should stay full");
                        BlockPos spot = h.absolutePos(FLOOR);
                        ContaminationData floor = ContaminationData.get(h.getLevel());
                        floor.clear(spot);
                        ItemStack spare = new ItemStack(kind.bucket());
                        player.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0, 90);
                        player.setItemInHand(InteractionHand.MAIN_HAND, spare);
                        InteractionResultHolder<ItemStack> spilt = spare.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
                        h.assertTrue(spilt.getResult().consumesAction() && spilt.getObject().is(Items.BUCKET), "A bucket of " + name + " emptied on the ground should come back empty: " + spilt.getObject());
                        double dirt = ((FluidBucketItem) kind.bucket()).spillDirt();
                        boolean coolant = kind.fluid() == CMFluids.PRIMARY_COOLANT || kind.fluid() == CMFluids.CONTAMINATED_COOLANT;
                        h.assertTrue(coolant == dirt > 0 && Math.abs(floor.get(spot) - dirt) < 1e-6, "Spilled " + name + " should dirty the ground as a leak of it does: " + floor.get(spot) + " for " + dirt);
                        floor.clear(spot);
                        h.assertBlockPresent(Blocks.AIR, FLOOR);
                        h.assertTrue(!((FluidBucketItem) kind.bucket()).emptyContents(player, h.getLevel(), spot, null, held), "A bucket of " + name + " should leave no liquid block");

                        IFluidHandler tank = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(TANK), Direction.NORTH);
                        FluidActionResult poured = FluidUtil.tryEmptyContainer(held, tank, 1000, null, true);
                        FluidTankBlockEntity be = h.getBlockEntity(TANK);
                        h.assertTrue(poured.isSuccess() && poured.getResult().is(Items.BUCKET) && FluidStack.isSameFluid(be.getTankInventory().getFluid(), full) && be.getTankInventory().getFluidAmount() == 1000,
                                "A bucket of " + name + " should empty into a Fluid Tank and leave a bucket: " + be.getTankInventory().getFluid());
                        if (kind.fluid() == CMFluids.CONTAMINATED_COOLANT) {
                            double share = TankCoolant.at(h.getLevel(), h.absolutePos(TANK)).coolant().activityShare();
                            h.assertTrue(share > 0.999, "A bucket keeps no record of how contaminated its coolant is, so it should count as fully contaminated: " + share);
                            com.criticalmass.content.control.PlantChannel.Reading activity = com.criticalmass.content.control.PlantChannel.ACTIVITY.read(h.getLevel(), h.absolutePos(TANK));
                            com.criticalmass.content.control.PlantChannel.Reading record = com.criticalmass.content.control.PlantChannel.RECORD.read(h.getLevel(), h.absolutePos(TANK));
                            h.assertTrue(activity != null && activity.value() > 99.9 && record != null && record.value() == 0, "A gauge should read the tank as contaminated with none of it on the reactor's record: " + activity + ", " + record);
                            TankCoolant.at(h.getLevel(), h.absolutePos(TANK)).coolant().contaminate();
                            record = com.criticalmass.content.control.PlantChannel.RECORD.read(h.getLevel(), h.absolutePos(TANK));
                            h.assertTrue(record != null && record.value() > 99.9, "Coolant the reactor contaminated should read as on its record: " + record);
                            double held1 = com.criticalmass.content.radiation.Radiation.itemStrength(new ItemStack(kind.bucket()));
                            h.assertTrue(Math.abs(held1 - 1000 * com.criticalmass.content.thermal.SteamActivity.DOSE) < 1e-9, "A bucket of Contaminated Coolant should give the dose a tank of as much gives: " + held1);
                        } else {
                            h.assertTrue(com.criticalmass.content.radiation.Radiation.itemStrength(new ItemStack(kind.bucket())) == 0, "A bucket of " + name + " should give no dose");
                        }
                        h.assertTrue(com.simibubi.create.content.fluids.transfer.GenericItemFilling.getRequiredAmountForItem(h.getLevel(), new ItemStack(Items.BUCKET), full.copy()) == 1000
                                && com.simibubi.create.content.fluids.transfer.GenericItemFilling.fillItem(h.getLevel(), 1000, new ItemStack(Items.BUCKET), full.copy()).is(kind.bucket()),
                                "A Spout should fill a bucket with " + name);
                        ItemStack onDrain = new ItemStack(kind.bucket());
                        h.assertTrue(com.simibubi.create.content.fluids.transfer.GenericItemEmptying.canItemBeEmptied(h.getLevel(), onDrain), "An Item Drain should take a bucket of " + name);
                        net.createmod.catnip.data.Pair<FluidStack, ItemStack> drained = com.simibubi.create.content.fluids.transfer.GenericItemEmptying.emptyItem(h.getLevel(), onDrain, false);
                        h.assertTrue(FluidStack.isSameFluid(drained.getFirst(), full) && drained.getFirst().getAmount() == 1000 && drained.getSecond().is(Items.BUCKET),
                                "An Item Drain should empty a bucket of " + name + ": " + drained.getFirst() + ", " + drained.getSecond());
                        FluidActionResult drawn = FluidUtil.tryFillContainer(new ItemStack(Items.BUCKET), tank, 1000, null, true);
                        h.assertTrue(drawn.isSuccess() && drawn.getResult().is(kind.bucket()) && be.getTankInventory().isEmpty(), "An empty bucket should draw " + name + " back out of the tank: " + drawn.getResult());
                    }
                })
                .thenSucceed();
    }
}
