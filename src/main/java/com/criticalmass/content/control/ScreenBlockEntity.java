package com.criticalmass.content.control;

import com.criticalmass.content.thermal.ThermalTooltips;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public class ScreenBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int LINES_PER_BLOCK = 3;
    public static final int CHARACTERS_PER_BLOCK = 5;
    public static final int CAPACITY = 400;
    public static final int MAX_SPAN = 16;
    private static final int SCAN_TICKS = 10;
    private static final int LINE_LIMIT = 160;

    private int energy;
    private boolean powered;
    private boolean controller = true;
    private int width = 1;
    private int height = 1;
    private int scanIn;
    private final List<String> lines = new ArrayList<>();
    private final IEnergyStorage storage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int taken = Math.max(0, Math.min(amount, CAPACITY - energy));
            if (!simulate && taken > 0) {
                energy += taken;
                setChanged();
            }
            return taken;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return CAPACITY;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    public ScreenBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public IEnergyStorage energy() {
        return storage;
    }

    public Direction facing() {
        return getBlockState().getValue(ScreenBlock.FACING);
    }

    public boolean powered() {
        return powered;
    }

    public boolean isController() {
        return controller;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public List<String> lines() {
        return lines;
    }

    private static boolean joins(LevelAccessor level, BlockPos pos, Direction facing) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof ScreenBlock && state.getValue(ScreenBlock.FACING) == facing;
    }

    public static Direction left(Direction facing) {
        return facing.getClockWise();
    }

    public static BlockPos controllerPos(LevelAccessor level, BlockPos pos, Direction facing) {
        BlockPos p = pos;
        for (int i = 0; i < MAX_SPAN && joins(level, p.above(), facing); i++) {
            p = p.above();
        }
        for (int i = 0; i < MAX_SPAN && joins(level, p.relative(left(facing)), facing); i++) {
            p = p.relative(left(facing));
        }
        return p;
    }

    @Nullable
    public static ScreenBlockEntity controllerOf(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ScreenBlock)) {
            return null;
        }
        return level.getBlockEntity(controllerPos(level, pos, state.getValue(ScreenBlock.FACING))) instanceof ScreenBlockEntity s ? s : null;
    }

    private void scan() {
        Direction facing = facing();
        boolean nowController = controllerPos(level, worldPosition, facing).equals(worldPosition);
        int nowWidth = 1;
        int nowHeight = 1;
        if (nowController) {
            while (nowWidth < MAX_SPAN && joins(level, worldPosition.relative(left(facing).getOpposite(), nowWidth), facing)) {
                nowWidth++;
            }
            while (nowHeight < MAX_SPAN && joins(level, worldPosition.below(nowHeight), facing)) {
                nowHeight++;
            }
        }
        if (nowController != controller || nowWidth != width || nowHeight != height) {
            controller = nowController;
            width = nowWidth;
            height = nowHeight;
            if (!controller) {
                lines.clear();
            }
            while (lines.size() > height * LINES_PER_BLOCK) {
                lines.remove(lines.size() - 1);
            }
            setChanged();
            sendData();
        }
    }

    public void setLine(int index, String text) {
        if (index < 0 || index >= height * LINES_PER_BLOCK) {
            return;
        }
        while (lines.size() <= index) {
            lines.add("");
        }
        String clipped = text.length() > LINE_LIMIT ? text.substring(0, LINE_LIMIT) : text;
        if (!clipped.equals(lines.get(index))) {
            lines.set(index, clipped);
            setChanged();
            sendData();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (scanIn-- <= 0) {
            scanIn = SCAN_TICKS;
            scan();
        }
        if (energy > 0) {
            energy--;
        }
        for (Direction d : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(d)) instanceof ScreenBlockEntity other && other.energy < energy - 1) {
                int moved = (energy - other.energy) / 2;
                energy -= moved;
                other.energy += moved;
                other.setChanged();
            }
        }
        boolean now = energy > 0;
        if (now != powered) {
            powered = now;
            sendData();
        }
        setChanged();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Energy", energy);
        tag.putBoolean("Powered", powered);
        tag.putBoolean("Controller", controller);
        tag.putInt("Width", width);
        tag.putInt("Height", height);
        ListTag list = new ListTag();
        for (String line : lines) {
            list.add(StringTag.valueOf(line));
        }
        tag.put("Lines", list);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        energy = tag.getInt("Energy");
        powered = tag.getBoolean("Powered");
        controller = !tag.contains("Controller") || tag.getBoolean("Controller");
        width = Math.max(1, tag.getInt("Width"));
        height = Math.max(1, tag.getInt("Height"));
        lines.clear();
        for (Tag t : tag.getList("Lines", Tag.TAG_STRING)) {
            lines.add(t.getAsString());
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("criticalmass.goggles.screen")));
        tooltip.add(powered ? ThermalTooltips.line("criticalmass.screen.powered", ChatFormatting.GREEN, energy) : ThermalTooltips.line("criticalmass.screen.unpowered", ChatFormatting.GOLD));
        ScreenBlockEntity main = level == null ? null : controllerOf(level, worldPosition);
        if (main != null) {
            tooltip.add(ThermalTooltips.line("criticalmass.screen.size", ChatFormatting.GRAY, main.width, main.height, main.height * LINES_PER_BLOCK, main.width * CHARACTERS_PER_BLOCK));
        }
        return true;
    }

    public static class Target extends DisplayTarget {
        @Override
        public void acceptText(int line, List<MutableComponent> text, DisplayLinkContext context) {
            ScreenBlockEntity screen = controllerOf(context.level(), context.getTargetPos());
            if (screen == null) {
                return;
            }
            reserve(line, screen, context);
            for (int i = 0; i < text.size(); i++) {
                if (i > 0 && isReserved(line + i, screen, context)) {
                    break;
                }
                screen.setLine(line + i, text.get(i).getString());
            }
        }

        @Override
        public DisplayTargetStats provideStats(DisplayLinkContext context) {
            ScreenBlockEntity screen = controllerOf(context.level(), context.getTargetPos());
            if (screen == null) {
                return new DisplayTargetStats(LINES_PER_BLOCK, CHARACTERS_PER_BLOCK, this);
            }
            return new DisplayTargetStats(screen.height * LINES_PER_BLOCK, screen.width * CHARACTERS_PER_BLOCK, this);
        }

        @Override
        public AABB getMultiblockBounds(LevelAccessor level, BlockPos pos) {
            ScreenBlockEntity screen = controllerOf(level, pos);
            if (screen == null) {
                return super.getMultiblockBounds(level, pos);
            }
            BlockPos corner = screen.getBlockPos().relative(left(screen.facing()).getOpposite(), screen.width - 1).below(screen.height - 1);
            return new AABB(screen.getBlockPos()).minmax(new AABB(corner));
        }
    }
}
