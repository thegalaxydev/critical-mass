package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantBuffer;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMFluids;
import com.simibubi.create.api.effect.OpenPipeEffectHandler;
import com.simibubi.create.api.event.PipeCollisionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class LavaReactionTests {
    private static final String EMPTY = "empty";
    private static final BlockPos POOL = new BlockPos(2, 2, 2);
    private static final IFluidHandler.FluidAction DO = IFluidHandler.FluidAction.EXECUTE;

    private LavaReactionTests() {
    }

    private static FluidStack dirty(int amount) {
        return new FluidStack(CMFluids.CONTAMINATED_COOLANT.source.get(), amount);
    }

    private static int formed(GameTestHelper h, int clean, int trials, boolean tracked) {
        OpenPipeEffectHandler spill = OpenPipeEffectHandler.REGISTRY.get(CMFluids.CONTAMINATED_COOLANT.source.get());
        BlockPos abs = h.absolutePos(POOL);
        AABB end = new AABB(abs.above()).expandTowards(0, -1, 0);
        int chartrine = 0;
        for (int i = 0; i < trials; i++) {
            h.setBlock(POOL, Blocks.LAVA);
            if (tracked) {
                CoolantBuffer loop = new CoolantBuffer(8000, () -> {
                });
                loop.set(dirty(1000), 0);
                if (clean > 0) {
                    loop.give(new CoolantBuffer.Packet(CoolantLoopTests.coolant(clean), 0));
                }
                loop.handler(h.getLevel(), abs.above(3), Direction.DOWN).drain(1000, DO);
            }
            spill.apply(h.getLevel(), end, dirty(1000));
            BlockState made = h.getBlockState(POOL);
            h.assertTrue(made.is(CMBlocks.CHARTRINE.get()) || made.is(Blocks.TUFF), "A full bucket spilled on lava should always make stone, got " + made);
            if (made.is(CMBlocks.CHARTRINE.get())) {
                chartrine++;
            }
        }
        return chartrine;
    }

    @GameTest(template = EMPTY)
    public static void contaminatedCoolantOnLavaMakesChartrineByItsShare(GameTestHelper h) {
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    OpenPipeEffectHandler spill = OpenPipeEffectHandler.REGISTRY.get(CMFluids.CONTAMINATED_COOLANT.source.get());
                    h.assertTrue(spill != null, "Contaminated Coolant should have an open pipe effect");
                    h.assertTrue(OpenPipeEffectHandler.REGISTRY.get(CMFluids.PRIMARY_COOLANT.source.get()) == null, "Primary Coolant should have none");
                    h.assertTrue(CMThermal.CHARTRINE_COOLANT <= 1000, "The trials below spill a full bucket to be sure of a block");
                    int full = formed(h, 0, 20, true);
                    h.assertTrue(full == 20, "Fully contaminated coolant should always make Chartrine: " + full + " of 20");
                    int half = formed(h, 1000, 200, true);
                    h.assertTrue(half >= 60 && half <= 140, "Coolant at half share should make Chartrine about half the time: " + half + " of 200");
                    int thin = formed(h, 3000, 200, true);
                    h.assertTrue(thin >= 20 && thin <= 85, "Coolant at a quarter share should make Chartrine about a quarter of the time: " + thin + " of 200");
                    int unknown = formed(h, 0, 100, false);
                    h.assertTrue(unknown <= 10, "Coolant of unknown share should count as barely contaminated: " + unknown + " of 100");

                    h.setBlock(POOL, Blocks.STONE);
                    spill.apply(h.getLevel(), new AABB(h.absolutePos(POOL).above()).expandTowards(0, -1, 0), dirty(1000));
                    h.assertBlockPresent(Blocks.STONE, POOL);
                    h.setBlock(POOL, Blocks.WATER);
                    spill.apply(h.getLevel(), new AABB(h.absolutePos(POOL).above()).expandTowards(0, -1, 0), dirty(1000));
                    h.assertBlockPresent(Blocks.WATER, POOL);
                    h.setBlock(POOL, Blocks.AIR);

                    PipeCollisionEvent.Flow met = new PipeCollisionEvent.Flow(h.getLevel(), h.absolutePos(POOL), Fluids.LAVA, CMFluids.CONTAMINATED_COOLANT.source.get(), null);
                    NeoForge.EVENT_BUS.post(met);
                    h.assertTrue(met.getState() != null && (met.getState().is(Blocks.TUFF) || met.getState().is(CMBlocks.CHARTRINE.get())), "Lava and Contaminated Coolant meeting in a pipe should leave stone: " + met.getState());
                    PipeCollisionEvent.Flow plain = new PipeCollisionEvent.Flow(h.getLevel(), h.absolutePos(POOL), Fluids.LAVA, CMFluids.PRIMARY_COOLANT.source.get(), null);
                    NeoForge.EVENT_BUS.post(plain);
                    h.assertTrue(plain.getState() == null, "Lava and Primary Coolant should leave nothing of ours: " + plain.getState());
                })
                .thenSucceed();
    }
}
