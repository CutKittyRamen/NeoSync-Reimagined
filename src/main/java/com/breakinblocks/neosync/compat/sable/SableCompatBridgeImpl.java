package com.breakinblocks.neosync.compat.sable;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.UUID;

final class SableCompatBridgeImpl implements SableCompatBridge {
    public SableCompatBridgeImpl() {}

    @Override
    @Nullable
    public Object getTrackingSublevel(Entity entity) {
        return Sable.HELPER.getTrackingOrVehicleSubLevel(entity);
    }

    @Override
    @Nullable
    public Object getContainingSublevel(BlockEntity be) {
        return Sable.HELPER.getContaining(be);
    }

    @Override
    public Vec3 worldToLocal(@Nullable Object sublevel, Vec3 worldPos) {
        if (!(sublevel instanceof SubLevel sub)) return worldPos;
        return sub.logicalPose().transformPositionInverse(worldPos);
    }

    @Override
    public Vec3 localToWorld(@Nullable Object sublevel, Vec3 localPos) {
        if (!(sublevel instanceof SubLevel sub)) return localPos;
        return sub.logicalPose().transformPosition(localPos);
    }

    @Override
    public Vec3 transformDirectionToWorld(@Nullable Object sublevel, Vec3 localDir) {
        if (!(sublevel instanceof SubLevel sub)) return localDir;
        return sub.logicalPose().transformNormal(localDir);
    }

    @Override
    @Nullable
    public BlockEntity getSublevelBlockEntity(@Nullable Object sublevel, BlockPos localPos) {
        if (!(sublevel instanceof SubLevel sub)) return null;
        LevelPlot plot = sub.getPlot();
        if (plot == null) return null;
        ChunkPos chunkPos = new ChunkPos(localPos);
        if (!plot.contains(chunkPos)) return null;
        LevelChunk chunk = plot.getChunk(plot.toLocal(chunkPos));
        if (chunk == null) return null;
        return chunk.getBlockEntity(localPos);
    }

    @Override
    @Nullable
    public Object findSublevelAt(Level level, Vec3 worldPos) {
        BoundingBox3d bounds = new BoundingBox3d(worldPos.subtract(0.5, 0.5, 0.5), worldPos.add(0.5, 0.5, 0.5));
        for (SubLevel sub : Sable.HELPER.getAllIntersecting(level, bounds)) {
            Vec3 local = sub.logicalPose().transformPositionInverse(worldPos);
            if (Sable.HELPER.getContaining(level, local) == sub) {
                return sub;
            }
        }
        return null;
    }

    @Override
    public float getSublevelYaw(@Nullable Object sublevel) {
        if (!(sublevel instanceof SubLevel sub)) return 0F;
        Vector3d forward = new Vector3d(0, 0, 1);
        sub.logicalPose().orientation().transform(forward);
        return (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
    }

    @Override
    @Nullable
    public UUID getSublevelUuid(@Nullable Object sublevel) {
        if (!(sublevel instanceof SubLevel sub)) return null;
        return sub.getUniqueId();
    }

    @Override
    @Nullable
    public Object findSublevelByUuid(Level parentLevel, UUID uuid) {
        if (uuid == null) return null;
        SubLevelContainer container = SubLevelContainer.getContainer(parentLevel);
        if (container == null) return null;
        return container.getSubLevel(uuid);
    }

    @Override
    public void forceClientSync(ServerLevel parentLevel, @Nullable Object sublevel) {
        if (!(sublevel instanceof ServerSubLevel sub)) return;
        ServerSubLevelContainer container = SubLevelContainer.getContainer(parentLevel);
        if (container == null) return;
        Pose3dc pose = sub.logicalPose();
        container.physicsSystem().getPipeline().teleport(sub, pose.position(), pose.orientation());
    }
}
