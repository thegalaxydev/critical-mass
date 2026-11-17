package com.criticalmass.content.power;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class WireLinks {
    public static final double MAX_LENGTH = 24.0;
    public static final int MAX_PER_TERMINAL = 4;
    public static final int GLOW_STEPS = 4;

    public record Link(BlockPos other, int mine, int theirs, WireType type) {
    }

    private final List<Link> links = new ArrayList<>();
    private final Map<Link, Double> heat = new HashMap<>();
    private final Map<Link, Integer> glow = new HashMap<>();

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

    @Nullable
    public Link find(BlockPos other, int mine, int theirs) {
        for (Link link : links) {
            if (link.other().equals(other) && link.mine() == mine && link.theirs() == theirs) {
                return link;
            }
        }
        return null;
    }

    public boolean has(BlockPos other, int mine, int theirs) {
        return find(other, mine, theirs) != null;
    }

    public double heat(Link link) {
        return heat.getOrDefault(link, 0.0);
    }

    public int glow(Link link) {
        return glow.getOrDefault(link, 0);
    }

    public boolean warm(Link link, double value) {
        double held = Math.max(0, value);
        if (held <= 0) {
            heat.remove(link);
        } else {
            heat.put(link, held);
        }
        int shown = (int) Math.min(GLOW_STEPS, Math.floor(held * GLOW_STEPS));
        Integer before = shown == 0 ? glow.remove(link) : glow.put(link, shown);
        return (before == null ? 0 : before) != shown;
    }

    public ListTag save() {
        ListTag list = new ListTag();
        for (Link link : links) {
            CompoundTag tag = new CompoundTag();
            tag.putIntArray("To", new int[]{link.other().getX(), link.other().getY(), link.other().getZ()});
            tag.putInt("Mine", link.mine());
            tag.putInt("Theirs", link.theirs());
            tag.putInt("Type", link.type().ordinal());
            int shown = glow(link);
            if (shown > 0) {
                tag.putInt("Glow", shown);
            }
            list.add(tag);
        }
        return list;
    }

    public void load(CompoundTag from, String key) {
        links.clear();
        glow.clear();
        ListTag list = from.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            int[] to = tag.getIntArray("To");
            if (to.length == 3) {
                Link link = new Link(new BlockPos(to[0], to[1], to[2]), tag.getInt("Mine"), tag.getInt("Theirs"), WireType.of(tag.getInt("Type")));
                links.add(link);
                if (tag.getInt("Glow") > 0) {
                    glow.put(link, tag.getInt("Glow"));
                }
            }
        }
        heat.keySet().retainAll(links);
    }

    @Nullable
    public static WireNode node(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof WireNode node ? node : null;
    }

    public static String connect(Level level, BlockPos a, int ta, BlockPos b, int tb, WireType type) {
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
        from.wires().links.add(new Link(b.immutable(), ta, tb, type));
        to.wires().links.add(new Link(a.immutable(), tb, ta, type));
        from.wiresChanged();
        to.wiresChanged();
        PowerNetwork.changed(level);
        return "";
    }

    private void drop(Link link) {
        links.remove(link);
        heat.remove(link);
        glow.remove(link);
    }

    private static void unhook(Level level, BlockPos at, Link link) {
        WireNode other = node(level, link.other());
        if (other != null) {
            Link back = other.wires().find(at, link.theirs(), link.mine());
            if (back != null) {
                other.wires().drop(back);
                other.wiresChanged();
            }
        }
    }

    public static List<WireType> cutAll(Level level, BlockPos pos) {
        List<WireType> cut = new ArrayList<>();
        WireNode node = node(level, pos);
        if (node == null || node.wires().isEmpty()) {
            return cut;
        }
        List<Link> mine = new ArrayList<>(node.wires().links);
        node.wires().links.clear();
        node.wires().heat.clear();
        node.wires().glow.clear();
        for (Link link : mine) {
            unhook(level, pos, link);
            cut.add(link.type());
        }
        node.wiresChanged();
        PowerNetwork.changed(level);
        return cut;
    }

    @Nullable
    public static WireType cutLast(Level level, BlockPos pos, int terminal) {
        WireNode node = node(level, pos);
        if (node == null) {
            return null;
        }
        List<Link> mine = node.wires().links;
        for (int i = mine.size() - 1; i >= 0; i--) {
            if (mine.get(i).mine() == terminal) {
                Link link = mine.get(i);
                return cut(level, pos, link) ? link.type() : null;
            }
        }
        return null;
    }

    public static void sketch(Level level, BlockPos a, int ta, BlockPos b, int tb, WireType type, int glowing) {
        if (!(level.getBlockEntity(a) instanceof WireNode from) || !(level.getBlockEntity(b) instanceof WireNode to)) {
            return;
        }
        Link there = from.wires().find(b, ta, tb);
        if (there == null) {
            there = new Link(b.immutable(), ta, tb, type);
            from.wires().links.add(there);
        }
        Link back = to.wires().find(a, tb, ta);
        if (back == null) {
            back = new Link(a.immutable(), tb, ta, type);
            to.wires().links.add(back);
        }
        if (glowing > 0) {
            from.wires().glow.put(there, glowing);
            to.wires().glow.put(back, glowing);
        } else {
            from.wires().glow.remove(there);
            to.wires().glow.remove(back);
        }
    }

    public static void erase(Level level, BlockPos a, BlockPos b) {
        if (level.getBlockEntity(a) instanceof WireNode from) {
            from.wires().links.removeIf(link -> link.other().equals(b));
            from.wires().glow.keySet().retainAll(from.wires().links);
        }
        if (level.getBlockEntity(b) instanceof WireNode to) {
            to.wires().links.removeIf(link -> link.other().equals(a));
            to.wires().glow.keySet().retainAll(to.wires().links);
        }
    }

    public static boolean cut(Level level, BlockPos pos, Link link) {
        WireNode node = node(level, pos);
        if (node == null || !node.wires().links.contains(link)) {
            return false;
        }
        node.wires().drop(link);
        unhook(level, pos, link);
        node.wiresChanged();
        PowerNetwork.changed(level);
        return true;
    }
}
