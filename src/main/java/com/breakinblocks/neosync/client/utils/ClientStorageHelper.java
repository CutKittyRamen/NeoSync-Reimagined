package com.breakinblocks.neosync.client.utils;

import com.breakinblocks.neosync.client.gui.ShellSelectorGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientStorageHelper {
    private ClientStorageHelper() {}

    public static boolean isLocalPlayer(Entity entity) {
        return Minecraft.getInstance().player == entity;
    }

    public static boolean hasOpenScreen() {
        return Minecraft.getInstance().screen != null;
    }

    public static void openSelector(BlockPos pos, Runnable onClose, Runnable onRemoved) {
        Minecraft.getInstance().setScreen(new ShellSelectorGUI(pos, onClose, onRemoved));
    }
}
