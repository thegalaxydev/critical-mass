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
        public int wires;

        public Side side(Level level) {
            if (!grids.isEmpty()) {
                return new Side(true, Power.GRID_HZ, 0, true);
            }
            for (BlockPos pos : generators) {
                if (level.getBlockEntity(pos) instanceof GeneratorBlockEntity g && g.energised()) {
                    return new Side(true, g.frequency(), g.phase(), false);
                }
            }
            return Side.DEAD;
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
            if (link.mine() == terminal) {
                island.wires++;
                queue.add(new Step(link.other(), link.theirs()));
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
            if (!seen.add(step)) {
                continue;
            }
            WireNode node = WireLinks.node(level, step.pos());
            if (node == null) {
                continue;
            }
            BlockState state = level.getBlockState(step.pos());
            Block block = state.getBlock();
            if (block instanceof GeneratorBlock) {
                BlockPos master = GeneratorBlockEntity.masterPos(level, step.pos());
                if (island.generators.add(master) && (!stopAtDevices || origin)) {
                    for (BlockPos section : GeneratorBlockEntity.sections(level, master)) {
                        seen.add(new Step(section, 0));
                        WireNode part = WireLinks.node(level, section);
                        if (part != null) {
                            follow(island, part, 0, queue);
                        }
                    }
                }
            } else if (block instanceof GridTieBlock) {
                island.grids.add(step.pos());
                if (!stopAtDevices) {
                    follow(island, node, step.terminal(), queue);
                }
            } else if (block instanceof BreakerBlock) {
                if (stopAtDevices) {
                    island.breakers.add(step.pos());
                    continue;
                }
                follow(island, node, step.terminal(), queue);
                if (!step.pos().equals(held) && state.getValue(BreakerBlock.CLOSED)) {
                    island.breakers.add(step.pos());
                    queue.add(new Step(step.pos(), 1 - step.terminal()));
                }
            } else {
                follow(island, node, step.terminal(), queue);
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
