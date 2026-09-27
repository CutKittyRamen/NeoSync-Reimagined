package com.breakinblocks.neosync.compat.sable;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.UUID;

public final class SableCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean LOADED = ModList.get().isLoaded("sable");
    private static final SableCompatBridge BRIDGE = LOADED ? loadBridge() : new SableCompatBridge() {};

    private SableCompat() {}

    private static SableCompatBridge loadBridge() {
        try {
            Class<?> cls = Class.forName("com.breakinblocks.neosync.compat.sable.SableCompatBridgeImpl");
            return (SableCompatBridge) cls.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            LOGGER.warn("Sable is loaded but compat bridge failed to initialize", t);
            return new SableCompatBridge() {};
        }
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    @Nullable
    public static Object getTrackingSublevel(Entity entity) {
        return BRIDGE.getTrackingSublevel(entity);
    }

    @Nullable
    public static Object getContainingSublevel(BlockEntity be) {
        return BRIDGE.getContainingSublevel(be);
    }

    public static Vec3 worldToLocal(@Nullable Object sublevel, Vec3 worldPos) {
        return BRIDGE.worldToLocal(sublevel, worldPos);
    }

    public static Vec3 localToWorld(@Nullable Object sublevel, Vec3 localPos) {
        return BRIDGE.localToWorld(sublevel, localPos);
    }

    public static Vec3 transformDirectionToWorld(@Nullable Object sublevel, Vec3 localDir) {
        return BRIDGE.transformDirectionToWorld(sublevel, localDir);
    }

    @Nullable
    public static BlockEntity getSublevelBlockEntity(@Nullable Object sublevel, BlockPos localPos) {
        return BRIDGE.getSublevelBlockEntity(sublevel, localPos);
    }

    @Nullable
    public static Object findSublevelAt(Level level, Vec3 worldPos) {
        return BRIDGE.findSublevelAt(level, worldPos);
    }

    public static float getSublevelYaw(@Nullable Object sublevel) {
        return BRIDGE.getSublevelYaw(sublevel);
    }

    @Nullable
    public static UUID getSublevelUuid(@Nullable Object sublevel) {
        return BRIDGE.getSublevelUuid(sublevel);
    }

    @Nullable
    public static Object findSublevelByUuid(Level parentLevel, @Nullable UUID uuid) {
        if (uuid == null) return null;
        return BRIDGE.findSublevelByUuid(parentLevel, uuid);
    }

    public static void forceClientSync(ServerLevel parentLevel, @Nullable Object sublevel) {
        BRIDGE.forceClientSync(parentLevel, sublevel);
    }

    @Nullable
    public static BlockEntity findBlockEntity(Level worldLevel, BlockPos worldPos, Entity entityForSublevelLookup) {
        Object sub = getTrackingSublevel(entityForSublevelLookup);
        if (sub != null) {
            Vec3 localPos = worldToLocal(sub, Vec3.atCenterOf(worldPos));
            BlockEntity be = getSublevelBlockEntity(sub, BlockPos.containing(localPos));
            if (be != null) return be;
        }
        return worldLevel.isLoaded(worldPos) ? worldLevel.getBlockEntity(worldPos) : null;
    }
}
