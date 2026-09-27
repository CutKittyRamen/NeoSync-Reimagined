package com.breakinblocks.neosync.api.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.breakinblocks.neosync.NeoSync;
import com.breakinblocks.neosync.api.shell.ServerShell;
import com.breakinblocks.neosync.api.shell.ShellState;
import com.breakinblocks.neosync.api.shell.ShellStateContainer;
import com.breakinblocks.neosync.common.utils.WorldUtil;

import java.util.UUID;

public record ShellRenamePacket(UUID shellUuid, String name) implements CustomPacketPayload {
    public static final Type<ShellRenamePacket> TYPE = new Type<>(NeoSync.locate("shell/rename"));

    public static final StreamCodec<FriendlyByteBuf, ShellRenamePacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ShellRenamePacket::shellUuid,
            ByteBufCodecs.stringUtf8(ShellState.MAX_NAME_LENGTH), ShellRenamePacket::name,
            ShellRenamePacket::new
    );

    public ShellRenamePacket(ShellState shell, String name) {
        this(shell.getUuid(), name == null ? "" : name);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void send() {
        PacketDistributor.sendToServer(this);
    }

    public static void handle(ShellRenamePacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !(player instanceof ServerShell shell)) {
                return;
            }

            ShellState state = shell.getShellStateByUuid(payload.shellUuid);
            if (state == null || !shell.canBeApplied(state)) {
                return;
            }

            ShellState target = findStoredState(player, state);
            target.setName(payload.name);
            shell.update(target);
        });
    }

    private static ShellState findStoredState(ServerPlayer player, ShellState state) {
        if (state.isVirtual()) {
            return state;
        }

        ServerLevel world = WorldUtil.findWorld(player.level().getServer().getAllLevels(), state.getWorld()).orElse(null);
        if (world == null) {
            return state;
        }

        BlockPos pos = state.getPos();
        if (state.getSubLevelUuid() == null) {
            world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        }

        ShellStateContainer container = ShellStateContainer.findAt(world, pos, state.getLocalOffset(), player, state.getSubLevelUuid());
        ShellState stored = container == null ? null : container.getShellState();
        if (stored == null || !state.getUuid().equals(stored.getUuid())) {
            return state;
        }

        if (container instanceof BlockEntity blockEntity) {
            blockEntity.setChanged();
        }
        return stored;
    }
}
