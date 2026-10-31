package com.breakinblocks.neosync.common.entity;

import com.breakinblocks.neosync.NeoSync;
import com.breakinblocks.neosync.api.shell.ShellState;
import com.breakinblocks.neosync.compat.sable.SableCompat;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * A sublevel does not finish settling on the tick a player arrives on it: the plot pose and the client's copy of it
 * both catch up over the next few ticks. Without a correction window the player is left standing where the ship used
 * to be, which reads as falling through it.
 */
@EventBusSubscriber(modid = NeoSync.MOD_ID)
public final class ShellArrival {
    private static final long WINDOW_TICKS = 40L;
    private static final long[] RESYNC_DELAYS = {5L, 15L, 30L};
    private static final double DRIFT_THRESHOLD_SQR = 4.0;

    private static final List<Pending> PENDING = Collections.synchronizedList(new ArrayList<>());

    private ShellArrival() {}

    private record Pending(UUID playerUuid, ResourceKey<Level> level, UUID subLevelUuid, Vec3 localOffset,
                           float yawDelta, long startTick) {}

    public static void schedule(ServerPlayer player, ServerLevel level, ShellState state) {
        UUID subLevelUuid = state.getSubLevelUuid();
        Vec3 localOffset = state.getLocalOffset();
        if (subLevelUuid == null || localOffset == null) {
            return;
        }
        MinecraftServer server = level.getServer();
        PENDING.add(new Pending(player.getUUID(), level.dimension(), subLevelUuid, localOffset,
                state.getYawDelta(), server.getTickCount()));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }

        MinecraftServer server = event.getServer();
        long tick = server.getTickCount();
        synchronized (PENDING) {
            Iterator<Pending> it = PENDING.iterator();
            while (it.hasNext()) {
                Pending pending = it.next();
                if (!settle(server, pending, tick)) {
                    it.remove();
                    continue;
                }
                if (tick - pending.startTick() >= WINDOW_TICKS) {
                    it.remove();
                }
            }
        }
    }

    private static boolean settle(MinecraftServer server, Pending pending, long tick) {
        ServerLevel level = server.getLevel(pending.level());
        if (level == null) {
            return false;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(pending.playerUuid());
        if (player == null || player.serverLevel() != level) {
            return false;
        }
        Object sublevel = SableCompat.findSublevelByUuid(level, pending.subLevelUuid());
        if (sublevel == null) {
            return false;
        }

        long elapsed = tick - pending.startTick();
        for (long delay : RESYNC_DELAYS) {
            if (elapsed == delay) {
                SableCompat.forceClientSync(level, sublevel);
                break;
            }
        }

        if (player.isSpectator() || player.getAbilities().flying || player.isFallFlying()) {
            return true;
        }

        Vec3 expected = SableCompat.localToWorld(sublevel, pending.localOffset());
        if (player.position().distanceToSqr(expected) <= DRIFT_THRESHOLD_SQR) {
            return true;
        }
        if (pending.subLevelUuid().equals(SableCompat.getSublevelUuid(SableCompat.getTrackingSublevel(player)))) {
            return true;
        }

        float yaw = pending.yawDelta() + SableCompat.getSublevelYaw(sublevel);
        player.teleportTo(level, expected.x, expected.y, expected.z,
                Collections.<RelativeMovement>emptySet(), yaw, player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.fallDistance = 0F;
        return true;
    }
}
