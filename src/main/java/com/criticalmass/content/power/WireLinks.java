package com.criticalmass.content.power;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class WireLinks {
    public static final double MAX_LENGTH = 24.0;
    public static final int MAX_PER_TERMINAL = 4;

    public record Link(BlockPos other, int mine, int theirs) {
    }

    private final List<Link> links = new ArrayList<>();

    public List<Link> all() {
        return links;
    }

    public boolean isEmpty() {
        return links.isEmpty();
    }

    public int count(int terminal) {
        int n = 0;
        for (Link link : links) {
            if (link.mine() == terminal) {
                n++;
            }
        }
        return n;
    }

    public boolean has(BlockPos other, int mine, int theirs) {
        return links.contains(new Link(other, mine, theirs));
    }

    public ListTag save() {
        ListTag list = new ListTag();
        for (Link link : links) {
            CompoundTag tag = new CompoundTag();
            tag.putIntArray("To", new int[]{link.other().getX(), link.other().getY(), link.other().getZ()});
            tag.putInt("Mine", link.mine());
            tag.putInt("Theirs", link.theirs());
            list.add(tag);
        }
        return list;
    }

    public void load(CompoundTag from, String key) {
        links.clear();
        ListTag list = from.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            int[] to = tag.getIntArray("To");
            if (to.length == 3) {
                links.add(new Link(new BlockPos(to[0], to[1], to[2]), tag.getInt("Mine"), tag.getInt("Theirs")));
            }
        }
    }

    @Nullable
    public static WireNode node(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof WireNode node ? node : null;
    }

    public static String connect(Level level, BlockPos a, int ta, BlockPos b, int tb) {
        WireNode from = node(level, a);
        WireNode to = node(level, b);
        if (from == null || to == null) {
            return "criticalmass.wire.gone";
        }
        if (a.equals(b)) {
            return "criticalmass.wire.same";
        }
        if (Math.sqrt(a.distSqr(b)) > MAX_LENGTH) {
            return "criticalmass.wire.too_far";
        }
        if (from.wires().has(b, ta, tb)) {
            return "criticalmass.wire.already";
        }
        if (from.wires().count(ta) >= MAX_PER_TERMINAL || to.wires().count(tb) >= MAX_PER_TERMINAL) {
            return "criticalmass.wire.full";
        }
        from.wires().links.add(new Link(b.immutable(), ta, tb));
        to.wires().links.add(new Link(a.immutable(), tb, ta));
        from.wiresChanged();
        to.wiresChanged();
        PowerNetwork.changed(level);
        return "";
    }

    private static void unhook(Level level, BlockPos at, Link link) {
        WireNode other = node(level, link.other());
        if (other != null && other.wires().links.remove(new Link(at, link.theirs(), link.mine()))) {
            other.wiresChanged();
        }
    }

    public static int cutAll(Level level, BlockPos pos) {
        WireNode node = node(level, pos);
        if (node == null || node.wires().isEmpty()) {
            return 0;
        }
        List<Link> mine = new ArrayList<>(node.wires().links);
        node.wires().links.clear();
        for (Link link : mine) {
            unhook(level, pos, link);
        }
        node.wiresChanged();
        PowerNetwork.changed(level);
        return mine.size();
    }

    public static boolean cutLast(Level level, BlockPos pos, int terminal) {
        WireNode node = node(level, pos);
        if (node == null) {
            return false;
        }
        List<Link> mine = node.wires().links;
        for (int i = mine.size() - 1; i >= 0; i--) {
            if (mine.get(i).mine() == terminal) {
                Link link = mine.remove(i);
                unhook(level, pos, link);
                node.wiresChanged();
                PowerNetwork.changed(level);
                return true;
            }
        }
        return false;
    }
}
