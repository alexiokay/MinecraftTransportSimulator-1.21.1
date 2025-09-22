package mcinterface1211;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;

import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.capabilities.Capabilities;

public class WrapperItemStack implements IWrapperItemStack {

    protected final ItemStack stack;

    protected WrapperItemStack(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public boolean isCompleteMatch(IWrapperItemStack other) {
        ItemStack otherStack = ((WrapperItemStack) other).stack;
        return !stack.isEmpty() && otherStack.is(stack.getItem()) && (otherStack.has(DataComponents.CUSTOM_DATA) ? otherStack.get(DataComponents.CUSTOM_DATA).equals(stack.get(DataComponents.CUSTOM_DATA)) : !stack.has(DataComponents.CUSTOM_DATA));
    }

    @Override
    public int getFurnaceFuelValue() {
        // In NeoForge 1.21.1, use ItemStack.getBurnTime() directly instead of ForgeHooks.getBurnTime()
        return stack.getBurnTime(RecipeType.SMELTING);
    }

    @Override
    public IWrapperItemStack getSmeltedItem(AWrapperWorld world) {
        Level mcWorld = ((WrapperWorld) world).world;
        var results = mcWorld.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING);
        return new WrapperItemStack(results.isEmpty() ? ItemStack.EMPTY : results.get(0).value().getResultItem(((WrapperWorld) world).world.registryAccess()));
    }

    @Override
    public int getSmeltingTime(AWrapperWorld world) {
        Level mcWorld = ((WrapperWorld) world).world;
        var results = mcWorld.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING);
        return results.isEmpty() ? 0 : results.get(0).value().getCookingTime();
    }

    @Override
    public boolean isBrewingFuel() {
        return stack.getItem() == Items.BLAZE_POWDER;
    }

    @Override
    public boolean isBrewingVessel() {
        // Basic brewing vessel check for common potion containers
        return stack.getItem() == Items.GLASS_BOTTLE ||
               stack.getItem() == Items.POTION ||
               stack.getItem() == Items.SPLASH_POTION ||
               stack.getItem() == Items.LINGERING_POTION;
    }

    @Override
    public boolean isBrewingModifier() {
        // Basic brewing ingredient check for common brewing ingredients
        return stack.getItem() == Items.NETHER_WART ||
               stack.getItem() == Items.REDSTONE ||
               stack.getItem() == Items.GLOWSTONE_DUST ||
               stack.getItem() == Items.GUNPOWDER ||
               stack.getItem() == Items.DRAGON_BREATH ||
               stack.getItem() == Items.FERMENTED_SPIDER_EYE ||
               stack.getItem() == Items.MAGMA_CREAM ||
               stack.getItem() == Items.SUGAR ||
               stack.getItem() == Items.RABBIT_FOOT ||
               stack.getItem() == Items.GLISTERING_MELON_SLICE ||
               stack.getItem() == Items.SPIDER_EYE ||
               stack.getItem() == Items.PUFFERFISH ||
               stack.getItem() == Items.GOLDEN_CARROT ||
               stack.getItem() == Items.TURTLE_HELMET ||
               stack.getItem() == Items.PHANTOM_MEMBRANE;
    }

    @Override
    public IWrapperItemStack getBrewedItem(IWrapperItemStack modifierStack) {
        // Since the brewing recipe system has fundamentally changed in NeoForge 1.21.1,
        // and there's no direct replacement for the old getOutput method,
        // we return an empty stack as brewing is now handled differently
        return new WrapperItemStack(ItemStack.EMPTY);
    }

    @Override
    public AItemBase getItem() {
        Item item = stack.getItem();
        return item instanceof IBuilderItemInterface ? ((IBuilderItemInterface) item).getWrappedItem() : null;
    }

    @Override
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    @Override
    public int getSize() {
        return stack.getCount();
    }

    @Override
    public int getMaxSize() {
        return stack.getMaxStackSize();
    }

    @Override
    public int add(int qty) {
        if (qty < 0) {
            int amountToRemove = -qty;
            if (amountToRemove > getSize()) {
                amountToRemove = getSize();
            }
            stack.setCount(stack.getCount() - amountToRemove);
            return qty + amountToRemove;
        } else {
            int amountToAdd = qty;
            if (amountToAdd + getSize() > getMaxSize()) {
                amountToAdd = getMaxSize() - getSize();
            }
            stack.setCount(stack.getCount() + amountToAdd);
            return qty - amountToAdd;
        }
    }

    @Override
    public IWrapperItemStack copy() {
        return new WrapperItemStack(stack.copy());
    }

    @Override
    public IWrapperItemStack split(int qty) {
        return new WrapperItemStack(stack.split(qty));
    }

    @Override
    public boolean interactWith(EntityFluidTank tank, IWrapperPlayer player) {
        IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler != null) {
            if (!player.isSneaking()) {
                //Item can provide fluid.  Check if the tank can accept it.
                FluidStack drainedStack = handler.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
                if (drainedStack != null) {
                    //Able to take fluid from item, attempt to do so.
                    ResourceLocation fluidLocation = BuiltInRegistries.FLUID.getKey(drainedStack.getFluid());
                    int amountToDrain = (int) tank.fill(fluidLocation.getPath(), fluidLocation.getNamespace(), drainedStack.getAmount(), false);
                    drainedStack = handler.drain(amountToDrain, player.isCreative() ? FluidAction.SIMULATE : FluidAction.EXECUTE);
                    if (drainedStack != null) {
                        //Was able to provide liquid from item.  Fill the tank.
                        tank.fill(fluidLocation.getPath(), fluidLocation.getNamespace(), drainedStack.getAmount(), true);
                        player.setHeldStack(new WrapperItemStack(handler.getContainer()));
                    }
                }
            } else {
                //Item can hold fluid.  Check if we can fill it.
                //Need to find the mod that registered this fluid, NeoForge has them per-mod vs just all with a single name.
                for (ResourceLocation fluidKey : BuiltInRegistries.FLUID.keySet()) {
                    if ((tank.getFluidMod().equals(EntityFluidTank.WILDCARD_FLUID_MOD) || tank.getFluidMod().equals(fluidKey.getNamespace())) && fluidKey.getPath().equals(tank.getFluid())) {
                        FluidStack containedStack = new FluidStack(BuiltInRegistries.FLUID.get(fluidKey), (int) tank.getFluidLevel());
                        int amountFilled = handler.fill(containedStack, player.isCreative() ? FluidAction.SIMULATE : FluidAction.EXECUTE);
                        if (amountFilled > 0) {
                            //Were able to fill the item.  Apply state change to tank and item.
                            tank.drain(amountFilled, true);
                            player.setHeldStack(new WrapperItemStack(handler.getContainer()));
                        }
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public IWrapperNBT getData() {
        return stack.has(DataComponents.CUSTOM_DATA) ? new WrapperNBT(stack.get(DataComponents.CUSTOM_DATA).copyTag()) : null;
    }

    @Override
    public void setData(IWrapperNBT data) {
        if (data != null) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(((WrapperNBT) data).tag));
        } else {
            stack.remove(DataComponents.CUSTOM_DATA);
        }
    }
}