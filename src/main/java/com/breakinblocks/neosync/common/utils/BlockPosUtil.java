package com.breakinblocks.neosync.common.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class BlockPosUtil {
    public static Optional<Direction> getHorizontalFacing(BlockPos pos, BlockGetter blockView) {
        BlockState state = blockView.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return Optional.of(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        return Optional.empty();
    }

    public static boolean hasPlayerInside(BlockPos pos, EntityGetter world) {
        return hasPlayerInside(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), world);
    }

    public static boolean hasPlayerInside(Vec3 worldCenter, EntityGetter world) {
        return world.getNearestPlayer(worldCenter.x, worldCenter.y, worldCenter.z, 1, false) != null;
    }

    public static boolean isEntityInside(Entity entity, BlockPos pos) {
        return isEntityInside(entity.position(), pos);
    }

    public static boolean isEntityInside(Vec3 effectivePos, BlockPos pos) {
        double dX = Math.abs((pos.getX() + 0.5) - effectivePos.x);
        double dZ = Math.abs((pos.getZ() + 0.5) - effectivePos.z);
        final double MAX_DELTA = 0.01;
        return dX < MAX_DELTA && dZ < MAX_DELTA;
    }

    public static void moveEntity(Entity entity, BlockPos target, Direction facing, boolean inside) {
        Direction targetDirection = facing.getOpposite();
        double targetX = target.getX() + 0.5;
        double targetZ = target.getZ() + 0.5;
        if (!inside) {
            targetX += targetDirection.getStepX();
            targetZ += targetDirection.getStepZ();
        }
        moveEntityToward(entity, new Vec3(targetX, entity.getY(), targetZ), targetDirection.toYRot());
    }

    public static void moveEntityToward(Entity entity, Vec3 targetWorldPos, float facingYaw) {
        Vec3 currentPos = entity.position();
        final double MAX_SPEED = 0.33;
        double velocityX = getMinVelocity(targetWorldPos.x - currentPos.x, MAX_SPEED);
        double velocityZ = getMinVelocity(targetWorldPos.z - currentPos.z, MAX_SPEED);

        entity.setDeltaMovement(velocityX, 0, velocityZ);
        entity.setXRot(0);
        entity.setYRot(facingYaw);
        entity.setYHeadRot(facingYaw);
        entity.setYBodyRot(facingYaw);
        entity.yRotO = facingYaw;
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.yBodyRotO = facingYaw;
            livingEntity.yHeadRotO = facingYaw;
        }
    }

    private static double getMinVelocity(double velocity, double absLimit) {
        return Math.abs(velocity) < absLimit ? velocity : absLimit * Math.signum(velocity);
    }
}