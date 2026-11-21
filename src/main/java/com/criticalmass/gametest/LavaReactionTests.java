package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.radiation.ContaminationData;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantBuffer;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMFluids;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.api.effect.OpenPipeEffectHandler;
import com.simibubi.create.api.event.PipeCollisionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
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

    private static CoolantBuffer buffer() {
        return new CoolantBuffer(8000, () -> {
        });
    }

    private static CoolantBuffer fromTheReactor(int clean) {
        CoolantBuffer loop = buffer();
        loop.set(CoolantLoopTests.coolant(1000), 0);
        loop.contaminate();
        if (clean > 0) {
            loop.give(new CoolantBuffer.Packet(CoolantLoopTests.coolant(clean), 0));
        }
        return loop;
    }

    private static AABB end(GameTestHelper h) {
        return new AABB(h.absolutePos(POOL).above()).expandTowards(0, -1, 0);
    }

    private static OpenPipeEffectHandler spill() {
        return OpenPipeEffectHandler.REGISTRY.get(CMFluids.CONTAMINATED_COOLANT.source.get());
    }

    private static int formed(GameTestHelper h, int trials, java.util.function.Supplier<CoolantBuffer> source) {
        BlockPos abs = h.absolutePos(POOL);
        int chartrine = 0;
        for (int i = 0; i < trials; i++) {
            h.setBlock(POOL, Blocks.LAVA);
            CoolantBuffer from = source.get();
            if (from != null) {
                from.handler(h.getLevel(), abs.above(3), Direction.DOWN).drain(1000, DO);
            }
            spill().apply(h.getLevel(), end(h), dirty(1000));
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
        CoolantBuffer carried = buffer();
        ItemStack[] bucket = new ItemStack[1];
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    ContaminationData floor = ContaminationData.get(h.getLevel());
                    BlockPos mouth = h.absolutePos(POOL).above();
                    floor.clear(mouth);
                    h.assertTrue(spill() != null, "Contaminated Coolant should have an open pipe effect");
                    h.assertTrue(OpenPipeEffectHandler.REGISTRY.get(CMFluids.PRIMARY_COOLANT.source.get()) == null, "Primary Coolant should have none");
                    h.assertTrue(CMThermal.CHARTRINE_COOLANT <= 1000, "The trials below spill a full bucket to be sure of a block");
                    int full = formed(h, 20, () -> fromTheReactor(0));
                    h.assertTrue(full == 20, "Coolant contaminated in the reactor should always make Chartrine: " + full + " of 20");
                    int half = formed(h, 200, () -> fromTheReactor(1000));
                    h.assertTrue(half >= 60 && half <= 140, "Watered down by half it should make Chartrine about half the time: " + half + " of 200");
                    int thin = formed(h, 200, () -> fromTheReactor(3000));
                    h.assertTrue(thin >= 20 && thin <= 85, "Watered down to a quarter it should make Chartrine about a quarter of the time: " + thin + " of 200");
                    int unknown = formed(h, 40, () -> null);
                    h.assertTrue(unknown == 0, "Coolant with no record should make only tuff: " + unknown + " of 40");
                    int assumed = formed(h, 40, () -> {
                        CoolantBuffer store = buffer();
                        store.set(dirty(1000), 0);
                        return store;
                    });
                    h.assertTrue(assumed == 0, "Coolant that is only assumed to be contaminated should make only tuff: " + assumed + " of 40");
                    h.assertTrue(floor.get(mouth) == 0, "Coolant that lands on lava should not dirty the floor: " + floor.get(mouth));

                    h.setBlock(POOL, Blocks.STONE);
                    fromTheReactor(0).handler(h.getLevel(), mouth.above(2), Direction.DOWN).drain(1000, DO);
                    spill().apply(h.getLevel(), end(h), dirty(1000));
                    h.assertBlockPresent(Blocks.STONE, POOL);
                    h.assertTrue(Math.abs(floor.clear(mouth) - 1000 * CMThermal.CONTAMINATED_SPILL) < 1e-6, "Contaminated Coolant dumped from a pipe should dirty the floor as a leak of it does");
                    fromTheReactor(1000).handler(h.getLevel(), mouth.above(2), Direction.DOWN).drain(1000, DO);
                    spill().apply(h.getLevel(), end(h), dirty(1000));
                    h.assertTrue(Math.abs(floor.clear(mouth) - (500 * CMThermal.CONTAMINATED_SPILL + 500 * CMThermal.CLEAN_SPILL)) < 1e-6, "Watered down it should dirty the floor less");
                    h.setBlock(POOL, Blocks.WATER);
                    spill().apply(h.getLevel(), end(h), dirty(1000));
                    h.assertBlockPresent(Blocks.WATER, POOL);
                    floor.clear(mouth);
                    h.setBlock(POOL, Blocks.AIR);

                    PipeCollisionEvent.Flow met = new PipeCollisionEvent.Flow(h.getLevel(), h.absolutePos(POOL), Fluids.LAVA, CMFluids.CONTAMINATED_COOLANT.source.get(), null);
                    NeoForge.EVENT_BUS.post(met);
                    h.assertTrue(met.getState() != null && met.getState().is(Blocks.TUFF), "Lava and Contaminated Coolant meeting in a pipe should leave tuff: " + met.getState());
                    PipeCollisionEvent.Flow plain = new PipeCollisionEvent.Flow(h.getLevel(), h.absolutePos(POOL), Fluids.LAVA, CMFluids.PRIMARY_COOLANT.source.get(), null);
                    NeoForge.EVENT_BUS.post(plain);
                    h.assertTrue(plain.getState() == null, "Lava and Primary Coolant should leave nothing of ours: " + plain.getState());

                    CoolantBuffer reactor = fromTheReactor(0);
                    h.assertTrue(reactor.provenShare() > 0.999 && reactor.activityShare() > 0.999, "The reactor's own contamination should be on record");
                    FluidActionResult drawn = FluidUtil.tryFillContainer(new ItemStack(Items.BUCKET), reactor.handler(h.getLevel(), mouth, Direction.DOWN), 1000, null, true);
                    h.assertTrue(drawn.isSuccess() && drawn.getResult().is(CMItems.CONTAMINATED_COOLANT_BUCKET.get()), "A bucket should draw the coolant off: " + drawn.getResult());
                    bucket[0] = drawn.getResult();
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    BlockPos mouth = h.absolutePos(POOL).above();
                    FluidActionResult poured = FluidUtil.tryEmptyContainer(bucket[0], carried.handler(h.getLevel(), mouth, Direction.DOWN), 1000, null, true);
                    h.assertTrue(poured.isSuccess() && carried.amount() == 1000, "The bucket should pour into another store");
                    h.assertTrue(carried.activityShare() > 0.999 && carried.provenLoad() == 0, "Coolant carried in a bucket should stay dangerous and lose its record: " + carried.activityShare() + ", " + carried.provenLoad());
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    double share = carried.provenShare();
                    int laundered = formed(h, 20, () -> {
                        CoolantBuffer again = buffer();
                        again.give(new CoolantBuffer.Packet(dirty(1000), 0, 0, 1000, share * 1000));
                        return again;
                    });
                    h.assertTrue(laundered == 0, "Coolant that went through a bucket should make only tuff: " + laundered + " of 20");
                    h.setBlock(POOL, Blocks.AIR);
                    ContaminationData.get(h.getLevel()).clear(h.absolutePos(POOL).above());
                })
                .thenSucceed();
    }
}
