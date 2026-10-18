package com.criticalmass.content;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CMShapes {
    private static final Map<VoxelShape, Map<Direction, VoxelShape>> CACHE = new IdentityHashMap<>();

    private CMShapes() {
    }

    public static synchronized VoxelShape rotated(VoxelShape north, Direction facing) {
        return CACHE.computeIfAbsent(north, k -> new EnumMap<>(Direction.class))
                .computeIfAbsent(facing, f -> build(north, f));
    }

    private static VoxelShape build(VoxelShape north, Direction facing) {
        VoxelShape out = Shapes.empty();
        for (AABB box : north.toAabbs()) {
            out = Shapes.or(out, Shapes.create(rotate(box, facing)));
        }
        return out.optimize();
    }

    private static AABB rotate(AABB b, Direction facing) {
        double[] a = point(b.minX, b.minY, b.minZ, facing);
        double[] c = point(b.maxX, b.maxY, b.maxZ, facing);
        return new AABB(Math.min(a[0], c[0]), Math.min(a[1], c[1]), Math.min(a[2], c[2]),
                Math.max(a[0], c[0]), Math.max(a[1], c[1]), Math.max(a[2], c[2]));
    }

    private static double[] point(double x, double y, double z, Direction facing) {
        return switch (facing) {
            case NORTH -> new double[] {x, y, z};
            case SOUTH -> new double[] {1 - x, y, 1 - z};
            case EAST -> new double[] {1 - z, y, x};
            case WEST -> new double[] {z, y, 1 - x};
            case UP -> new double[] {x, 1 - z, y};
            case DOWN -> new double[] {x, z, 1 - y};
        };
    }
}
