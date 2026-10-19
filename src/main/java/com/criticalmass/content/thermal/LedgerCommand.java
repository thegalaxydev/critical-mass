package com.criticalmass.content.thermal;

import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class LedgerCommand {
    private LedgerCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("criticalmass").requires(s -> s.hasPermission(2))
                .then(Commands.literal("ledger").executes(ctx -> report(ctx.getSource()))));
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }

    private static String where(BlockEntity be) {
        BlockPos p = be.getBlockPos();
        return p.getX() + " " + p.getY() + " " + p.getZ() + " " + be.getLevel().dimension().location();
    }

    private static int report(CommandSourceStack source) {
        List<BlockEntity> all = LoadedThermalBlocks.snapshot();
        all.sort(Comparator.comparing(be -> be.getClass().getSimpleName() + where(be)));
        long coolant = 0;
        double stored = 0;
        double added = 0;
        double extracted = 0;
        int controllers = 0;
        int pumps = 0;
        int turbines = 0;
        for (BlockEntity be : all) {
            if (be instanceof ReactorControllerBlockEntity c) {
                controllers++;
                coolant += c.coolant().amount();
                stored += c.coolant().heat() + c.core().storedHeat();
                added += c.heatAdded();
                source.sendSuccess(() -> Component.literal("controller " + where(c) + " status=" + c.status() + " coolant=" + c.coolant().amount() + " heat=" + fmt(c.coolant().heat())
                        + " temp=" + fmt(c.coolant().temperature()) + " core=" + fmt(c.core().storedHeat()) + " coreMax=" + fmt(c.core().maxTemperature())
                        + " hotSpots=" + c.core().hotSpots() + " added=" + fmt(c.heatAdded()) + " insertion=" + fmt(c.insertion()) + " flow=" + c.effectiveFlow()
                        + " problems=" + c.problemPositions().size() + " missing=" + c.missing().size()), false);
            } else if (be instanceof SteamTurbineBlockEntity t) {
                turbines++;
                coolant += t.coolant().amount();
                stored += t.coolant().heat();
                extracted += t.heatExtracted();
                source.sendSuccess(() -> Component.literal("turbine " + where(t) + " coolant=" + t.coolant().amount() + " heat=" + fmt(t.coolant().heat())
                        + " extracted=" + fmt(t.heatExtracted()) + " capacity=" + fmt(t.stressCapacity()) + " speed=" + t.getSpeed()), false);
            } else if (be instanceof CirculationPumpBlockEntity p) {
                pumps++;
                source.sendSuccess(() -> Component.literal("pump " + where(p) + " flow=" + p.flow() + " moved=" + p.movedTotal() + " speed=" + p.getSpeed()
                        + " status=" + p.status()), false);
            }
        }
        double balance = added - stored - extracted;
        String summary = "totals controllers=" + controllers + " pumps=" + pumps + " turbines=" + turbines + " coolant=" + coolant
                + " stored=" + fmt(stored) + " added=" + fmt(added) + " extracted=" + fmt(extracted) + " balance=" + fmt(balance);
        source.sendSuccess(() -> Component.literal(summary), false);
        return controllers + pumps + turbines;
    }
}
