package com.breakinblocks.neosync.common.block.entity;

import com.breakinblocks.neosync.NeoSync;
import com.breakinblocks.neosync.api.shell.ShellState;
import com.breakinblocks.neosync.common.config.SyncConfig;
import com.breakinblocks.neosync.common.utils.NeoSyncDebug;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@OnlyIn(Dist.CLIENT)
public class ShellEntity extends RemotePlayer {
    private static final ResourceLocation TECHNOBLADE_CAPE_TEXTURE = NeoSync.locate("textures/entity/technoblade.png");
    private static final Cache<UUID, PlayerInfo> PLAYER_ENTRY_CACHE = CacheBuilder.newBuilder()
            .initialCapacity(20)
            .maximumSize(40)
            .expireAfterAccess(20, TimeUnit.MINUTES)
            .build();

    public boolean isActive;
    public float pitchProgress;
    private final ShellState state;
    private Runnable onInitialized;
    private final PlayerInfo playerEntry;

    public ShellEntity(ShellState state) {
        this(Minecraft.getInstance().level, state);
    }

    public ShellEntity(ClientLevel world, ShellState state) {
        super(world, buildProfile(state));
        this.isActive = false;
        this.pitchProgress = 0;
        this.state = state;
        this.refreshFromState();
        this.playerEntry = getPlayerEntry(state);
        this.moveTo(state.getPos().getX() + 0.5, state.getPos().getY(), state.getPos().getZ() + 0.5, 0, 0);
        this.xCloakO = this.xCloak = getX();
        this.yCloakO = this.yCloak = getY();
        this.zCloakO = this.zCloak = getZ();
        NeoSyncDebug.info("shell-entity", "created shell entity state={} playerEntry={}", state.getUuid(), this.playerEntry != null);

        if (this.onInitialized != null) {
            this.onInitialized.run();
            this.onInitialized = null;
        }
    }

    public void refreshFromState() {
        this.state.getInventory().copyTo(this.getInventory());
        NeoSyncDebug.info("shell-entity", "refreshFromState state={} progress={} pos={}", this.state.getUuid(), this.state.getProgress(), this.state.getPos());
    }

    public void onInitialized(Runnable runnable) {
        if (this.state == null) {
            this.onInitialized = runnable;
        } else if (runnable != null) {
            runnable.run();
        }
    }

    public ShellState getState() {
        return this.state;
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource damageSource) {
        // Don't drop items when shell is destroyed.
    }

    @Override
    public boolean isCreative() {
        return true;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public boolean isModelPartShown(PlayerModelPart modelPart) {
        return true;
    }

    @Override
    public PlayerInfo getPlayerInfo() {
        return this.playerEntry;
    }

    @Override
    public PlayerSkin getSkin() {
        GameProfile profile = this.getGameProfile();
        PlayerSkin baseSkin;
        if (profile.getProperties().containsKey("textures")) {
            baseSkin = Minecraft.getInstance().getSkinManager().getInsecureSkin(profile);
        } else {
            PlayerInfo info = this.getPlayerInfo();
            baseSkin = info != null ? info.getSkin() : super.getSkin();
        }

        if (this.state != null
                && SyncConfig.SPEC.isLoaded()
                && SyncConfig.getInstance().enableTechnobladeEasterEgg()
                && SyncConfig.getInstance().renderTechnobladeCape()
                && SyncConfig.getInstance().isTechnoblade(this.state.getOwnerUuid())) {
            return new PlayerSkin(
                    baseSkin.texture(),
                    baseSkin.textureUrl(),
                    TECHNOBLADE_CAPE_TEXTURE,
                    baseSkin.elytraTexture(),
                    baseSkin.model(),
                    baseSkin.secure()
            );
        }

        return baseSkin;
    }

    private static GameProfile buildProfile(ShellState state) {
        GameProfile profile = new GameProfile(state.getOwnerUuid(), state.getOwnerName());
        String value = state.getTextureValue();
        if (value != null) {
            profile.getProperties().put("textures", new Property("textures", value, state.getTextureSignature()));
        }
        return profile;
    }

    private static PlayerInfo getPlayerEntry(ShellState state) {
        PlayerInfo entry = PLAYER_ENTRY_CACHE.getIfPresent(state.getOwnerUuid());

        if (entry == null) {
            Minecraft client = Minecraft.getInstance();
            ClientPacketListener networkHandler = client.getConnection();

            if (networkHandler != null) {
                entry = networkHandler.getPlayerInfo(state.getOwnerUuid());

                if (entry == null) {
                    entry = networkHandler.getPlayerInfo(state.getOwnerName());
                }
            }

            if (entry != null) {
                PLAYER_ENTRY_CACHE.put(state.getOwnerUuid(), entry);
            }
        }

        return entry;
    }
}
