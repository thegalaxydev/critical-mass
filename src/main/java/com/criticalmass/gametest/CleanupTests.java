package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.control.PlantAlarms;
import com.criticalmass.content.control.PlantChannel;
import com.criticalmass.content.fuel.ShieldedCaskBlockEntity;
import com.criticalmass.content.radiation.ContaminationData;
import com.criticalmass.content.radiation.Radiation;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantBuffer;
import com.criticalmass.content.thermal.DemineralizerBlock;
import com.criticalmass.content.thermal.DemineralizerBlockEntity;
import com.criticalmass.content.thermal.ResinCartridgeItem;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMFluids;
import com.criticalmass.registry.CMItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class CleanupTests {
    private static final String EMPTY = "empty";
    private static final BlockPos AT = new BlockPos(2, 1, 2);

    private CleanupTests() {
    }

    private static FluidStack dirty(int amount) {
        return new FluidStack(CMFluids.CONTAMINATED_COOLANT.source.get(), amount);
    }

    private static CoolantBuffer buffer(int capacity) {
        return new CoolantBuffer(capacity, () -> {
        });
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-6 * Math.max(1, Math.abs(b));
    }

    @GameTest(template = EMPTY)
    public static void coolantCarriesItsContaminationThroughSplitsAndMerges(GameTestHelper h) {
        CoolantBuffer a = buffer(100000);
        a.set(dirty(1000), 0);
        h.assertTrue(near(a.activityShare(), 1) && near(a.activityLoad(), 1000), "Contaminated Coolant should be wholly contaminated: " + a.activityShare());
        a.give(new CoolantBuffer.Packet(CoolantLoopTests.coolant(1000), 0));
        h.assertTrue(near(a.activityShare(), 0.5) && CMThermal.isContaminated(a.fluid()), "Clean coolant should dilute it and leave it contaminated: " + a.activityShare());
        CoolantBuffer.Packet out = a.take(500);
        h.assertTrue(near(out.activity(), 250) && near(a.activityShare(), 0.5) && CMThermal.isContaminated(out.fluid()), "A split should keep the share: " + out.activity());
        CoolantBuffer b = buffer(100000);
        b.set(CoolantLoopTests.coolant(9900), 0);
        b.give(new CoolantBuffer.Packet(dirty(100), 0));
        h.assertTrue(b.amount() == 10000 && near(b.activityLoad(), 100) && !CMThermal.isContaminated(b.fluid()) && CMThermal.isPrimaryMedium(b.fluid()),
                "At one part in a hundred the coolant should read as Primary Coolant and keep its load: " + b.fluid() + " " + b.activityLoad());
        b.give(new CoolantBuffer.Packet(dirty(100), 0));
        h.assertTrue(CMThermal.isContaminated(b.fluid()) && near(b.activityLoad(), 200), "Over one part in a hundred it should be Contaminated Coolant again: " + b.activityShare());
        CompoundTag saved = b.save(h.getLevel().registryAccess());
        CoolantBuffer copy = buffer(100000);
        copy.load(saved, h.getLevel().registryAccess());
        h.assertTrue(near(copy.activityLoad(), 200) && CMThermal.isContaminated(copy.fluid()), "The load should survive save and load: " + copy.activityLoad());
        saved.remove("Activity");
        copy.load(saved, h.getLevel().registryAccess());
        h.assertTrue(near(copy.activityLoad(), copy.amount()), "Contaminated Coolant saved before it carried a load should load as wholly contaminated: " + copy.activityLoad());
        b.contaminate();
        h.assertTrue(near(b.activityShare(), 1), "A fuel failure should contaminate the coolant wholly: " + b.activityShare());
        CoolantBuffer from = buffer(8000);
        CoolantBuffer to = buffer(8000);
        from.set(dirty(1000), 5000);
        from.give(new CoolantBuffer.Packet(CoolantLoopTests.coolant(1000), 0));
        BlockPos abs = h.absolutePos(AT);
        FluidStack moved = from.handler(h.getLevel(), abs, Direction.NORTH).drain(400, IFluidHandler.FluidAction.EXECUTE);
        to.handler(h.getLevel(), abs, Direction.SOUTH).fill(moved, IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(near(to.activityLoad(), 200) && near(to.heat(), 1000) && near(from.activityLoad(), 800), "Coolant piped between two machines should carry its share: " + to.activityLoad());
        CoolantBuffer tank = buffer(8000);
        tank.handler(null, null, null).fill(dirty(300), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(near(tank.activityLoad(), 300), "Contaminated Coolant from a plain tank should arrive wholly contaminated: " + tank.activityLoad());
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void aDemineralizerCleansCoolantIntoItsResin(GameTestHelper h) {
        h.setBlock(AT, CMBlocks.DEMINERALIZER.get());
        DemineralizerBlockEntity be = h.getBlockEntity(AT);
        BlockPos abs = h.absolutePos(AT);
        IFluidHandler flow = be.flow(Direction.NORTH);
        h.assertTrue(be.flow(Direction.UP) == null && be.flow(Direction.DOWN) != null, "Pipes should join every face but the top");
        h.assertTrue(flow.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE) == 0, "Water should be refused");
        h.assertTrue(flow.fill(CoolantLoopTests.coolant(100), IFluidHandler.FluidAction.EXECUTE) == 100, "Clean coolant should be taken");
        h.startSequence()
                .thenIdle(15)
                .thenExecute(() -> {
                    h.assertTrue(be.outlet().amount() == 100 && be.inlet().amount() == 0 && !be.blocked(), "Clean coolant should pass with no cartridge: " + be.outlet().amount());
                    h.assertTrue(flow.drain(100, IFluidHandler.FluidAction.EXECUTE).getAmount() == 100, "A pipe should draw off what has passed");
                    be.inlet().give(new CoolantBuffer.Packet(dirty(600), 6000, 600 * 500.0, 600));
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(be.inlet().amount() == 600 && be.outlet().amount() == 0 && be.blocked() && be.status().equals(DemineralizerBlockEntity.NO_RESIN) && PlantAlarms.trouble(be),
                            "Contaminated Coolant should wait with no cartridge: " + be.outlet().amount());
                    h.assertTrue(be.analog() == 0 && h.getBlockState(AT).getValue(DemineralizerBlock.RESIN) == DemineralizerBlock.Resin.NONE, "An empty Demineralizer should read 0");
                    ItemStack fresh = new ItemStack(CMItems.RESIN_CARTRIDGE.get(), 3);
                    h.assertTrue(be.items().insertItem(0, fresh, false).getCount() == 2 && be.loaded(), "A funnel should fit one cartridge");
                    h.assertTrue(be.items().insertItem(0, fresh, true).getCount() == 3 && DemineralizerBlockEntity.OCCUPIED.equals(be.refuseReason(fresh)), "A second cartridge should be refused");
                })
                .thenIdle(2)
                .thenExecute(() -> h.assertTrue(be.status().equals(DemineralizerBlockEntity.CLEANING) && be.analog() == 15 && h.getBlockState(AT).getValue(DemineralizerBlock.RESIN) != DemineralizerBlock.Resin.NONE,
                        "With resin it should start cleaning: " + be.status()))
                .thenIdle(600 / DemineralizerBlockEntity.RATE + 10)
                .thenExecute(() -> {
                    h.assertTrue(be.inlet().amount() == 0 && be.outlet().amount() == 600 && !CMThermal.isContaminated(be.outlet().fluid()) && be.outlet().activityLoad() == 0,
                            "All of it should come out as clean Primary Coolant: " + be.outlet().fluid());
                    h.assertTrue(near(be.outlet().heat(), 6000) && near(be.outlet().boronLoad(), 600 * 500.0), "Its heat and its boron should pass: " + be.outlet().heat() + " " + be.outlet().boronLoad());
                    h.assertTrue(near(be.load(), 600 / DemineralizerBlockEntity.RESIN_CAPACITY) && be.cartridge().is(CMItems.USED_RESIN_CARTRIDGE.get()), "The resin should hold what was taken out: " + be.load());
                    h.assertTrue(h.getBlockState(AT).getValue(DemineralizerBlock.RESIN) == DemineralizerBlock.Resin.USED && be.status().equals(DemineralizerBlockEntity.READY) && be.analog() == 13,
                            "It should show a used cartridge and read what is left: " + be.analog());
                    h.assertTrue(near(Radiation.itemStrength(be.cartridge()), Radiation.RESIN_DOSE * be.load()), "A used cartridge should radiate with its load: " + Radiation.itemStrength(be.cartridge()));
                    PlantChannel.Reading left = PlantChannel.LEVEL.read(h.getLevel(), abs);
                    h.assertTrue(left != null && near(left.value(), 85) && !PlantAlarms.trouble(be), "A gauge should read the resin left: " + left);
                    h.assertTrue(be.items().extractItem(0, 1, false).isEmpty(), "A funnel should leave a cartridge that still has resin");
                    h.assertTrue(flow.drain(600, IFluidHandler.FluidAction.EXECUTE).is(CMFluids.PRIMARY_COOLANT.source.get()), "A pipe should draw off Primary Coolant");
                    ItemStack partly = be.take();
                    h.assertTrue(!be.loaded() && near(ResinCartridgeItem.load(partly), 0.15), "A hand should lift the cartridge out with its load");
                    h.assertTrue(be.insert(ResinCartridgeItem.loaded(0.999)), "A partly used cartridge should go back in");
                    be.inlet().give(new CoolantBuffer.Packet(dirty(100), 0));
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(be.load() >= 1 && be.status().equals(DemineralizerBlockEntity.SPENT) && be.blocked() && PlantAlarms.trouble(be), "A full cartridge should stop the line: " + be.load());
                    h.assertTrue(be.inlet().amount() >= 95 && be.outlet().amount() <= 5 && be.analog() == 0 && h.getBlockState(AT).getValue(DemineralizerBlock.RESIN) == DemineralizerBlock.Resin.SPENT,
                            "What it could not clean should wait: " + be.inlet().amount());
                    ItemStack spent = be.items().extractItem(0, 1, false);
                    h.assertTrue(spent.is(CMItems.USED_RESIN_CARTRIDGE.get()) && ResinCartridgeItem.load(spent) >= 1 && !be.loaded(), "A funnel should take a spent cartridge");
                    h.assertTrue(near(Radiation.itemStrength(spent), Radiation.RESIN_DOSE) && ShieldedCaskBlockEntity.refuseReason(spent) == null, "A spent cartridge should give its full dose and fit a Shielded Cask");
                    h.assertTrue(DemineralizerBlockEntity.SPENT_CARTRIDGE.equals(be.refuseReason(spent)) && be.items().insertItem(0, spent, true).getCount() == 1, "A spent cartridge should not go back in");
                    h.assertTrue(be.insert(ResinCartridgeItem.loaded(0.5)), "A part used cartridge should fit again");
                    h.setBlock(AT, Blocks.AIR);
                    h.assertTrue(ContaminationData.get(h.getLevel()).get(abs) > 0, "Breaking it should spill what it holds");
                    h.assertTrue(!h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(2), e -> e.getItem().is(CMItems.USED_RESIN_CARTRIDGE.get())).isEmpty(), "Breaking it should drop its cartridge");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty_large", timeoutTicks = 100)
    public static void activityReadsOnEveryMachineThatHoldsCoolant(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        BlockPos tank = new BlockPos(8, 2, 8);
        BlockPos exchanger = new BlockPos(8, 2, 6);
        BlockPos cleaner = new BlockPos(8, 2, 4);
        h.setBlock(tank, com.simibubi.create.AllBlocks.FLUID_TANK.getDefaultState());
        h.setBlock(exchanger, CMBlocks.HEAT_EXCHANGER.get());
        h.setBlock(cleaner, CMBlocks.DEMINERALIZER.get());
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    com.criticalmass.content.thermal.ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
                    c.coolant().set(dirty(1000), 0);
                    c.coolant().give(new CoolantBuffer.Packet(CoolantLoopTests.coolant(1000), 0));
                    PlantChannel.Reading vessel = PlantChannel.ACTIVITY.read(h.getLevel(), h.absolutePos(TestVessels.CONTROLLER));
                    h.assertTrue(vessel != null && near(vessel.value(), 50) && vessel.max() == 100 && vessel.unit().equals("%"), "The controller should read its coolant's share: " + vessel);
                    h.assertTrue(com.criticalmass.content.control.ScreenBlockEntity.ROWS.contains(PlantChannel.ACTIVITY), "The reactor board should list the activity");
                    IFluidHandler pipe = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, h.absolutePos(tank), Direction.NORTH);
                    pipe.fill(CoolantLoopTests.coolant(900), IFluidHandler.FluidAction.EXECUTE);
                    pipe.fill(dirty(100), IFluidHandler.FluidAction.EXECUTE);
                    PlantChannel.Reading held = PlantChannel.ACTIVITY.read(h.getLevel(), h.absolutePos(tank));
                    h.assertTrue(held != null && near(held.value(), 10), "A gauge on a tank should read the share of what it holds: " + held);
                    com.criticalmass.content.thermal.HeatExchangerBlockEntity hx = h.getBlockEntity(exchanger);
                    hx.primary().set(dirty(500), 0);
                    hx.secondary().set(new FluidStack(Fluids.WATER, 400), 0);
                    hx.secondary().give(new CoolantBuffer.Packet(new FluidStack(Fluids.WATER, 100), 0, 0, 25));
                    PlantChannel.Reading steam = PlantChannel.ACTIVITY.read(h.getLevel(), h.absolutePos(exchanger));
                    h.assertTrue(steam != null && near(steam.value(), 5), "A gauge on a Heat Exchanger should read its steam side: " + steam);
                    DemineralizerBlockEntity resin = h.getBlockEntity(cleaner);
                    resin.inlet().set(dirty(300), 0);
                    PlantChannel.Reading waiting = PlantChannel.ACTIVITY.read(h.getLevel(), h.absolutePos(cleaner));
                    h.assertTrue(waiting != null && near(waiting.value(), 100), "A gauge on a Demineralizer should read what is waiting: " + waiting);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 1400)
    public static void aCleanUpLineBringsALoopBackToPrimaryCoolant(GameTestHelper h) {
        h.setBlock(AT, CMBlocks.DEMINERALIZER.get());
        DemineralizerBlockEntity be = h.getBlockEntity(AT);
        be.insert(new ItemStack(CMItems.RESIN_CARTRIDGE.get()));
        BlockPos abs = h.absolutePos(AT);
        CoolantBuffer loop = buffer(4000);
        loop.set(dirty(2000), 40000);
        IFluidHandler vessel = loop.handler(h.getLevel(), abs.above(3), Direction.NORTH);
        IFluidHandler flow = be.flow(Direction.NORTH);
        int[] ticks = new int[1];
        h.onEachTick(() -> {
            ticks[0]++;
            FluidStack letdown = vessel.drain(DemineralizerBlockEntity.RATE, IFluidHandler.FluidAction.SIMULATE);
            int room = flow.fill(letdown, IFluidHandler.FluidAction.SIMULATE);
            if (room > 0) {
                flow.fill(vessel.drain(room, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
            }
            FluidStack back = flow.drain(DemineralizerBlockEntity.RATE, IFluidHandler.FluidAction.SIMULATE);
            if (!back.isEmpty() && vessel.fill(back, IFluidHandler.FluidAction.SIMULATE) == back.getAmount()) {
                vessel.fill(flow.drain(back.getAmount(), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
            }
        });
        h.startSequence()
                .thenIdle(100)
                .thenExecute(() -> h.assertTrue(CMThermal.isContaminated(loop.fluid()) && loop.activityShare() < 0.9 && loop.activityShare() > 0.3, "The loop should be getting cleaner: " + loop.activityShare()))
                .thenWaitUntil(() -> h.assertTrue(!CMThermal.isContaminated(loop.fluid()), "The loop should come back to Primary Coolant, share " + loop.activityShare() + " after " + ticks[0]))
                .thenExecute(() -> {
                    double held = loop.activityLoad() + be.inlet().activityLoad() + be.load() * DemineralizerBlockEntity.RESIN_CAPACITY;
                    h.assertTrue(near(held, 2000) && be.load() > 0.45 && be.load() < 0.5, "The resin should hold what left the coolant: " + be.load() + " of " + held);
                    h.assertTrue(loop.amount() + be.inlet().amount() + be.outlet().amount() == 2000 && near(loop.heat() + be.inlet().heat() + be.outlet().heat(), 40000), "No coolant or heat should be lost");
                })
                .thenSucceed();
    }
}
