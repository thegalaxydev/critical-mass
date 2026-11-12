package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.InstrumentationPanelBlockEntity;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class PanelTests {
    private static final String EMPTY = "empty";

    private PanelTests() {
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void panelReadsTheReactorItIsMountedOn(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity controller = h.getBlockEntity(TestVessels.CONTROLLER);
        double hot = 300;
        controller.coolant().set(CoolantLoopTests.coolant(6000), 6000 * (hot - CMThermal.AMBIENT) * CMThermal.HEAT_CAPACITY);
        BlockPos mounted = TestVessels.CONTROLLER.west().above();
        BlockPos loose = new BlockPos(12, 1, 12);
        h.setBlock(mounted, CMBlocks.INSTRUMENTATION_PANEL.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
        h.setBlock(loose, CMBlocks.INSTRUMENTATION_PANEL.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    InstrumentationPanelBlockEntity panel = h.getBlockEntity(mounted);
                    InstrumentationPanelBlockEntity nearby = h.getBlockEntity(loose);
                    h.assertTrue(panel.linked() && panel.status().equals(controller.status()), "Mounted panel should show the controller status: " + panel.status() + " vs " + controller.status());
                    h.assertTrue(Math.abs(panel.coolant() - controller.coolant().temperature()) < 1, "Panel should show the vessel coolant temperature: " + panel.coolant());
                    h.assertTrue(panel.flow() == controller.effectiveFlow(), "Panel should show the vessel flow");
                    int expected = Math.round(15 * InstrumentationPanelBlockEntity.coreFraction(Math.max(CMThermal.AMBIENT, controller.core().maxTemperature())));
                    h.assertTrue(panel.comparator() == expected, "Comparator should follow the hottest fuel: " + panel.comparator() + " vs " + expected);
                    h.assertTrue(nearby.linked(), "A panel within range should find the nearest controller");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY)
    public static void lampsFollowReactorStates(GameTestHelper h) {
        h.assertTrue(InstrumentationPanelBlockEntity.lampFor("criticalmass.reactor.status.running") == InstrumentationPanelBlockEntity.Lamp.GREEN, "Running is green");
        h.assertTrue(InstrumentationPanelBlockEntity.lampFor("criticalmass.reactor.status.starting") == InstrumentationPanelBlockEntity.Lamp.AMBER, "Starting is amber");
        h.assertTrue(InstrumentationPanelBlockEntity.lampFor("criticalmass.reactor.status.cooling_down") == InstrumentationPanelBlockEntity.Lamp.AMBER, "Cooling down is amber");
        h.assertTrue(InstrumentationPanelBlockEntity.lampFor("criticalmass.reactor.status.tripped") == InstrumentationPanelBlockEntity.Lamp.RED, "Tripped is red");
        h.assertTrue(InstrumentationPanelBlockEntity.lampFor("criticalmass.reactor.status.breached") == InstrumentationPanelBlockEntity.Lamp.RED, "Breached is red");
        h.assertTrue(InstrumentationPanelBlockEntity.lampFor("criticalmass.reactor.status.cold") == InstrumentationPanelBlockEntity.Lamp.NONE, "Cold has no lamp");
        h.succeed();
    }
}
