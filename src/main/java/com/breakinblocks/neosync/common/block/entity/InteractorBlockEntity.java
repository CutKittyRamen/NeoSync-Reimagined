package com.breakinblocks.neosync.common.block.entity;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;
import com.breakinblocks.neosync.common.block.InteractorBlock;

public class InteractorBlockEntity extends BlockEntity {

    public InteractorBlockEntity(BlockPos pos, BlockState state) {
        super(SyncBlockEntities.INTERACTOR.get(), pos, state);
    }

    @LuaFunction
    public final boolean interact(String uuidString) {
        if (!(this.level instanceof ServerLevel serverLevel)) return false;

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            return false;
        }

        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(uuid);
        if (player == null) return false;

        Direction facing = this.getBlockState().getValue(InteractorBlock.FACING);
        BlockPos targetPos = this.worldPosition.relative(facing);

        // Simulate interaction
        BlockHitResult hitResult = new BlockHitResult(
                Vec3.atCenterOf(targetPos),
                facing.getOpposite(),
                targetPos,
                false
        );

        return player.gameMode.useItemOn(player, serverLevel, player.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND, hitResult).consumesAction();
    }

    private Object peripheral;

    public Object getPeripheral() {
        if (peripheral == null) {
            peripheral = new IPeripheral() {
                @Override
                public String getType() {
                    return "interactor";
                }

                @Override
                public boolean equals(IPeripheral other) {
                    return this == other;
                }

                @Override
                public Object getTarget() {
                    return InteractorBlockEntity.this;
                }
            };
        }
        return peripheral;
    }
}
