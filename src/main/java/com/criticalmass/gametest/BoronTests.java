package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.fuel.SpentFuelRackBlockEntity;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantBuffer;
import com.criticalmass.content.thermal.PipeCoolant;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class BoronTests {
    private static final String EMPTY = "empty";
    private static final String LARGE = "empty_large";

    private BoronTests() {
    }

    private static FluidStack acid(int amount) {
        return new FluidStack(CMFluids.BORIC_ACID.source.get(), amount);
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) <= 1e-6 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
    }

    private static double pipeBoron(GameTestHelper h) {
        double total = 0;
        for (BlockPos p : TestVessels.PIPES) {
            PipeCoolant c = PipeCoolant.at(h.getLevel(), h.absolutePos(p));
            total += c == null ? 0 : c.coolant().boronLoad();
        }
        return total;
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void boronSplitsAndMergesWithTheCoolant(GameTestHelper h) {
        CoolantBuffer a = new CoolantBuffer(8000, () -> {
        });
        CoolantBuffer b = new CoolantBuffer(8000, () -> {
        });
        a.set(CoolantLoopTests.coolant(4000), 0);
        a.setBoronPpm(1200);
        b.set(CoolantLoopTests.coolant(1000), 0);
        b.give(a.take(1000));
        h.assertTrue(near(a.boronPpm(), 1200) && near(b.boronPpm(), 600), "A split keeps the concentration and a merge averages it: " + a.boronPpm() + " and " + b.boronPpm());
        h.assertTrue(near(a.boronLoad() + b.boronLoad(), 4000 * 1200.0), "Boron should be conserved: " + (a.boronLoad() + b.boronLoad()));
        b.give(new CoolantBuffer.Packet(acid(500), 0));
        h.assertTrue(b.amount() == 2500 && CMThermal.isPrimaryMedium(b.fluid()), "Boric Acid should join the coolant it is poured into: " + b.fluid());
        h.assertTrue(near(b.boronLoad(), 1000 * 1200.0 + 500 * CMThermal.BORIC_ACID_PPM), "Boric Acid should bring its boron: " + b.boronLoad());
        a.set(a.fluid().copy(), 12345);
        h.assertTrue(near(a.boronPpm(), 1200), "Setting the coolant's heat should leave its boron alone: " + a.boronPpm());
        CoolantBuffer tank = new CoolantBuffer(8000, () -> {
        });
        tank.give(new CoolantBuffer.Packet(acid(1000), 0));
        h.assertTrue(CMThermal.isBoricAcid(tank.fluid()) && near(tank.boronPpm(), CMThermal.BORIC_ACID_PPM), "Boric Acid by itself stays Boric Acid: " + tank.boronPpm());
        tank.give(new CoolantBuffer.Packet(CoolantLoopTests.coolant(1000), 0));
        h.assertTrue(CMThermal.isPrimaryMedium(tank.fluid()) && near(tank.boronPpm(), CMThermal.BORIC_ACID_PPM / 2), "Coolant poured onto Boric Acid should make borated coolant: " + tank.boronPpm());
        CompoundTag saved = b.save(h.getLevel().registryAccess());
        CoolantBuffer copy = new CoolantBuffer(8000, () -> {
        });
        copy.load(saved, h.getLevel().registryAccess());
        h.assertTrue(copy.boronLoad() == b.boronLoad(), "Boron should survive save and load: " + copy.boronLoad());
        CoolantBuffer water = new CoolantBuffer(8000, () -> {
        });
        water.set(new FluidStack(Fluids.WATER, 1000), 0);
        h.assertTrue(!water.canMix(acid(100)), "Boric Acid should not go into plain water");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void boricAcidThroughAPortBoratesTheVessel(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    c.holdBoron(0);
                    c.coolant().set(CoolantLoopTests.coolant(6000), 0);
                    IFluidHandler port = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(TestVessels.INLET), Direction.NORTH);
                    h.assertTrue(port != null, "The inlet port should take pipes on its face");
                    h.assertTrue(port.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.SIMULATE) == 0, "Plain water should not go into primary coolant");
                    int filled = port.fill(acid(1000), IFluidHandler.FluidAction.EXECUTE);
                    h.assertTrue(filled == 1000 && c.coolant().amount() == 7000, "The port should take 1,000 mB of Boric Acid: " + filled);
                    h.assertTrue(near(c.boron(), 1000), "1,000 mB of acid in 7,000 mB should read 1,000 ppm: " + c.boron());
                    port.fill(CoolantLoopTests.coolant(1000), IFluidHandler.FluidAction.EXECUTE);
                    h.assertTrue(near(c.boron(), 875), "Clean coolant should dilute it to 875 ppm: " + c.boron());
                    FluidStack out = port.drain(2000, IFluidHandler.FluidAction.EXECUTE);
                    h.assertTrue(out.getAmount() == 2000 && CMThermal.isPrimaryMedium(out), "Letdown should come out as coolant: " + out);
                    h.assertTrue(near(c.boron(), 875) && near(c.coolant().boronLoad(), 875 * 6000.0), "Letdown should leave the concentration alone: " + c.boron());
                })
                .thenIdle(3)
                .thenExecute(() -> h.assertTrue(near(c.core().boronReactivity(), -CMThermal.BORON_WORTH * 875),
                        "The core should feel 875 ppm: " + c.core().boronReactivity()))
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 500)
    public static void thePumpCarriesBoronRoundTheLoop(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.loop(h, true);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 0);
        c.coolant().setBoronPpm(1000);
        h.startSequence()
                .thenIdle(300)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(TestVessels.PUMP);
                    SteamTurbineBlockEntity turbine = h.getBlockEntity(TestVessels.TURBINE);
                    h.assertTrue(pump.flow() > 0, "The pump should be moving coolant: " + pump.status());
                    double total = c.coolant().boronLoad() + turbine.coolant().boronLoad() + pipeBoron(h);
                    h.assertTrue(Math.abs(total - 6000 * 1000.0) < 1, "Boron should be conserved round the loop: " + total);
                    h.assertTrue(turbine.coolant().amount() > 0 && Math.abs(turbine.coolant().boronPpm() - 1000) < 1e-6, "The loop should carry the vessel's concentration: " + turbine.coolant().boronPpm());
                    c.setBoron(400);
                    double set = c.coolant().boronLoad() + turbine.coolant().boronLoad() + pipeBoron(h);
                    h.assertTrue(Math.abs(set - 6000 * 400.0) < 1 && near(c.boron(), 400), "Setting boron should set the vessel and its loop: " + set);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void boronTravelsBetweenMachinesThroughCreatePipes(GameTestHelper h) {
        BlockPos from = new BlockPos(1, 1, 1);
        BlockPos to = new BlockPos(4, 1, 1);
        h.setBlock(from, CMBlocks.SPENT_FUEL_RACK.get());
        h.setBlock(to, CMBlocks.SPENT_FUEL_RACK.get());
        SpentFuelRackBlockEntity a = h.getBlockEntity(from);
        SpentFuelRackBlockEntity b = h.getBlockEntity(to);
        a.coolant().set(CoolantLoopTests.coolant(1000), 0);
        a.coolant().setBoronPpm(2000);
        IFluidHandler source = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(from), Direction.UP);
        IFluidHandler target = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(to), Direction.UP);
        int filled = target.fill(source.drain(250, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(filled == 250 && near(b.coolant().boronPpm(), 2000), "Boron should travel with the coolant: " + b.coolant().boronPpm());
        h.assertTrue(near(a.coolant().boronLoad() + b.coolant().boronLoad(), 1000 * 2000.0), "Boron should be conserved");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void boilingLeavesTheBoronBehind(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 6000 * (CMThermal.UNPRESSURIZED_BOILING + 60 - CMThermal.AMBIENT) * CMThermal.HEAT_CAPACITY);
        c.coolant().setBoronPpm(1000);
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(c.coolant().amount() < 6000, "Coolant above its boiling point should flash off: " + c.coolant().amount());
                    h.assertTrue(near(c.coolant().boronLoad(), 6000 * 1000.0) && c.coolant().boronPpm() > 1000, "The steam should leave its boron in the vessel: " + c.coolant().boronPpm());
                })
                .thenSucceed();
    }
}
