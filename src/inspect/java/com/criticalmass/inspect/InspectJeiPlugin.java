package com.criticalmass.inspect;

import java.util.List;
import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class InspectJeiPlugin implements IModPlugin {
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("criticalmass_inspect", "jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    static String showRecipes(String spec) {
        if (runtime == null) {
            return "no jei runtime";
        }
        String type = null;
        int at = spec.indexOf('@');
        if (at >= 0) {
            type = spec.substring(at + 1);
            spec = spec.substring(0, at);
        }
        boolean uses = spec.startsWith("uses:");
        String id = uses ? spec.substring(5) : spec;
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
        IFocus<ItemStack> focus = runtime.getJeiHelpers().getFocusFactory().createFocus(uses ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack);
        if (type == null) {
            runtime.getRecipesGui().show(focus);
            return "shown";
        }
        Optional<RecipeType<?>> recipeType = runtime.getJeiHelpers().getRecipeType(ResourceLocation.parse(type));
        if (recipeType.isEmpty()) {
            return "no recipe type " + type;
        }
        return showType(recipeType.get(), focus);
    }

    private static <T> String showType(RecipeType<T> type, IFocus<ItemStack> focus) {
        List<T> recipes = runtime.getRecipeManager().createRecipeLookup(type).limitFocus(List.of(focus)).get().toList();
        if (recipes.isEmpty()) {
            return "no recipes";
        }
        runtime.getRecipesGui().showRecipes(runtime.getRecipeManager().getRecipeCategory(type), recipes, List.of(focus));
        return "shown " + recipes.size();
    }
}
