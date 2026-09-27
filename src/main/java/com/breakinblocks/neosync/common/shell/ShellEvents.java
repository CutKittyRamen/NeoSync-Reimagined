package com.breakinblocks.neosync.common.shell;

import com.breakinblocks.neosync.NeoSync;
import com.breakinblocks.neosync.api.networking.ShellUpdatePacket;
import com.breakinblocks.neosync.api.shell.ServerShell;
import com.breakinblocks.neosync.api.shell.ShellState;
import com.breakinblocks.neosync.common.utils.NeoSyncDebug;
import com.breakinblocks.neosync.common.utils.WorldUtil;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.Collection;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = NeoSync.MOD_ID)
public class ShellEvents {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncPlayerShells(player, "login");
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncPlayerShells(player, "dimension_change");
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncPlayerShells(player, "respawn");
        }
    }
    
    @SubscribeEvent
    public static void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel().getServer() != null) {
            ShellSavedData.get(event.getLevel().getServer()).setDirty();
        }
    }

    private static void syncPlayerShells(ServerPlayer player, String reason) {
        if (player.server == null) return;
        
        ShellSavedData data = ShellSavedData.get(player.server);
        Collection<ShellState> shells = data.getAvailableShellStates(player.getUUID()).collect(Collectors.toList());
        
        boolean isArtificial = false;
        if (player instanceof ServerShell shell) {
            isArtificial = shell.isArtificial();
        }

        NeoSyncDebug.info("server-shell", "syncPlayerShells player={} reason={} shells={} artificial={}", 
            player.getName().getString(), reason, shells.size(), isArtificial);

        new ShellUpdatePacket(WorldUtil.getId(player.level()), isArtificial, false, shells.stream().toList()).send(player);
    }
}
