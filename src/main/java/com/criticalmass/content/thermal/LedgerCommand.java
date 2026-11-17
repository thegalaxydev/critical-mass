package com.criticalmass.content.thermal;

import com.criticalmass.content.fuel.FuelState;
import com.criticalmass.content.power.BreakerBlockEntity;
import com.criticalmass.content.power.Power;
import com.criticalmass.content.power.PowerNetwork;
import com.criticalmass.content.power.GridTieBlockEntity;
import com.criticalmass.content.power.GeneratorBlockEntity;
import com.criticalmass.content.power.WireLinks;
import com.criticalmass.content.power.WireType;
import com.criticalmass.content.radiation.ContaminationData;
import com.criticalmass.content.radiation.Radiation;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.ControlRodDriveBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.criticalmass.content.control.DashPart;
import com.criticalmass.content.control.DashboardBlockEntity;
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
                .then(Commands.literal("desk").then(Commands.argument("desk", BlockPosArgument.blockPos()).then(Commands.argument("slot", IntegerArgumentType.integer(0, 1))
                        .then(Commands.argument("part", StringArgumentType.word()).then(Commands.argument("target", BlockPosArgument.blockPos())
                                .then(Commands.argument("mode", IntegerArgumentType.integer(0)).then(Commands.argument("color", IntegerArgumentType.integer(0, 34))
                                        .executes(ctx -> desk(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "desk"), IntegerArgumentType.getInteger(ctx, "slot"),
                                                StringArgumentType.getString(ctx, "part"), BlockPosArgument.getLoadedBlockPos(ctx, "target"), IntegerArgumentType.getInteger(ctx, "mode"),
                                                IntegerArgumentType.getInteger(ctx, "color"), ""))
                                        .then(Commands.argument("label", StringArgumentType.greedyString())
                                                .executes(ctx -> desk(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "desk"), IntegerArgumentType.getInteger(ctx, "slot"),
                                                        StringArgumentType.getString(ctx, "part"), BlockPosArgument.getLoadedBlockPos(ctx, "target"), IntegerArgumentType.getInteger(ctx, "mode"),
                                                        IntegerArgumentType.getInteger(ctx, "color"), StringArgumentType.getString(ctx, "label")))))))))))
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
                .then(Commands.literal("generator").then(Commands.argument("generator", BlockPosArgument.blockPos()).then(Commands.argument("load", DoubleArgumentType.doubleArg(0, 1))
                        .executes(ctx -> generator(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "generator"), DoubleArgumentType.getDouble(ctx, "load"))))))
                .then(Commands.literal("wire").then(Commands.argument("from", BlockPosArgument.blockPos()).then(Commands.argument("fromTerminal", IntegerArgumentType.integer(0, 2))
                        .then(Commands.argument("to", BlockPosArgument.blockPos()).then(Commands.argument("toTerminal", IntegerArgumentType.integer(0, 2)).then(Commands.argument("kind", StringArgumentType.word())
                                .executes(ctx -> wire(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "from"), IntegerArgumentType.getInteger(ctx, "fromTerminal"),
                                        BlockPosArgument.getLoadedBlockPos(ctx, "to"), IntegerArgumentType.getInteger(ctx, "toTerminal"), StringArgumentType.getString(ctx, "kind")))))))))
                .then(Commands.literal("breaker").then(Commands.argument("breaker", BlockPosArgument.blockPos())
                        .then(Commands.literal("close").executes(ctx -> breaker(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "breaker"), true)))
                        .then(Commands.literal("open").executes(ctx -> breaker(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "breaker"), false)))
                        .then(Commands.literal("reset").executes(ctx -> reset(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "breaker"))))))
                .then(Commands.literal("grid").then(Commands.argument("tie", BlockPosArgument.blockPos())
                        .then(Commands.literal("normal").executes(ctx -> grid(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "tie"), GridTieBlockEntity.Event.NORMAL, 0)))
                        .then(Commands.literal("dip").then(Commands.argument("seconds", IntegerArgumentType.integer(1, 600))
                                .executes(ctx -> grid(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "tie"), GridTieBlockEntity.Event.DIP, IntegerArgumentType.getInteger(ctx, "seconds")))))
                        .then(Commands.literal("swing").then(Commands.argument("seconds", IntegerArgumentType.integer(1, 600))
                                .executes(ctx -> grid(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "tie"), GridTieBlockEntity.Event.SWING, IntegerArgumentType.getInteger(ctx, "seconds")))))
                        .then(Commands.literal("outage").then(Commands.argument("seconds", IntegerArgumentType.integer(1, 600))
                                .executes(ctx -> grid(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "tie"), GridTieBlockEntity.Event.OUTAGE, IntegerArgumentType.getInteger(ctx, "seconds")))))))
                .then(Commands.literal("strike").then(Commands.argument("near", BlockPosArgument.blockPos()).executes(ctx -> strike(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "near")))))
                .then(Commands.literal("xenon").then(Commands.argument("controller", BlockPosArgument.blockPos()).then(Commands.argument("level", DoubleArgumentType.doubleArg(0, 3))
                        .executes(ctx -> xenon(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "controller"), DoubleArgumentType.getDouble(ctx, "level"))))))
                .then(Commands.literal("fuel").then(Commands.argument("controller", BlockPosArgument.blockPos())
                        .then(Commands.literal("fill").executes(ctx -> fuel(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "controller"), -1)))
                        .then(Commands.literal("burn").then(Commands.argument("remaining", DoubleArgumentType.doubleArg(0, 1))
                                .executes(ctx -> fuel(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "controller"), DoubleArgumentType.getDouble(ctx, "remaining"))))))));
    }

    private static int desk(CommandSourceStack source, BlockPos pos, int slot, String kind, BlockPos target, int mode, int color, String label) {
        if (!(source.getLevel().getBlockEntity(pos) instanceof DashboardBlockEntity desk)) {
            source.sendFailure(Component.literal("No dashboard at " + pos.toShortString()));
            return 0;
        }
        DashPart.Type type = DashPart.Type.byKey(kind);
        if (!type.key.equals(kind)) {
            source.sendFailure(Component.literal("Parts are button, switch, lever, gauge and alarm"));
            return 0;
        }
        desk.remove(slot);
        DashPart part = new DashPart(type);
        desk.install(slot, part);
        if (!desk.bind(slot, target)) {
            source.sendFailure(Component.literal("A " + kind + " cannot use the block at " + target.toShortString()));
            return 0;
        }
        desk.edit(slot, part.body, color, part.style, part.options, part.face, mode, false, label);
        if (type == DashPart.Type.GAUGE) {
            desk.edit(slot, part.body, part.color, part.style, part.options, DashPart.face(part.channel(source.getLevel())), part.mode, false, label);
        }
        source.sendSuccess(() -> Component.literal(kind + " fitted: " + desk.modeKey(part)), false);
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

    private static int generator(CommandSourceStack source, BlockPos pos, double load) {
        if (!(source.getLevel().getBlockEntity(pos) instanceof GeneratorBlockEntity section)) {
            source.sendFailure(Component.translatable("criticalmass.command.no_generator"));
            return 0;
        }
        GeneratorBlockEntity g = section.master();
        g.setLoad(load);
        source.sendSuccess(() -> Component.translatable("criticalmass.command.generator", Math.round(g.load() * 100)), true);
        return 1;
    }

    private static int wire(CommandSourceStack source, BlockPos from, int fromTerminal, BlockPos to, int toTerminal, String kind) {
        WireType type = switch (kind) {
            case "wire" -> WireType.COPPER;
            case "cable" -> WireType.CABLE;
            case "line" -> WireType.LINE;
            default -> null;
        };
        if (type == null) {
            source.sendFailure(Component.translatable("criticalmass.command.wire_kind"));
            return 0;
        }
        String problem = WireLinks.connect(source.getLevel(), from, fromTerminal, to, toTerminal, type);
        if (!problem.isEmpty()) {
            source.sendFailure(Component.translatable(problem));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("criticalmass.wire.joined"), false);
        return 1;
    }

    private static int grid(CommandSourceStack source, BlockPos pos, GridTieBlockEntity.Event event, int seconds) {
        if (!(source.getLevel().getBlockEntity(pos) instanceof GridTieBlockEntity tie)) {
            source.sendFailure(Component.translatable("criticalmass.command.no_grid"));
            return 0;
        }
        tie.disturb(event, seconds * 20, event == GridTieBlockEntity.Event.DIP ? Power.DIP_FLOOR : event == GridTieBlockEntity.Event.SWING ? Power.SWING_HZ : 0);
        source.sendSuccess(() -> Component.translatable("criticalmass.command.grid." + event.name().toLowerCase(java.util.Locale.ROOT)), true);
        return 1;
    }

    private static int strike(CommandSourceStack source, BlockPos pos) {
        if (!PowerNetwork.strikeNear(source.getLevel(), net.minecraft.world.phys.Vec3.atCenterOf(pos), 6.0, false)) {
            source.sendFailure(Component.translatable("criticalmass.command.no_wire_near"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("criticalmass.command.struck"), true);
        return 1;
    }

    private static int breaker(CommandSourceStack source, BlockPos pos, boolean close) {
        if (!(source.getLevel().getBlockEntity(pos) instanceof BreakerBlockEntity b)) {
            source.sendFailure(Component.translatable("criticalmass.command.no_breaker"));
            return 0;
        }
        String result = close ? b.close() : b.open();
        boolean done = b.closed() == close;
        if (done) {
            source.sendSuccess(() -> Component.translatable(result), true);
        } else {
            source.sendFailure(Component.translatable(result));
        }
        return done ? 1 : 0;
    }

    private static int reset(CommandSourceStack source, BlockPos pos) {
        if (!(source.getLevel().getBlockEntity(pos) instanceof BreakerBlockEntity b)) {
            source.sendFailure(Component.translatable("criticalmass.command.no_breaker"));
            return 0;
        }
        String result = b.reset();
        source.sendSuccess(() -> Component.translatable(result), true);
        return 1;
    }
}
