package com.breakinblocks.neosync.mixins.common;

import com.breakinblocks.neosync.api.shell.ShellState;
import com.breakinblocks.neosync.api.shell.ShellStateManager;
import com.breakinblocks.neosync.api.shell.ShellStateUpdateType;
import com.breakinblocks.neosync.common.shell.ShellSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements ShellStateManager {

    private ShellSavedData getSavedData() {
        return ShellSavedData.get((MinecraftServer) (Object) this);
    }

    @Override
    public void setAvailableShellStates(UUID owner, Stream<ShellState> states) {
        getSavedData().setAvailableShellStates(owner, states);
    }

    @Override
    public Stream<ShellState> getAvailableShellStates(UUID owner) {
        return getSavedData().getAvailableShellStates(owner);
    }

    @Override
    public @Nullable ShellState getShellStateByUuid(UUID owner, UUID uuid) {
        return getSavedData().getShellStateByUuid(owner, uuid);
    }

    @Override
    public void add(ShellState state) {
        getSavedData().add(state);
    }

    @Override
    public void remove(ShellState state) {
        getSavedData().remove(state);
    }

    @Override
    public void update(ShellState state) {
        getSavedData().update(state);
    }

}

