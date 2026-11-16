package com.criticalmass.content.power;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class PowerNetwork {
    private static final Map<Level, int[]> VERSIONS = new WeakHashMap<>();
    private static final int ANY = -1;

    private PowerNetwork() {
    }

    public static synchronized int version(Level level) {
        return VERSIONS.computeIfAbsent(level, k -> new int[1])[0];
    }

    public static synchronized void changed(Level level) {
        VERSIONS.computeIfAbsent(level, k -> new int[1])[0]++;
    }

    public record Side(boolean live, double frequency, double phase, boolean grid) {
        public static final Side DEAD = new Side(false, 0, 0, false);
    }

    public static final class Island {
        public final Set<BlockPos> generators = new LinkedHashSet<>();
        public final Set<BlockPos> grids = new LinkedHashSet<>();
        public final Set<BlockPos> breakers = new LinkedHashSet<>();
        public final Set<BlockPos> loads = new LinkedHashSet<>();
        public int wires;

        public Side side(Level level) {
            if (!grids.isEmpty()) {
                return new Side(true, Power.GRID_HZ, 0, true);
            }
            GeneratorBlockEntity lead = lead(level, null);
            return lead == null ? Side.DEAD : new Side(true, lead.frequency(), lead.phase(), false);
        }

        @Nullable
        public GeneratorBlockEntity lead(Level level, @Nullable GeneratorBlockEntity except) {
            GeneratorBlockEntity best = null;
            for (BlockPos pos : generators) {
                if (level.getBlockEntity(pos) instanceof GeneratorBlockEntity g && g != except && g.online()
                        && (best == null || pos.compareTo(best.getBlockPos()) < 0)) {
                    best = g;
                }
            }
            return best;
        }

        public double onlineRating(Level level, @Nullable GeneratorBlockEntity except) {
            double total = 0;
            for (GeneratorBlockEntity g : machines(level)) {
                if (g != except && g.online()) {
                    total += g.ratedSu();
                }
            }
            return total;
        }

        public double demand(Level level) {
            double total = 0;
            for (BlockPos pos : loads) {
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PowerLoad load) {
                    total += load.demandSu();
                }
            }
            return total;
        }

        public List<GeneratorBlockEntity> machines(Level level) {
            List<GeneratorBlockEntity> out = new ArrayList<>();
            for (BlockPos pos : generators) {
                if (level.getBlockEntity(pos) instanceof GeneratorBlockEntity g) {
                    out.add(g);
                }
            }
            return out;
        }
    }

    private record Step(BlockPos pos, int terminal) {
    }

    public static Island scan(Level level, BlockPos start) {
        return scan(level, new Step(start, 0), null, false);
    }

    public static Island scanSide(Level level, BlockPos breaker, int terminal) {
        return scan(level, new Step(breaker, terminal), breaker, false);
    }

    private static void follow(Island island, WireNode node, int terminal, ArrayDeque<Step> queue) {
        for (WireLinks.Link link : node.wires().all()) {
            if (terminal == ANY || link.mine() == terminal) {
                island.wires++;
                queue.add(new Step(link.other(), link.theirs()));
            }
        }
    }

    private static void followRun(Level level, Island island, List<BlockPos> run, Set<Step> seen, ArrayDeque<Step> queue) {
        for (BlockPos part : run) {
            seen.add(new Step(part, ANY));
            WireNode node = WireLinks.node(level, part);
            if (node != null) {
                follow(island, node, ANY, queue);
            }
        }
    }

    private static Island scan(Level level, Step start, @Nullable BlockPos held, boolean stopAtDevices) {
        Island island = new Island();
        Set<Step> seen = new HashSet<>();
        ArrayDeque<Step> queue = new ArrayDeque<>();
        queue.add(start);
        boolean first = true;
        while (!queue.isEmpty() && seen.size() < Power.SCAN_LIMIT) {
            Step step = queue.poll();
            boolean origin = first;
            first = false;
            WireNode node = WireLinks.node(level, step.pos());
            if (node == null) {
                continue;
            }
            BlockState state = level.getBlockState(step.pos());
            Block block = state.getBlock();
            if (block instanceof BreakerBlock) {
                if (!seen.add(step)) {
                    continue;
                }
                if (stopAtDevices) {
                    island.breakers.add(step.pos());
                    continue;
                }
                follow(island, node, step.terminal(), queue);
                if (!step.pos().equals(held) && state.getValue(BreakerBlock.CLOSED)) {
                    island.breakers.add(step.pos());
                    queue.add(new Step(step.pos(), 1 - step.terminal()));
                }
                continue;
            }
            if (seen.contains(new Step(step.pos(), ANY))) {
                continue;
            }
            if (block instanceof GeneratorBlock) {
                BlockPos master = GeneratorBlockEntity.masterPos(level, step.pos());
                if (island.generators.add(master) && (!stopAtDevices || origin)) {
                    followRun(level, island, GeneratorBlockEntity.sections(level, master), seen, queue);
                } else {
                    seen.add(new Step(step.pos(), ANY));
                }
            } else if (block instanceof BusBarBlock) {
                followRun(level, island, BusBarBlock.run(level, step.pos()), seen, queue);
            } else {
                seen.add(new Step(step.pos(), ANY));
                if (block instanceof GridTieBlock) {
                    island.grids.add(step.pos());
                    if (stopAtDevices) {
                        continue;
                    }
                } else if (node instanceof PowerLoad) {
                    island.loads.add(step.pos());
                }
                follow(island, node, ANY, queue);
            }
        }
        return island;
    }

    public static boolean tripNearest(Level level, BlockPos machine, String reason) {
        Island reach = scan(level, new Step(machine, 0), null, true);
        boolean any = false;
        for (BlockPos pos : reach.breakers) {
            if (level.getBlockEntity(pos) instanceof BreakerBlockEntity breaker && breaker.closed()) {
                breaker.trip(reason);
                any = true;
            }
        }
        return any;
    }
}
