package com.criticalmass.content.thermal;

import com.criticalmass.content.fuel.FuelState;
import com.criticalmass.content.radiation.ContaminationData;
import com.criticalmass.content.radiation.Radiation;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.ControlRodDriveBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.criticalmass.content.control.AlarmPart;
import com.lycoris.loconautics.content.controls.panel.PanelPart;
import com.lycoris.loconautics.content.controls.panel.PanelSlot;
import com.lycoris.loconautics.content.controls.panelbutton.ButtonPart;
import com.lycoris.loconautics.content.controls.panelgauge.GaugeBlockEntity;
import com.lycoris.loconautics.content.controls.panelgauge.GaugeMetric;
import com.lycoris.loconautics.content.controls.panellever.LeverPart;
import com.lycoris.loconautics.content.controls.panelswitch.SwitchPart;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class LedgerCommand {
    private LedgerCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("criticalmass").requires(s -> s.hasPermission(2))
                .then(Commands.literal("ledger").executes(ctx -> report(ctx.getSource())))
                .then(Commands.literal("desk").then(Commands.argument("desk", BlockPosArgument.blockPos()).then(Commands.argument("side", StringArgumentType.word())
                        .then(Commands.literal("gauge").then(Commands.argument("target", BlockPosArgument.blockPos()).then(Commands.argument("metric", StringArgumentType.word())
                                .executes(ctx -> gauge(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "desk"), StringArgumentType.getString(ctx, "side"),
                                        BlockPosArgument.getLoadedBlockPos(ctx, "target"), StringArgumentType.getString(ctx, "metric"))))))
                        .then(Commands.literal("alarm").then(Commands.argument("reactor", BlockPosArgument.blockPos())
                                .executes(ctx -> alarm(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "desk"), StringArgumentType.getString(ctx, "side"), BlockPosArgument.getLoadedBlockPos(ctx, "reactor")))))
                        .then(Commands.argument("part", StringArgumentType.word()).then(Commands.argument("link", BlockPosArgument.blockPos()).then(Commands.argument("color", IntegerArgumentType.integer(0, 34))
                                .executes(ctx -> control(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "desk"), StringArgumentType.getString(ctx, "side"), StringArgumentType.getString(ctx, "part"),
                                        BlockPosArgument.getLoadedBlockPos(ctx, "link"), IntegerArgumentType.getInteger(ctx, "color"), ""))
                                .then(Commands.argument("label", StringArgumentType.greedyString())
                                        .executes(ctx -> control(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "desk"), StringArgumentType.getString(ctx, "side"), StringArgumentType.getString(ctx, "part"),
                                                BlockPosArgument.getLoadedBlockPos(ctx, "link"), IntegerArgumentType.getInteger(ctx, "color"), StringArgumentType.getString(ctx, "label"))))))))))
                .then(Commands.literal("radiation").then(Commands.argument("pos", Vec3Argument.vec3())
                        .executes(ctx -> probe(ctx.getSource(), Vec3Argument.getVec3(ctx, "pos")))))
                .then(Commands.literal("contaminate").then(Commands.argument("pos", BlockPosArgument.blockPos()).then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> contaminate(ctx.getSource(), BlockPosArgument.getBlockPos(ctx, "pos"), DoubleArgumentType.getDouble(ctx, "amount"))))))
                .then(Commands.literal("boron").then(Commands.argument("controller", BlockPosArgument.blockPos()).then(Commands.argument("ppm", DoubleArgumentType.doubleArg(0, 5000))
                        .executes(ctx -> boron(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "controller"), DoubleArgumentType.getDouble(ctx, "ppm"))))))
                .then(Commands.literal("steady").then(Commands.argument("controller", BlockPosArgument.blockPos()).then(Commands.argument("power", DoubleArgumentType.doubleArg(0.001, 1))
                        .executes(ctx -> steady(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "controller"), DoubleArgumentType.getDouble(ctx, "power"), Double.NaN))
                        .then(Commands.argument("coolant", DoubleArgumentType.doubleArg(20, 340))
                                .executes(ctx -> steady(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "controller"), DoubleArgumentType.getDouble(ctx, "power"), DoubleArgumentType.getDouble(ctx, "coolant")))))))
                .then(Commands.literal("xenon").then(Commands.argument("controller", BlockPosArgument.blockPos()).then(Commands.argument("level", DoubleArgumentType.doubleArg(0, 3))
                        .executes(ctx -> xenon(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "controller"), DoubleArgumentType.getDouble(ctx, "level"))))))
                .then(Commands.literal("fuel").then(Commands.argument("controller", BlockPosArgument.blockPos())
                        .then(Commands.literal("fill").executes(ctx -> fuel(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "controller"), -1)))
                        .then(Commands.literal("burn").then(Commands.argument("remaining", DoubleArgumentType.doubleArg(0, 1))
                                .executes(ctx -> fuel(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "controller"), DoubleArgumentType.getDouble(ctx, "remaining"))))))));
    }

    private static PanelSlot slot(String side) {
        return side.equals("left") ? PanelSlot.TOP_LEFT : PanelSlot.TOP_RIGHT;
    }

    private static int gauge(CommandSourceStack source, BlockPos desk, String side, BlockPos target, String metricName) {
        if (!(source.getLevel().getBlockEntity(desk) instanceof GaugeBlockEntity panel)) {
            source.sendFailure(Component.literal("No dashboard at " + desk.toShortString()));
            return 0;
        }
        GaugeMetric metric = GaugeMetric.byName(metricName);
        if (!metric.getSerializedName().equals(metricName) || GaugeMetric.Group.of(source.getLevel().getBlockEntity(target)) != metric.group()) {
            source.sendFailure(Component.literal("The block at " + target.toShortString() + " has no reading called " + metricName));
            return 0;
        }
        PanelSlot slot = slot(side);
        panel.removePart(slot);
        if (!panel.addGauge(slot, target, metric)) {
            source.sendFailure(Component.literal("The dashboard would not take the gauge"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Gauge fitted: " + metricName), false);
        return 1;
    }

    private static int alarm(CommandSourceStack source, BlockPos desk, String side, BlockPos reactor) {
        if (!(source.getLevel().getBlockEntity(desk) instanceof GaugeBlockEntity panel)) {
            source.sendFailure(Component.literal("No dashboard at " + desk.toShortString()));
            return 0;
        }
        if (!(source.getLevel().getBlockEntity(reactor) instanceof ReactorControllerBlockEntity)) {
            source.sendFailure(Component.literal("No reactor controller at " + reactor.toShortString()));
            return 0;
        }
        PanelSlot slot = slot(side);
        panel.removePart(slot);
        if (!panel.putPart(slot, new AlarmPart(reactor, 0, 0))) {
            source.sendFailure(Component.literal("The dashboard would not take the alarm"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("alarm fitted"), false);
        return 1;
    }

    private static int control(CommandSourceStack source, BlockPos desk, String side, String kind, BlockPos link, int color, String label) {
        if (!(source.getLevel().getBlockEntity(desk) instanceof GaugeBlockEntity panel)) {
            source.sendFailure(Component.literal("No dashboard at " + desk.toShortString()));
            return 0;
        }
        PanelPart part = switch (kind) {
            case "button" -> new ButtonPart(0L, link, ButtonPart.DEFAULT_BODY, color, ButtonPart.DEFAULT_STYLE, ButtonPart.DEFAULT_OPTIONS);
            case "switch" -> new SwitchPart(0, link, SwitchPart.DEFAULT_BODY, color, SwitchPart.DEFAULT_STYLE, SwitchPart.DEFAULT_OPTIONS);
            case "lever" -> new LeverPart(0, link, LeverPart.DEFAULT_BODY, color, LeverPart.DEFAULT_STYLE, LeverPart.DEFAULT_OPTIONS);
            default -> null;
        };
        if (part == null) {
            source.sendFailure(Component.literal("Parts are button, switch, lever and gauge"));
            return 0;
        }
        PanelSlot slot = slot(side);
        panel.removePart(slot);
        if (!panel.putPart(slot, part)) {
            source.sendFailure(Component.literal("The dashboard would not take the " + kind));
            return 0;
        }
        if (!label.isEmpty()) {
            panel.setLabel(slot, Component.literal(label));
        }
        source.sendSuccess(() -> Component.literal(kind + " fitted"), false);
        return 1;
    }

    private static int probe(CommandSourceStack source, Vec3 pos) {
        double rate = Radiation.ambientRate(source.getLevel(), pos);
        double contamination = ContaminationData.get(source.getLevel()).near(pos, 2);
        for (Radiation.Source s : Radiation.sourcesNear(source.getLevel(), pos, Radiation.blockSources(source.getLevel()))) {
            Vec3 local = s.body() != null ? s.body().logicalPose().transformPositionInverse(pos) : pos;
            double part = Radiation.fromSources(source.getLevel(), pos, List.of(s), null);
            if (part > 0.01) {
                source.sendSuccess(() -> Component.literal("  source " + fmt(s.pos().x) + " " + fmt(s.pos().y) + " " + fmt(s.pos().z) + " strength=" + fmt(s.strength())
                        + (s.body() != null ? " body=" + s.body().getName() + " local=" + fmt(local.x) + " " + fmt(local.y) + " " + fmt(local.z) : "") + " gives=" + fmt(part)), false);
            }
        }
        source.sendSuccess(() -> Component.literal("radiation " + fmt(pos.x) + " " + fmt(pos.y) + " " + fmt(pos.z) + " rate=" + fmt(rate) + " contamination=" + fmt(contamination)
                + " cells=" + ContaminationData.get(source.getLevel()).size() + " total=" + fmt(ContaminationData.get(source.getLevel()).total())), false);
        return (int) Math.round(rate);
    }

    private static int contaminate(CommandSourceStack source, BlockPos pos, double amount) {
        ContaminationData.get(source.getLevel()).add(pos, amount);
        source.sendSuccess(() -> Component.literal("Contaminated " + pos.toShortString() + " by " + fmt(amount)), true);
        return 1;
    }

    private static int boron(CommandSourceStack source, BlockPos pos, double ppm) {
        if (!(source.getLevel().getBlockEntity(pos) instanceof ReactorControllerBlockEntity c)) {
            source.sendFailure(Component.literal("No reactor controller at " + pos.toShortString()));
            return 0;
        }
        c.setBoron(ppm);
        source.sendSuccess(() -> Component.literal("Boron set to " + fmt(ppm) + " ppm"), true);
        return 1;
    }

    private static int steady(CommandSourceStack source, BlockPos pos, double power, double coolant) {
        if (!(source.getLevel().getBlockEntity(pos) instanceof ReactorControllerBlockEntity c)) {
            source.sendFailure(Component.literal("No reactor controller at " + pos.toShortString()));
            return 0;
        }
        if (!Double.isNaN(coolant) && c.coolant().amount() > 0) {
            c.coolant().set(c.coolant().fluid().copy(), c.coolant().amount() * (coolant - CMThermal.AMBIENT) * CMThermal.HEAT_CAPACITY);
        }
        double insertion = c.setSteadyPower(power);
        source.sendSuccess(() -> Component.literal("Reactor set to " + fmt(power * 100) + "% power with rods " + fmt(insertion * 100) + "% inserted"), true);
        return 1;
    }

    private static int xenon(CommandSourceStack source, BlockPos pos, double level) {
        if (!(source.getLevel().getBlockEntity(pos) instanceof ReactorControllerBlockEntity c)) {
            source.sendFailure(Component.literal("No reactor controller at " + pos.toShortString()));
            return 0;
        }
        c.core().setKinetics(c.core().neutronPower(), level, level);
        source.sendSuccess(() -> Component.literal("Xenon and iodine set to " + fmt(level)), true);
        return 1;
    }

    private static int fuel(CommandSourceStack source, BlockPos pos, double remaining) {
        if (!(source.getLevel().getBlockEntity(pos) instanceof ReactorControllerBlockEntity c)) {
            source.sendFailure(Component.literal("No reactor controller at " + pos.toShortString()));
            return 0;
        }
        c.revalidate();
        int n = 0;
        if (remaining < 0) {
            n = c.core().fillFresh();
        } else {
            for (Map.Entry<Long, FuelState> e : new ArrayList<>(c.core().assemblies().entrySet())) {
                c.core().setAssembly(e.getKey(), e.getValue().withRemaining(remaining));
                n++;
            }
        }
        c.setChanged();
        int count = n;
        source.sendSuccess(() -> Component.literal((remaining < 0 ? "Loaded " : "Set remaining fuel on ") + count + " assemblies"), true);
        return n;
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
        int exchangers = 0;
        int others = 0;
        double sunk = 0;
        long lostVolume = 0;
        int ruptures = 0;
        for (BlockEntity be : all) {
            if (be instanceof ThermalNode node && node.sinks() != null) {
                sunk += node.sinks().totalHeat();
                lostVolume += node.sinks().totalVolume();
                ruptures += node.sinks().ruptures;
            }
            if (be instanceof ControlRodDriveBlockEntity d) {
                source.sendSuccess(() -> Component.literal("rod drive " + where(d) + " insertion=" + fmt(d.insertion()) + " target=" + fmt(d.target()) + " failSafe=" + d.failSafe() + " speed=" + d.getSpeed()), false);
                continue;
            }
            if (be instanceof BufferNode n) {
                others++;
                coolant += n.coolant().amount();
                stored += n.coolant().heat() + n.extraStoredHeat();
                added += n.heatSourced();
                source.sendSuccess(() -> Component.literal(be.getBlockState().getBlock().getName().getString() + " " + where(be) + " " + n.coolant().amount() + "mB "
                        + fmt(n.coolant().temperature()) + "C p=" + fmt(n.coolant().pressure()) + " status=" + n.status() + " sunk=" + fmt(n.sinks().totalHeat())), false);
                continue;
            }
            if (be instanceof ReactorControllerBlockEntity c) {
                controllers++;
                coolant += c.coolant().amount();
                stored += c.coolant().heat() + c.core().storedHeat();
                added += c.heatAdded();
                source.sendSuccess(() -> Component.literal("controller " + where(c) + " status=" + c.status() + " coolant=" + c.coolant().amount() + " heat=" + fmt(c.coolant().heat())
                        + " temp=" + fmt(c.coolant().temperature()) + " core=" + fmt(c.core().storedHeat()) + " coreMax=" + fmt(c.core().maxTemperature())
                        + " hotSpots=" + c.core().hotSpots() + " added=" + fmt(c.heatAdded()) + " insertion=" + fmt(c.insertion()) + " flow=" + c.effectiveFlow()
                        + " problems=" + c.problemPositions().size() + " warnings=" + c.warningPositions().size() + " missing=" + c.missing().size() + " fuel=" + c.core().assemblies().size() + " active=" + c.core().activeAssemblies()), false);
                source.sendSuccess(() -> Component.literal("  trip=" + c.tripReason() + " neutron=" + String.format(java.util.Locale.ROOT, "%.3e", c.core().neutronPower()) + " rho=" + fmt(c.core().reactivity() * 1e5) + "pcm"
                        + " fuel=" + fmt(c.core().fuelReactivity() * 1e5) + " rods=" + fmt(c.core().rodReactivity() * 1e5) + " temp=" + fmt(c.core().temperatureReactivity() * 1e5)
                        + " xenon=" + fmt(c.core().xenonReactivity() * 1e5) + " boron=" + fmt(c.core().boronReactivity() * 1e5) + " period=" + fmt(c.core().period())
                        + " xe=" + fmt(c.core().xenon()) + " iodine=" + fmt(c.core().iodine()) + " ppm=" + fmt(c.boron())), false);
            } else if (be instanceof SteamTurbineBlockEntity t) {
                turbines++;
                coolant += t.coolant().amount();
                stored += t.coolant().heat();
                extracted += t.heatExtracted();
                source.sendSuccess(() -> Component.literal("turbine " + where(t) + " coolant=" + t.coolant().amount() + " heat=" + fmt(t.coolant().heat())
                        + " extracted=" + fmt(t.heatExtracted()) + " capacity=" + fmt(t.stressCapacity()) + " speed=" + t.getSpeed()), false);
            } else if (be instanceof HeatExchangerBlockEntity x) {
                exchangers++;
                for (CoolantBuffer b : x.buffers()) {
                    coolant += b.amount();
                    stored += b.heat();
                }
                source.sendSuccess(() -> Component.literal("exchanger " + where(x) + " primary=" + x.primary().amount() + "/" + fmt(x.primary().temperature())
                        + "C secondary=" + x.secondary().amount() + "/" + fmt(x.secondary().temperature()) + "C rate=" + fmt(x.lastExchange()) + " total=" + fmt(x.exchanged())), false);
            } else if (be instanceof CirculationPumpBlockEntity p) {
                pumps++;
                for (ThermalNode pipe : p.pipeContents()) {
                    coolant += pipe.coolant().amount();
                    stored += pipe.coolant().heat();
                    if (pipe.sinks() != null) {
                        sunk += pipe.sinks().totalHeat();
                        lostVolume += pipe.sinks().totalVolume();
                    }
                }
                source.sendSuccess(() -> Component.literal("pump " + where(p) + " flow=" + p.flow() + " moved=" + p.movedTotal() + " speed=" + p.getSpeed()
                        + " status=" + p.status()), false);
            } else if (be instanceof ReinforcedValveBlockEntity v) {
                others++;
                coolant += v.coolant().amount();
                stored += v.coolant().heat();
                source.sendSuccess(() -> Component.literal("valve " + where(v) + " " + v.coolant().amount() + "mB open=" + ReinforcedValveBlock.isOpen(v.getBlockState())), false);
            }
        }
        double balance = added - stored - extracted - sunk;
        String summary = "totals controllers=" + controllers + " pumps=" + pumps + " turbines=" + turbines + " exchangers=" + exchangers + " other=" + others + " coolant=" + coolant
                + " stored=" + fmt(stored) + " added=" + fmt(added) + " extracted=" + fmt(extracted) + " sunk=" + fmt(sunk) + " lostVolume=" + lostVolume + " ruptures=" + ruptures + " balance=" + fmt(balance);
        source.sendSuccess(() -> Component.literal(summary), false);
        return controllers + pumps + turbines;
    }
}
