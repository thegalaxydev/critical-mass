package com.criticalmass.content.control;

import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.power.BreakerBlockEntity;
import com.criticalmass.content.power.BusBarBlockEntity;
import com.criticalmass.content.power.GeneratorBlockEntity;
import com.criticalmass.content.power.GridTieBlockEntity;
import com.criticalmass.content.power.MotorBlockEntity;
import com.criticalmass.content.power.Power;
import com.criticalmass.content.power.TransformerBlockEntity;
import com.criticalmass.content.kinetics.ControlRodDriveBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantBuffer;
import com.criticalmass.content.thermal.PressurizerBlockEntity;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.CoolingTowerBlockEntity;
import com.criticalmass.content.thermal.ThermalNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public enum PlantChannel {
    TEMPERATURE("temperature", "°C"),
    COOLANT_TEMPERATURE("coolant_temperature", "°C"),
    PRESSURE("pressure", "bar"),
    LEVEL("level", "%"),
    FLOW("flow", "mB/t"),
    POWER("power", "HU/t"),
    RODS_A("rods_a", "%"),
    RODS_B("rods_b", "%"),
    RODS_C("rods_c", "%"),
    RODS_D("rods_d", "%"),
    SPEED("speed", "RPM"),
    OUTPUT("output", ""),
    NEUTRON("neutron", "%"),
    PERIOD("period", "s"),
    REACTIVITY("reactivity", "pcm"),
    XENON("xenon", "%"),
    BORON("boron", "ppm"),
    FREQUENCY("frequency", "Hz"),
    SYNC("sync", "\u00b0"),
    DEMAND("demand", "MW"),
    VOLTAGE("voltage", "kV");

    public static final double COOLANT_SCALE_MAX = 400.0;
    public static final double STABLE_PERIOD = 999.0;
    public final String key;
    public final String unit;

    PlantChannel(String key, String unit) {
        this.key = key;
        this.unit = unit;
    }

    public String langKey() {
        return "criticalmass.channel." + key;
    }

    public record Reading(double value, double min, double max, String unit) {
        public double fraction() {
            return max <= min ? 0 : Math.max(0, Math.min(1, (value - min) / (max - min)));
        }
    }

    public static boolean isTarget(@Nullable BlockEntity be) {
        return be instanceof ThermalNode || be instanceof PressurizerBlockEntity || be instanceof CirculationPumpBlockEntity || be instanceof ControlRodDriveBlockEntity
                || be instanceof GeneratorBlockEntity || be instanceof BreakerBlockEntity || be instanceof GridTieBlockEntity || be instanceof MotorBlockEntity || be instanceof BusBarBlockEntity || be instanceof TransformerBlockEntity;
    }

    private static double scale(double kilovolts) {
        return kilovolts <= Power.MOTOR_KV * Power.OVERVOLTAGE ? 8 : kilovolts <= Power.GENERATOR_KV * Power.OVERVOLTAGE ? 25 : 250;
    }

    private Reading of(double value, double min, double max) {
        return new Reading(value, min, max, unit);
    }

    @Nullable
    private Reading buffer(CoolantBuffer b) {
        return switch (this) {
            case TEMPERATURE, COOLANT_TEMPERATURE -> of(b.temperature(), CMThermal.AMBIENT, COOLANT_SCALE_MAX);
            case PRESSURE -> of(b.pressure(), 0, CMThermal.RUPTURE_PRESSURE);
            case LEVEL -> of(100.0 * b.amount() / Math.max(1, b.capacity()), 0, 100);
            default -> null;
        };
    }

    @Nullable
    public Reading read(Level level, @Nullable BlockPos target) {
        if (target == null || !level.isLoaded(target)) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(target);
        if (be instanceof ReactorControllerBlockEntity c) {
            return switch (this) {
                case TEMPERATURE -> of(Math.max(CMThermal.AMBIENT, c.core().maxTemperature()), CMThermal.AMBIENT, CMThermal.TRIP_TEMPERATURE);
                case FLOW -> of(c.effectiveFlow(), 0, CMThermal.PUMP_MAX_FLOW);
                case POWER -> of(c.core().lastOutput(), 0, Math.max(1, c.core().ratedOutput()));
                case RODS_A, RODS_B, RODS_C, RODS_D -> {
                    double v = c.bankInsertion(ordinal() - RODS_A.ordinal());
                    yield Double.isNaN(v) ? null : of(v * 100, 0, 100);
                }
                case PRESSURE -> of(c.primaryPressure(), 0, CMThermal.PRESSURIZER_MAX);
                case NEUTRON -> of(c.core().neutronPower() * 100, 0, 120);
                case PERIOD -> of(Double.isInfinite(c.core().period()) ? STABLE_PERIOD : Math.max(-STABLE_PERIOD, Math.min(STABLE_PERIOD, c.core().period())), -STABLE_PERIOD, STABLE_PERIOD);
                case REACTIVITY -> of(c.core().reactivity() * 1.0e5, -10000, 1000);
                case XENON -> of(c.core().xenon() * 100, 0, 200);
                case BORON -> of(c.boron(), 0, 2500);
                default -> buffer(c.coolant());
            };
        }
        if (be instanceof PressurizerBlockEntity z) {
            return switch (this) {
                case PRESSURE -> of(z.pressure(), 0, CMThermal.PRESSURIZER_MAX);
                case TEMPERATURE, COOLANT_TEMPERATURE -> of(PressurizerBlockEntity.boilingPoint(z.pressure()), CMThermal.AMBIENT, PressurizerBlockEntity.boilingPoint(CMThermal.PRESSURIZER_MAX));
                default -> null;
            };
        }
        if (be instanceof SteamTurbineBlockEntity t) {
            return switch (this) {
                case SPEED -> of(t.stressCapacity() > 0 ? t.ratedSpeed() * (1 + t.overspeed()) : 0, 0, t.ratedSpeed() * 1.25);
                case POWER -> of(t.heatRate() * CMThermal.SU_PER_HEAT, 0, Math.max(CMThermal.TURBINE_MIN_SU, t.stressCapacity()));
                case OUTPUT -> new Reading(t.shaftLoad(), 0, Math.max(CMThermal.TURBINE_MIN_SU, t.stressCapacity()), "SU");
                case FLOW -> of(t.admission() * 100, 0, 100);
                default -> buffer(t.coolant());
            };
        }
        if (be instanceof CirculationPumpBlockEntity p) {
            return switch (this) {
                case FLOW -> of(p.flow(), 0, CMThermal.PUMP_MAX_FLOW);
                case SPEED -> of(Math.abs(p.getSpeed()), 0, CMThermal.TURBINE_MAX_RPM);
                default -> null;
            };
        }
        if (be instanceof ControlRodDriveBlockEntity d) {
            return switch (this) {
                case RODS_A, RODS_B, RODS_C, RODS_D -> of(d.insertion() * 100, 0, 100);
                default -> null;
            };
        }
        if (be instanceof GeneratorBlockEntity section) {
            GeneratorBlockEntity g = section.master();
            return switch (this) {
                case OUTPUT -> new Reading(Power.megawatts(g.outputSu()), 0, Power.megawatts(g.ratedSu()), "MW");
                case FREQUENCY -> g.energised() ? of(g.frequency(), 45, 55) : of(0, 45, 55);
                case SYNC -> g.synced() ? of(0, -180, 180) : g.energised() ? of(g.phase(), -180, 180) : null;
                case DEMAND -> new Reading(Power.megawatts(g.load() * g.ratedSu()), 0, Power.megawatts(g.ratedSu()), "MW");
                default -> null;
            };
        }
        if (be instanceof MotorBlockEntity m) {
            return switch (this) {
                case DEMAND -> of(Power.megawatts(m.demandSu()), 0, Power.megawatts(Power.MOTOR_RATED_SU / Power.MOTOR_EFFICIENCY));
                case SPEED -> of(Math.abs(m.getSpeed()), 0, MotorBlockEntity.MAX_SPEED);
                default -> null;
            };
        }
        if (be instanceof BusBarBlockEntity bus) {
            return switch (this) {
                case FREQUENCY -> of(bus.frequency(), 45, 55);
                case VOLTAGE -> of(bus.kilovolts(), 0, scale(bus.kilovolts()));
                case DEMAND -> of(Power.megawatts(bus.loadSu()), 0, Power.megawatts(Power.SECTION_RATED_SU));
                default -> null;
            };
        }
        if (be instanceof TransformerBlockEntity t) {
            return switch (this) {
                case VOLTAGE -> of(t.lowSide(), 0, scale(t.lowKilovolts()));
                case DEMAND -> of(t.throughMw(), 0, Power.TRANSFORMER_MW * 1.5);
                case TEMPERATURE -> of(t.oilTemperature(), 0, 120);
                default -> null;
            };
        }
        if (be instanceof BreakerBlockEntity b) {
            return this == SYNC && b.bothLive() ? of(b.angle(), -180, 180) : null;
        }
        if (be instanceof GridTieBlockEntity tie) {
            double scale = Math.max(Power.ORDER_STEP_MW, tie.contractMw());
            return switch (this) {
                case DEMAND -> of(tie.orderMw(), 0, scale);
                case OUTPUT -> new Reading(tie.deliveredMw(), 0, scale, "MW");
                case FREQUENCY -> of(Power.GRID_HZ, 45, 55);
                default -> null;
            };
        }
        if (be instanceof CoolingTowerBlockEntity t) {
            return buffer(t.stackCoolant());
        }
        if (be instanceof ThermalNode n) {
            return buffer(n.coolant());
        }
        return null;
    }
}
