package com.breakinblocks.neosync.compat.sable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

interface SableCompatBridge {
    @Nullable
    default Object getTrackingSublevel(Entity entity) { return null; }

    @Nullable
    default Object getContainingSublevel(BlockEntity be) { return null; }

    default Vec3 worldToLocal(@Nullable Object sublevel, Vec3 worldPos) { return worldPos; }

    default Vec3 localToWorld(@Nullable Object sublevel, Vec3 localPos) { return localPos; }

    default Vec3 transformDirectionToWorld(@Nullable Object sublevel, Vec3 localDir) { return localDir; }

    @Nullable
    default BlockEntity getSublevelBlockEntity(@Nullable Object sublevel, BlockPos localPos) { return null; }

    @Nullable
    default Object findSublevelAt(Level level, Vec3 worldPos) { return null; }

    default float getSublevelYaw(@Nullable Object sublevel) { return 0F; }

    @Nullable
    default UUID getSublevelUuid(@Nullable Object sublevel) { return null; }

    @Nullable
    default Object findSublevelByUuid(Level parentLevel, UUID uuid) { return null; }

    default void forceClientSync(ServerLevel parentLevel, @Nullable Object sublevel) {}
}
