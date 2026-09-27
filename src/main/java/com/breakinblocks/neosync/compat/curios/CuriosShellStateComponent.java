package com.breakinblocks.neosync.compat.curios;

import com.breakinblocks.neosync.api.shell.ShellStateComponent;
import com.breakinblocks.neosync.common.utils.nbt.SyncRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class CuriosShellStateComponent extends ShellStateComponent {
    public static final String IDENTIFIER = "Identifier";
    public static final String STACKS = "Stacks";
    public static final String COSMETICS = "Cosmetics";
    public static final String RENDERS = "Renders";

    private static final String ID = "neosync:curios";
    private static final String KEY = "curios";

    public record Worn(String identifier, int index, ItemStack stack, boolean cosmetic, boolean renderable) {}

    @Override
    public String getId() {
        return ID;
    }

    protected abstract ListTag getCurios();

    protected abstract void setCurios(ListTag curios);

    @Override
    public Collection<ItemStack> getItems() {
        return readItems(this.getCurios());
    }

    protected static List<ItemStack> readItems(ListTag curios) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < curios.size(); i++) {
            CompoundTag slotType = curios.getCompound(i);
            collect(readSlots(slotType.getCompound(STACKS)), items);
            collect(readSlots(slotType.getCompound(COSMETICS)), items);
        }
        return items;
    }

    /**
     * @return The curios of the shell that a wearer would have visible on them.
     */
    public List<Worn> getWorn() {
        return readWorn(this.getCurios());
    }

    @Override
    public void clone(ShellStateComponent component) {
        CuriosShellStateComponent other = component.as(CuriosShellStateComponent.class);
        if (other != null) {
            this.setCurios(other.getCurios());
        }
    }

    @Override
    protected void readComponentNbt(CompoundTag nbt) {
        this.setCurios(nbt.getList(KEY, Tag.TAG_COMPOUND));
    }

    @Override
    protected CompoundTag writeComponentNbt(CompoundTag nbt) {
        nbt.put(KEY, this.getCurios());
        return nbt;
    }

    protected static List<Worn> readWorn(ListTag curios) {
        List<Worn> worn = new ArrayList<>();
        for (int i = 0; i < curios.size(); i++) {
            CompoundTag slotType = curios.getCompound(i);
            String identifier = slotType.getString(IDENTIFIER);
            List<ItemStack> stacks = readSlots(slotType.getCompound(STACKS));
            List<ItemStack> cosmetics = readSlots(slotType.getCompound(COSMETICS));
            byte[] renders = slotType.getByteArray(RENDERS);

            for (int index = 0; index < Math.max(stacks.size(), cosmetics.size()); index++) {
                boolean renderable = index >= renders.length || renders[index] != 0;
                ItemStack cosmetic = index < cosmetics.size() ? cosmetics.get(index) : ItemStack.EMPTY;
                if (!cosmetic.isEmpty()) {
                    worn.add(new Worn(identifier, index, cosmetic, true, renderable));
                    continue;
                }

                ItemStack stack = index < stacks.size() ? stacks.get(index) : ItemStack.EMPTY;
                if (renderable && !stack.isEmpty()) {
                    worn.add(new Worn(identifier, index, stack, false, true));
                }
            }
        }
        return worn;
    }

    private static List<ItemStack> readSlots(CompoundTag nbt) {
        if (nbt.isEmpty()) {
            return List.of();
        }

        ItemStackHandler handler = new ItemStackHandler();
        handler.deserializeNBT(SyncRegistries.provider(), nbt);
        List<ItemStack> stacks = new ArrayList<>(handler.getSlots());
        for (int i = 0; i < handler.getSlots(); i++) {
            stacks.add(handler.getStackInSlot(i));
        }
        return stacks;
    }

    private static void collect(List<ItemStack> stacks, List<ItemStack> items) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }
    }
}
