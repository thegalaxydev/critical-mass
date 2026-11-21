package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.control.DashPart;
import com.criticalmass.content.control.PlantChannel;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantBuffer;
import com.criticalmass.content.thermal.TankCoolant;
import com.criticalmass.content.thermal.TankFigure;
import com.criticalmass.registry.CMFluids;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class TankFigureTests {
    private static final String EMPTY = "empty";
    private static final BlockPos HOLDING = new BlockPos(0, 1, 0);
    private static final BlockPos WASTE = new BlockPos(2, 1, 2);
    private static final BlockPos DILUTED = new BlockPos(4, 1, 0);
    private static final BlockPos WATER = new BlockPos(0, 1, 4);
    private static final IFluidHandler.FluidAction DO = IFluidHandler.FluidAction.EXECUTE;

    private TankFigureTests() {
    }

    private static FluidStack dirty(int amount) {
        return new FluidStack(CMFluids.CONTAMINATED_COOLANT.source.get(), amount);
    }

    private static FluidStack acid(int amount) {
        return new FluidStack(CMFluids.BORIC_ACID.source.get(), amount);
    }

    private static CoolantBuffer buffer() {
        return new CoolantBuffer(8000, () -> {
        });
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-6 * Math.max(1, Math.abs(b));
    }

    private static IFluidHandler pipe(GameTestHelper h, BlockPos pos) {
        return h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pos), Direction.NORTH);
    }

    private static CoolantBuffer held(GameTestHelper h, BlockPos pos) {
        return TankCoolant.at(h.getLevel(), h.absolutePos(pos)).coolant();
    }

    private static FluidStack inside(GameTestHelper h, BlockPos pos) {
        FluidTankBlockEntity tank = h.getBlockEntity(pos);
        return tank.getTankInventory().getFluid();
    }

    @GameTest(template = EMPTY)
    public static void aFluidTankKeepsTheFigureOfTheCoolantItHolds(GameTestHelper h) {
        for (BlockPos pos : new BlockPos[] {HOLDING, WASTE, DILUTED, WATER}) {
            h.setBlock(pos, AllBlocks.FLUID_TANK.getDefaultState());
        }
        BlockPos side = h.absolutePos(new BlockPos(2, 3, 2));
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    IFluidHandler holding = pipe(h, HOLDING);
                    h.assertTrue(holding instanceof TankFigure, "A Fluid Tank should be reached through the figure keeper: " + holding);
                    CoolantBuffer vessel = buffer();
                    vessel.set(CoolantLoopTests.coolant(4000), 80000);
                    vessel.setBoronPpm(1200);
                    IFluidHandler port = vessel.handler(h.getLevel(), side, Direction.SOUTH);
                    h.assertTrue(holding.fill(port.drain(1000, DO), DO) == 1000, "The tank should take the letdown");
                    CoolantBuffer kept = held(h, HOLDING);
                    h.assertTrue(inside(h, HOLDING).getAmount() == 1000 && kept.amount() == 1000 && near(kept.boronPpm(), 1200) && near(kept.heat(), 20000),
                            "Coolant let down into a tank should keep its boron and its heat: " + kept.boronPpm() + " ppm, " + kept.heat());
                    CoolantBuffer back = buffer();
                    h.assertTrue(back.handler(h.getLevel(), side, Direction.SOUTH).fill(holding.drain(500, DO), DO) == 500, "The tank should give it back");
                    h.assertTrue(near(back.boronPpm(), 1200) && near(back.heat(), 10000) && inside(h, HOLDING).getAmount() == 500, "What comes back should carry what went in: " + back.boronPpm());
                    PlantChannel.Reading boron = PlantChannel.BORON.read(h.getLevel(), h.absolutePos(HOLDING));
                    h.assertTrue(boron != null && near(boron.value(), 1200) && DashPart.accepts(DashPart.Type.GAUGE, h.getLevel(), h.absolutePos(HOLDING)), "A gauge should read the tank's boron: " + boron);
                    h.assertTrue(holding.fill(acid(500), DO) == 500 && CMThermal.isPrimaryMedium(inside(h, HOLDING)) && inside(h, HOLDING).getAmount() == 1000,
                            "Boric Acid should join the coolant in a tank: " + inside(h, HOLDING));
                    h.assertTrue(near(held(h, HOLDING).boronPpm(), (500 * 1200.0 + 500 * CMThermal.BORIC_ACID_PPM) / 1000), "The acid should bring its boron: " + held(h, HOLDING).boronPpm());

                    CoolantBuffer loop = buffer();
                    loop.set(dirty(1000), 0);
                    loop.give(new CoolantBuffer.Packet(CoolantLoopTests.coolant(1000), 0));
                    IFluidHandler waste = pipe(h, WASTE);
                    waste.fill(loop.handler(h.getLevel(), side, Direction.SOUTH).drain(600, DO), DO);
                    h.assertTrue(CMThermal.isContaminated(inside(h, WASTE)) && near(held(h, WASTE).activityLoad(), 300), "A tank should keep the share of its Contaminated Coolant: " + held(h, WASTE).activityLoad());
                    CoolantBuffer on = buffer();
                    on.handler(h.getLevel(), side, Direction.SOUTH).fill(waste.drain(600, DO), DO);
                    h.assertTrue(near(on.activityLoad(), 300) && inside(h, WASTE).isEmpty(), "It should give the same share back: " + on.activityLoad());

                    IFluidHandler diluted = pipe(h, DILUTED);
                    h.assertTrue(diluted.fill(CoolantLoopTests.coolant(990), DO) == 990 && diluted.fill(dirty(10), DO) == 10, "Contaminated Coolant should mix into the coolant in a tank");
                    h.assertTrue(inside(h, DILUTED).getAmount() == 1000 && !CMThermal.isContaminated(inside(h, DILUTED)) && near(held(h, DILUTED).activityLoad(), 10),
                            "One part in a hundred should leave the tank Primary Coolant: " + inside(h, DILUTED));
                    h.assertTrue(diluted.fill(dirty(10), DO) == 10 && CMThermal.isContaminated(inside(h, DILUTED)) && inside(h, DILUTED).getAmount() == 1010, "A little more should turn the tank: " + inside(h, DILUTED));

                    IFluidHandler water = pipe(h, WATER);
                    h.assertTrue(water.fill(new FluidStack(Fluids.WATER, 1000), DO) == 1000 && water.fill(CoolantLoopTests.coolant(100), DO) == 0 && inside(h, WATER).is(Fluids.WATER),
                            "A tank of water should stay a tank of water");
                    h.assertTrue(water.drain(400, DO).getAmount() == 400 && inside(h, WATER).getAmount() == 600, "Water should come out as it always did");
                })
                .thenSucceed();
    }
}
