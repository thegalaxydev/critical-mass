package com.criticalmass.content.fuel;

import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class CentrifugeBlockEntity extends KineticBlockEntity {
    public static final int INPUT = 0;
    public static final int ENRICHED = 1;
    public static final int DEPLETED = 2;
    public static final float MIN_RPM = 96;
    public static final int BATCH = 2;
    public static final int WORK = 25600;
    public static final int CASCADE_MAX = 4;
    public static final double STAGE_GAIN = 0.25;
    private static final int SYNC_TICKS = 10;
    private static final int STAGE_TICKS = 20;
    private final ItemStackHandler items = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT && stack.is(CMItems.URANIUM_CONCENTRATE.get());
        }
    };
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return items.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == INPUT ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == INPUT ? ItemStack.EMPTY : items.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return items.isItemValid(slot, stack);
        }
    };
    private int progress;
    private int syncIn;
    private int stageIn;
    private int stages = 1;
    private double enrichedCarry;
    private double depletedCarry;
    private String status = "criticalmass.centrifuge.idle";

    public CentrifugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public IItemHandler automation() {
        return automation;
    }

    public ItemStack held(int slot) {
        return items.getStackInSlot(slot);
    }

    public String status() {
        return status;
    }

    public double progress() {
        return progress / (double) WORK;
    }

    public int stages() {
        return stages;
    }

    public double enrichedPerBatch() {
        return 1 + STAGE_GAIN * (stages - 1);
    }

    private int countStages() {
        int n = 1;
        for (Direction d : new Direction[]{Direction.UP, Direction.DOWN}) {
            BlockPos p = worldPosition.relative(d);
            while (n < CASCADE_MAX && level.getBlockState(p).is(getBlockState().getBlock())) {
                n++;
                p = p.relative(d);
            }
        }
        return n;
    }

    private void produce(int slot, double carry, Item item) {
        int whole = (int) Math.floor(carry + 1.0e-9);
        if (whole > 0) {
            add(slot, new ItemStack(item, whole));
        }
    }

    public ItemStack insert(ItemStack stack) {
        ItemStack left = items.insertItem(INPUT, stack, false);
        sendData();
        return left;
    }

    public boolean takeOutputs(Player player) {
        boolean any = false;
        for (int slot : new int[]{ENRICHED, DEPLETED}) {
            ItemStack out = items.extractItem(slot, items.getSlotLimit(slot), false);
            if (!out.isEmpty()) {
                player.getInventory().placeItemBackInInventory(out);
                any = true;
            }
        }
        if (any) {
            sendData();
        }
        return any;
    }

    private boolean room() {
        return items.getStackInSlot(ENRICHED).getCount() + BATCH <= items.getSlotLimit(ENRICHED) && items.getStackInSlot(DEPLETED).getCount() + BATCH <= items.getSlotLimit(DEPLETED);
    }

    private void add(int slot, ItemStack made) {
        ItemStack now = items.getStackInSlot(slot);
        if (now.isEmpty()) {
            items.setStackInSlot(slot, made);
        } else {
            now.grow(made.getCount());
            items.setStackInSlot(slot, now);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        float speed = Math.abs(getSpeed());
        String now;
        boolean finished = false;
        if (stageIn-- <= 0) {
            stageIn = STAGE_TICKS;
            int counted = countStages();
            finished = counted != stages;
            stages = counted;
        }
        if (items.getStackInSlot(INPUT).getCount() < BATCH) {
            progress = 0;
            now = "criticalmass.centrifuge.idle";
        } else if (!room()) {
            now = "criticalmass.centrifuge.full";
        } else if (speed < MIN_RPM) {
            now = "criticalmass.centrifuge.slow";
        } else {
            now = "criticalmass.centrifuge.running";
            progress += (int) speed;
            if (progress >= WORK) {
                progress = 0;
                items.extractItem(INPUT, BATCH, false);
                enrichedCarry += enrichedPerBatch();
                depletedCarry += BATCH - enrichedPerBatch();
                produce(ENRICHED, enrichedCarry, CMItems.ENRICHED_URANIUM.get());
                produce(DEPLETED, depletedCarry, CMItems.DEPLETED_URANIUM.get());
                enrichedCarry -= Math.floor(enrichedCarry + 1.0e-9);
                depletedCarry -= Math.floor(depletedCarry + 1.0e-9);
                finished = true;
            }
        }
        boolean changed = !now.equals(status);
        status = now;
        if (changed || finished || (syncIn-- <= 0 && now.endsWith("running"))) {
            syncIn = SYNC_TICKS;
            setChanged();
            sendData();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (level != null) {
            for (int slot = 0; slot < items.getSlots(); slot++) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(slot));
            }
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putString("Status", status);
        tag.putInt("Stages", stages);
        tag.putDouble("EnrichedCarry", enrichedCarry);
        tag.putDouble("DepletedCarry", depletedCarry);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        progress = tag.getInt("Progress");
        status = tag.contains("Status") ? tag.getString("Status") : "criticalmass.centrifuge.idle";
        stages = Math.max(1, tag.getInt("Stages"));
        enrichedCarry = tag.getDouble("EnrichedCarry");
        depletedCarry = tag.getDouble("DepletedCarry");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.kinetic(tooltip, super.addToGoggleTooltip(tooltip, isPlayerSneaking));
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ThermalTooltips.status(tooltip, status, status.endsWith("running") ? ChatFormatting.GREEN : ChatFormatting.GOLD, (int) MIN_RPM);
        if (status.endsWith("running")) {
            ThermalTooltips.label(tooltip, "criticalmass.goggles.label.progress");
            ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.percent", ChatFormatting.AQUA, Math.round(progress() * 100)));
        }
        if (stages > 1) {
            ThermalTooltips.label(tooltip, "criticalmass.goggles.label.cascade");
            ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.goggles.cascade_stages", ChatFormatting.AQUA, stages)
                    .append(ThermalTooltips.aside("criticalmass.goggles.aside.yield", ThermalTooltips.number(enrichedPerBatch()), BATCH)));
        }
        return true;
    }
}
