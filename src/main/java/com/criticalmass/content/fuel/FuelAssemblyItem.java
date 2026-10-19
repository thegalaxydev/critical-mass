package com.criticalmass.content.fuel;

import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMItems;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class FuelAssemblyItem extends Item {
    public enum Form {
        FRESH, SPENT, DAMAGED;

        public Item item() {
            return switch (this) {
                case FRESH -> CMItems.FRESH_FUEL_ASSEMBLY.get();
                case SPENT -> CMItems.SPENT_FUEL_ASSEMBLY.get();
                case DAMAGED -> CMItems.DAMAGED_FUEL_ASSEMBLY.get();
            };
        }
    }

    private final Form form;

    public FuelAssemblyItem(Form form, Properties properties) {
        super(properties);
        this.form = form;
    }

    public Form form() {
        return form;
    }

    public static boolean isAssembly(ItemStack stack) {
        return stack.getItem() instanceof FuelAssemblyItem;
    }

    @Nullable
    public static FuelState state(ItemStack stack) {
        return stack.get(CMComponents.FUEL_STATE.get());
    }

    public static FuelState defaultState(Form form) {
        FuelState s = FuelState.fresh();
        return switch (form) {
            case FRESH -> s;
            case SPENT -> s.withRemaining(0);
            case DAMAGED -> s.withCondition(0, "unknown");
        };
    }

    public static FuelState ensure(ItemStack stack) {
        FuelState s = state(stack);
        if (s == null && stack.getItem() instanceof FuelAssemblyItem a) {
            s = defaultState(a.form);
            stack.set(CMComponents.FUEL_STATE.get(), s);
        }
        return s;
    }

    public static ItemStack stackFor(FuelState state) {
        ItemStack stack = new ItemStack(state.form().item());
        stack.set(CMComponents.FUEL_STATE.get(), state);
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && state(stack) == null) {
            ensure(stack);
        }
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        ensure(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        FuelState s = state(stack);
        return s != null && (form == Form.FRESH ? s.remaining() < 1 : s.residual() > 0);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        FuelState s = state(stack);
        if (s == null) {
            return 13;
        }
        double v = form == Form.FRESH ? s.remaining() : s.coolingProgress();
        return Math.round(13 * (float) Mth.clamp(v, 0, 1));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return form == Form.FRESH ? 0x6fd36a : 0x4fa8e0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FuelState s = state(stack);
        if (s == null) {
            s = defaultState(form);
        }
        tooltip.add(Component.translatable("criticalmass.fuel.remaining", Math.round(s.remaining() * 1000) / 10.0).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("criticalmass.fuel.condition", Math.round(s.condition() * 1000) / 10.0)
                .withStyle(s.condition() < 0.5 ? ChatFormatting.RED : ChatFormatting.GRAY));
        if (s.residual() > 0) {
            tooltip.add(Component.translatable("criticalmass.fuel.residual", ThermalTooltips.number(s.residual())).withStyle(ChatFormatting.GOLD));
        }
        if (form != Form.FRESH) {
            tooltip.add(Component.translatable("criticalmass.fuel.cooling", Math.round(s.coolingProgress() * 100))
                    .withStyle(s.cooled() ? ChatFormatting.AQUA : ChatFormatting.GOLD));
        }
        if (s.burned() > 0) {
            tooltip.add(Component.translatable("criticalmass.fuel.burned", ThermalTooltips.number(s.burned())).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (s.contamination() > 0) {
            tooltip.add(Component.translatable("criticalmass.fuel.contamination", ThermalTooltips.number(s.contamination())).withStyle(ChatFormatting.DARK_GREEN));
        }
        if (!s.damageCause().isEmpty()) {
            tooltip.add(Component.translatable("criticalmass.fuel.damage_cause", Component.translatable("criticalmass.fuel.cause." + s.damageCause())).withStyle(ChatFormatting.RED));
        }
        if (form == Form.SPENT) {
            tooltip.add(Component.translatable(s.cooled() ? "criticalmass.fuel.store_shielded" : "criticalmass.fuel.needs_rack").withStyle(ChatFormatting.DARK_AQUA));
        }
        if (form == Form.DAMAGED) {
            tooltip.add(Component.translatable("criticalmass.fuel.no_reinsert").withStyle(ChatFormatting.DARK_RED));
        }
        if (flag.isAdvanced()) {
            UUID id = s.id();
            tooltip.add(Component.translatable("criticalmass.fuel.lifecycle", id.toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
