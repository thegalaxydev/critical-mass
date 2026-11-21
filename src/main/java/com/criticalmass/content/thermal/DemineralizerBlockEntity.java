package com.criticalmass.content.thermal;

import com.criticalmass.content.radiation.ContaminationData;
import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMFluids;
import com.criticalmass.registry.CMItems;
import com.criticalmass.registry.CMSounds;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class DemineralizerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, Clearable {
    public static final int CAPACITY = 1000;
    public static final int RATE = 10;
    public static final double RESIN_CAPACITY = 4000;
    public static final String NO_RESIN = "criticalmass.demineralizer.no_resin";
    public static final String SPENT = "criticalmass.demineralizer.spent";
    public static final String CLEANING = "criticalmass.demineralizer.cleaning";
    public static final String READY = "criticalmass.demineralizer.ready";
    public static final String OCCUPIED = "criticalmass.demineralizer.occupied";
    public static final String SPENT_CARTRIDGE = "criticalmass.demineralizer.spent_cartridge";
    private static final double TRACE = 1e-9;
    private static final int SYNC_TICKS = 20;
    private final CoolantBuffer inlet = new CoolantBuffer(CAPACITY, this::setChanged);
    private final CoolantBuffer outlet = new CoolantBuffer(CAPACITY, this::setChanged);
    private final IItemHandler items = new Slot();
    private ItemStack cartridge = ItemStack.EMPTY;
    private String status = NO_RESIN;
    private double sentLoad = -1;
    private int analog = -1;

    public DemineralizerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public CoolantBuffer inlet() {
        return inlet;
    }

    public CoolantBuffer outlet() {
        return outlet;
    }

    public IItemHandler items() {
        return items;
    }

    public ItemStack cartridge() {
        return cartridge;
    }

    public String status() {
        return status;
    }

    public boolean loaded() {
        return !cartridge.isEmpty();
    }

    public double load() {
        return ResinCartridgeItem.load(cartridge);
    }

    public double resinLeft() {
        return loaded() ? 1 - load() : 0;
    }

    public boolean blocked() {
        return inlet.activityLoad() > TRACE && resinLeft() <= 0;
    }

    public int analog() {
        return (int) Math.ceil(15 * resinLeft() - 1e-6);
    }

    @Nullable
    public String refuseReason(ItemStack stack) {
        if (!ResinCartridgeItem.isCartridge(stack)) {
            return OCCUPIED;
        }
        if (loaded()) {
            return OCCUPIED;
        }
        return ResinCartridgeItem.load(stack) >= 1 ? SPENT_CARTRIDGE : null;
    }

    public boolean insert(ItemStack stack) {
        if (refuseReason(stack) != null) {
            return false;
        }
        cartridge = stack.copyWithCount(1);
        resinChanged();
        return true;
    }

    public ItemStack take() {
        ItemStack out = cartridge;
        cartridge = ItemStack.EMPTY;
        if (!out.isEmpty()) {
            resinChanged();
        }
        return out;
    }

    private DemineralizerBlock.Resin shown() {
        double load = load();
        return !loaded() ? DemineralizerBlock.Resin.NONE : load >= 1 ? DemineralizerBlock.Resin.SPENT : load > 0 ? DemineralizerBlock.Resin.USED : DemineralizerBlock.Resin.FRESH;
    }

    private void resinChanged() {
        setChanged();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        DemineralizerBlock.Resin now = shown();
        if (state.hasProperty(DemineralizerBlock.RESIN) && state.getValue(DemineralizerBlock.RESIN) != now) {
            if (now == DemineralizerBlock.Resin.SPENT) {
                level.playSound(null, worldPosition, CMSounds.RELAY_CLICK.get(), SoundSource.BLOCKS, 0.6f, 0.8f);
            }
            level.setBlock(worldPosition, state.setValue(DemineralizerBlock.RESIN, now), Block.UPDATE_ALL);
        }
        int signal = analog();
        if (signal != analog) {
            analog = signal;
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
        sentLoad = load();
        sendData();
    }

    private void absorb(double activity) {
        double load = Math.min(1, load() + activity / RESIN_CAPACITY);
        if (cartridge.is(CMItems.RESIN_CARTRIDGE.get())) {
            cartridge = ResinCartridgeItem.loaded(load);
        } else {
            cartridge.set(CMComponents.RESIN_LOAD.get(), load);
        }
        setChanged();
    }

    private boolean pass() {
        int n = Math.min(RATE, Math.min(inlet.amount(), outlet.space()));
        if (n <= 0) {
            return false;
        }
        double share = inlet.activityLoad() / inlet.amount();
        if (share > TRACE) {
            double left = resinLeft() * RESIN_CAPACITY;
            if (left <= 0) {
                return false;
            }
            n = (int) Math.min(n, Math.max(1, Math.floor(left / share)));
        }
        CoolantBuffer.Packet p = inlet.take(n);
        boolean cleaned = p.activity() > TRACE;
        if (cleaned) {
            absorb(p.activity());
        }
        FluidStack clean = CMThermal.isContaminated(p.fluid()) ? new FluidStack(CMFluids.PRIMARY_COOLANT.source.get(), p.amount()) : p.fluid();
        outlet.give(new CoolantBuffer.Packet(clean, p.heat(), p.boron(), 0));
        return cleaned;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        DemineralizerBlock.Resin before = shown();
        boolean cleaned = pass();
        String now = !loaded() ? NO_RESIN : load() >= 1 ? SPENT : cleaned ? CLEANING : READY;
        boolean moved = shown() != before || analog() != analog;
        if (!now.equals(status)) {
            status = now;
            moved = true;
        }
        if (moved || level.getGameTime() % SYNC_TICKS == 0 && Math.abs(load() - sentLoad) > 1e-4) {
            resinChanged();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), cartridge);
        cartridge = ItemStack.EMPTY;
        double spilled = inlet.activityLoad() * CMThermal.CONTAMINATED_SPILL + (inlet.amount() - inlet.activityLoad() + outlet.amount()) * CMThermal.CLEAN_SPILL;
        if (spilled > 0) {
            ContaminationData.get(server).add(worldPosition, spilled);
        }
        inlet.set(FluidStack.EMPTY, 0);
        outlet.set(FluidStack.EMPTY, 0);
    }

    @Override
    public void clearContent() {
        cartridge = ItemStack.EMPTY;
    }

    public IFluidHandler flow(@Nullable Direction side) {
        return side == Direction.UP ? null : new Flow(side);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (!cartridge.isEmpty()) {
            tag.put("Cartridge", cartridge.save(registries));
        }
        tag.putString("Status", status);
        if (!clientPacket) {
            tag.put("Inlet", inlet.save(registries));
            tag.put("Outlet", outlet.save(registries));
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        cartridge = tag.contains("Cartridge") ? ItemStack.parseOptional(registries, tag.getCompound("Cartridge")) : ItemStack.EMPTY;
        status = tag.contains("Status") ? tag.getString("Status") : NO_RESIN;
        if (!clientPacket) {
            inlet.load(tag.getCompound("Inlet"), registries);
            outlet.load(tag.getCompound("Outlet"), registries);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ChatFormatting color = status.equals(CLEANING) ? ChatFormatting.GREEN : status.equals(SPENT) ? ChatFormatting.RED : status.equals(READY) ? ChatFormatting.AQUA : ChatFormatting.GRAY;
        ThermalTooltips.status(tooltip, status, color);
        if (loaded()) {
            ThermalTooltips.label(tooltip, "criticalmass.goggles.label.resin_left");
            ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.percent", resinLeft() > 0.2 ? ChatFormatting.AQUA : ChatFormatting.GOLD, Math.round(resinLeft() * 100)));
        }
        return true;
    }

    private final class Flow implements IFluidHandler {
        private final IFluidHandler in;
        private final IFluidHandler out;

        Flow(@Nullable Direction side) {
            in = inlet.handler(level, worldPosition, side);
            out = outlet.handler(level, worldPosition, side);
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? inlet.fluid() : outlet.fluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && CMThermal.isPrimaryMedium(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return CMThermal.isPrimaryMedium(resource) ? in.fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return out.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return out.drain(maxDrain, action);
        }
    }

    private final class Slot implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return cartridge;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || refuseReason(stack) != null) {
                return stack;
            }
            if (!simulate) {
                insert(stack);
            }
            return stack.copyWithCount(stack.getCount() - 1);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0 || cartridge.isEmpty() || load() < 1) {
                return ItemStack.EMPTY;
            }
            return simulate ? cartridge.copy() : take();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return ResinCartridgeItem.isCartridge(stack) && ResinCartridgeItem.load(stack) < 1;
        }
    }
}
