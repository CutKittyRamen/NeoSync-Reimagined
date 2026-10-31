package com.breakinblocks.neosync.api.shell;

import com.breakinblocks.neosync.compat.sable.SableCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A container that can store player's shell.
 */
public interface ShellStateContainer {
    /**
     * Attempts to retrieve a {@link ShellStateContainer} instance from a block in the world.
     */
    @Nullable
    static ShellStateContainer find(Level world, BlockPos pos) {
        if (!world.isLoaded(pos)) return null;
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof ShellStateContainer container ? container : null;
    }

    @Nullable
    static ShellStateContainer findNear(Entity entity) {
        Object sublevel = SableCompat.getTrackingSublevel(entity);
        if (sublevel != null) {
            Vec3 local = SableCompat.worldToLocal(sublevel, entity.position());
            BlockEntity be = SableCompat.getSublevelBlockEntity(sublevel, BlockPos.containing(local));
            if (be instanceof ShellStateContainer container) return container;
        }
        return find(entity.level(), entity.blockPosition());
    }

    @Nullable
    static ShellStateContainer findAt(Level world, BlockPos pos, Entity sublevelContext) {
        return findAt(world, pos, null, sublevelContext, null);
    }

    @Nullable
    static ShellStateContainer findAt(Level world, BlockPos pos, @Nullable Vec3 localPos, @Nullable Entity sublevelContext, @Nullable UUID sublevelHint) {
        if (sublevelHint != null) {
            Object sublevel = SableCompat.findSublevelByUuid(world, sublevelHint);
            ShellStateContainer hit = lookupInSublevel(sublevel, pos, localPos);
            if (hit != null) return hit;
        }
        if (sublevelContext != null) {
            Object sublevel = SableCompat.getTrackingSublevel(sublevelContext);
            ShellStateContainer hit = lookupInSublevel(sublevel, pos, localPos);
            if (hit != null) return hit;
        }
        return find(world, pos);
    }

    @Nullable
    private static ShellStateContainer lookupInSublevel(@Nullable Object sublevel, BlockPos worldPos, @Nullable Vec3 localPos) {
        if (sublevel == null) return null;
        Vec3 local = localPos != null ? localPos : SableCompat.worldToLocal(sublevel, Vec3.atCenterOf(worldPos));
        BlockEntity be = SableCompat.getSublevelBlockEntity(sublevel, BlockPos.containing(local));
        return be instanceof ShellStateContainer container ? container : null;
    }

    /**
     * Attempts to retrieve a {@link ShellStateContainer} that contains a given {@link ShellState}.
     */
    @Nullable
    static ShellStateContainer find(Level world, ShellState state) {
        ShellStateContainer container = find(world, state.getPos());
        if (container != null && container.getShellState() == state) {
            return container;
        }
        return null;
    }

    default boolean isRemotelyAccessible() {
        return true;
    }

    @Nullable
    ShellState getShellState();

    void setShellState(@Nullable ShellState state);

    @Nullable
    default DyeColor getColor() {
        ShellState state = this.getShellState();
        return state == null ? null : state.getColor();
    }
}
