package com.criticalmass.content.control;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.VesselLookup;
import com.criticalmass.registry.CMBlockEntities;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PlantDisplaySources {
    public static final DeferredRegister<DisplaySource> SOURCES = DeferredRegister.create(CreateRegistries.DISPLAY_SOURCE, CriticalMass.MODID);
    public static final DeferredRegister<DisplayTarget> TARGETS = DeferredRegister.create(CreateRegistries.DISPLAY_TARGET, CriticalMass.MODID);
    public static final Supplier<ScreenBlockEntity.Target> SCREEN = TARGETS.register("plant_screen", ScreenBlockEntity.Target::new);
    public static final Map<PlantChannel, Supplier<Reading>> CHANNELS = new EnumMap<>(PlantChannel.class);
    public static final Supplier<Reading> ROD_INSERTION = SOURCES.register("rod_insertion", () -> new Reading(PlantChannel.RODS_A, "criticalmass.display_source.rod_insertion"));
    public static final Supplier<Status> STATUS = SOURCES.register("reactor_status", () -> new Status(true,
            PlantChannel.TEMPERATURE, PlantChannel.COOLANT_TEMPERATURE, PlantChannel.PRESSURE, PlantChannel.FLOW, PlantChannel.POWER));
    public static final Supplier<Alarms> ALARMS = SOURCES.register("reactor_alarms", Alarms::new);
    public static final Supplier<Status> NEUTRONICS = SOURCES.register("reactor_neutronics", () -> new Status(false,
            PlantChannel.NEUTRON, PlantChannel.PERIOD, PlantChannel.REACTIVITY, PlantChannel.XENON, PlantChannel.BORON));
    private static final int REFRESH_TICKS = 20;

    static {
        for (PlantChannel c : PlantChannel.values()) {
            CHANNELS.put(c, SOURCES.register(c.key, () -> new Reading(c, c.langKey())));
        }
    }

    private PlantDisplaySources() {
    }

    public static void attach() {
        DisplayTarget.BY_BLOCK.register(CMBlocks.PLANT_SCREEN.get(), SCREEN.get());
        PlantChannel[] buffer = {PlantChannel.TEMPERATURE, PlantChannel.PRESSURE, PlantChannel.LEVEL};
        PlantChannel[] reactor = {PlantChannel.TEMPERATURE, PlantChannel.COOLANT_TEMPERATURE, PlantChannel.PRESSURE, PlantChannel.LEVEL, PlantChannel.FLOW,
                PlantChannel.POWER, PlantChannel.RODS_A, PlantChannel.RODS_B, PlantChannel.RODS_C, PlantChannel.RODS_D,
                PlantChannel.NEUTRON, PlantChannel.PERIOD, PlantChannel.REACTIVITY, PlantChannel.XENON, PlantChannel.BORON};
        DisplaySource.BY_BLOCK_ENTITY.add(CMBlockEntities.REACTOR_CONTROLLER.get(), STATUS.get());
        DisplaySource.BY_BLOCK_ENTITY.add(CMBlockEntities.REACTOR_CONTROLLER.get(), ALARMS.get());
        DisplaySource.BY_BLOCK_ENTITY.add(CMBlockEntities.REACTOR_CONTROLLER.get(), NEUTRONICS.get());
        DisplaySource.BY_BLOCK.add(CMBlocks.REACTOR_SIGNAL_INTERFACE.get(), NEUTRONICS.get());
        add(CMBlockEntities.REACTOR_CONTROLLER.get(), reactor);
        DisplaySource.BY_BLOCK.add(CMBlocks.REACTOR_SIGNAL_INTERFACE.get(), STATUS.get());
        DisplaySource.BY_BLOCK.add(CMBlocks.REACTOR_SIGNAL_INTERFACE.get(), ALARMS.get());
        for (PlantChannel c : reactor) {
            DisplaySource.BY_BLOCK.add(CMBlocks.REACTOR_SIGNAL_INTERFACE.get(), CHANNELS.get(c).get());
        }
        add(CMBlockEntities.PRESSURIZER.get(), PlantChannel.PRESSURE);
        add(CMBlockEntities.TOWER_FAN.get(), PlantChannel.SPEED, PlantChannel.FLOW);
        add(CMBlockEntities.STEAM_TURBINE.get(), PlantChannel.SPEED, PlantChannel.POWER, PlantChannel.OUTPUT, PlantChannel.FLOW, PlantChannel.TEMPERATURE, PlantChannel.PRESSURE, PlantChannel.LEVEL);
        add(CMBlockEntities.CIRCULATION_PUMP.get(), PlantChannel.FLOW, PlantChannel.SPEED);
        DisplaySource.BY_BLOCK_ENTITY.add(CMBlockEntities.CONTROL_ROD_DRIVE.get(), ROD_INSERTION.get());
        add(CMBlockEntities.HEAT_EXCHANGER.get(), buffer);
        add(CMBlockEntities.CONDENSER.get(), buffer);
        add(CMBlockEntities.COOLING_TOWER.get(), buffer);
        add(CMBlockEntities.RADIATOR.get(), buffer);
        add(CMBlockEntities.RELIEF_VALVE.get(), buffer);
        add(CMBlockEntities.EMERGENCY_TANK.get(), buffer);
        add(CMBlockEntities.TOWER_BASIN.get(), buffer);
        add(CMBlockEntities.TOWER_DISTRIBUTOR.get(), buffer);
        add(CMBlockEntities.FEEDWATER_TANK.get(), buffer);
    }

    private static void add(BlockEntityType<?> type, PlantChannel... channels) {
        for (PlantChannel c : channels) {
            DisplaySource.BY_BLOCK_ENTITY.add(type, CHANNELS.get(c).get());
        }
    }

    private static BlockPos resolve(DisplayLinkContext context) {
        BlockPos pos = context.getSourcePos();
        if (context.level().getBlockState(pos).is(CMBlocks.REACTOR_SIGNAL_INTERFACE.get())) {
            VesselLookup.Result vessel = VesselLookup.findController(context.level(), pos);
            return vessel.ok() ? vessel.controller() : pos;
        }
        return pos;
    }

    public static String number(double v) {
        double a = Math.abs(v);
        if (a > 0 && a < 0.01) {
            return String.format(Locale.ROOT, "%.1e", v);
        }
        return a >= 100 ? String.valueOf(Math.round(v)) : a >= 10 ? String.format(Locale.ROOT, "%.1f", v) : String.format(Locale.ROOT, "%.2f", v);
    }

    private static String value(PlantChannel.Reading r) {
        return r == null ? "--" : number(r.value()) + (r.unit().isEmpty() ? "" : " " + r.unit());
    }

    public static class Reading extends SingleLineDisplaySource {
        private final PlantChannel channel;
        private final String nameKey;

        public Reading(PlantChannel channel, String nameKey) {
            this.channel = channel;
            this.nameKey = nameKey;
        }

        public PlantChannel channel() {
            return channel;
        }

        @Override
        protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
            return Component.literal(value(channel.read(context.level(), resolve(context))));
        }

        @Override
        protected boolean allowsLabeling(DisplayLinkContext context) {
            return true;
        }

        @Override
        public Component getName() {
            return Component.translatable(nameKey);
        }

        @Override
        public int getPassiveRefreshTicks() {
            return REFRESH_TICKS;
        }
    }

    public static class Status extends DisplaySource {
        private final boolean headline;
        private final PlantChannel[] lines;

        public Status(boolean headline, PlantChannel... lines) {
            this.headline = headline;
            this.lines = lines;
        }

        @Override
        public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
            BlockPos source = resolve(context);
            if (!(context.level().getBlockEntity(source) instanceof ReactorControllerBlockEntity c)) {
                return EMPTY;
            }
            List<MutableComponent> lines = new ArrayList<>();
            if (headline) {
                lines.add(c.isTripped() && !c.tripReason().isEmpty() ? Component.translatable("criticalmass.reactor.trip." + c.tripReason() + ".short") : Component.translatable(c.status() + ".short"));
            }
            for (PlantChannel channel : this.lines) {
                if (lines.size() >= stats.maxRows()) {
                    break;
                }
                PlantChannel.Reading r = channel.read(context.level(), source);
                if (r != null) {
                    lines.add(Component.translatable(channel.langKey() + ".short").append(" " + value(r)));
                }
            }
            return lines;
        }

        @Override
        public int getPassiveRefreshTicks() {
            return REFRESH_TICKS;
        }
    }

    public static class Alarms extends DisplaySource {
        @Override
        public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
            BlockPos source = resolve(context);
            if (!(context.level().getBlockEntity(source) instanceof ReactorControllerBlockEntity c)) {
                return EMPTY;
            }
            int mask = PlantAlarms.evaluate(c);
            List<MutableComponent> lines = new ArrayList<>();
            for (PlantAlarms.Alarm a : PlantAlarms.Alarm.values()) {
                if ((mask & 1 << a.ordinal()) != 0 && lines.size() < stats.maxRows()) {
                    lines.add(Component.translatable(a.langKey()));
                }
            }
            if (lines.isEmpty()) {
                lines.add(Component.translatable("criticalmass.display_source.no_alarms"));
            }
            return lines;
        }

        @Override
        public int getPassiveRefreshTicks() {
            return REFRESH_TICKS;
        }
    }
}
