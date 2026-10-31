package com.breakinblocks.neosync.common.block.entity;

import com.breakinblocks.neosync.api.shell.ShellState;
import com.breakinblocks.neosync.common.block.InteractorBlock;
import com.breakinblocks.neosync.common.config.SyncConfig;
import com.breakinblocks.neosync.common.utils.WorldUtil;
import com.breakinblocks.neosync.common.utils.nbt.SyncRegistries;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

import java.util.UUID;

public class InteractorBlockEntity extends BlockEntity {

    private Object peripheral;

    public InteractorBlockEntity(BlockPos pos, BlockState state) {
        super(SyncBlockEntities.INTERACTOR.get(), pos, state);
    }

    public final boolean interact(String uuidString) {
        if (!(this.level instanceof ServerLevel serverLevel)) return false;

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            return false;
        }

        Direction facing = this.getBlockState().getValue(InteractorBlock.FACING);
        BlockPos targetPos = this.worldPosition.relative(facing);
        if (!(serverLevel.getBlockEntity(targetPos) instanceof ShellConstructorBlockEntity targetConstructor)) {
            return false;
        }

        ShellConstructorBlockEntity constructor = (ShellConstructorBlockEntity) targetConstructor.getBottomPart().orElse(null);
        if (constructor == null || constructor.getShellState() != null) {
            return false;
        }

        MinecraftServer server = serverLevel.getServer();
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(uuid);
        BlockPos constructorPos = constructor.getBlockPos();

        ShellState shellState;
        if (onlinePlayer != null) {
            shellState = ShellState.empty(onlinePlayer, constructorPos, constructor.getColor());
            shellState.bindTo(constructor);
            boolean isCreative = !onlinePlayer.gameMode.isSurvival();
            if (isCreative && SyncConfig.getInstance().enableInstantShellConstruction()) {
                shellState.setProgress(ShellState.PROGRESS_DONE);
            }
        } else {
            GameProfile profile = server.getProfileCache() != null ? server.getProfileCache().get(uuid).orElse(null) : null;
            String ownerName = profile != null && profile.getName() != null ? profile.getName() : uuid.toString();
            String textureValue = null;
            String textureSignature = null;
            if (profile != null) {
                Property textures = profile.getProperties().get("textures").stream().findFirst().orElse(null);
                if (textures != null) {
                    textureValue = textures.value();
                    textureSignature = textures.signature();
                }
            }

            CompoundTag shellTag = new CompoundTag();
            shellTag.putUUID("uuid", UUID.randomUUID());
            shellTag.putInt("color", constructor.getColor() == null ? -1 : constructor.getColor().getId());
            shellTag.putFloat("progress", ShellState.PROGRESS_START);
            shellTag.putBoolean("isArtificial", true);
            shellTag.putBoolean("isVirtual", false);
            shellTag.putBoolean("isTemporary", false);
            shellTag.putBoolean("manualOnly", false);
            shellTag.putUUID("ownerUuid", uuid);
            shellTag.putString("ownerName", ownerName);
            if (textureValue != null) {
                shellTag.putString("textureValue", textureValue);
            }
            if (textureSignature != null) {
                shellTag.putString("textureSignature", textureSignature);
            }
            shellTag.putFloat("health", 20.0F);
            shellTag.putInt("gameMode", server.getDefaultGameType().getId());
            shellTag.put("inventory", new ListTag());
            shellTag.put("components", new CompoundTag());
            shellTag.putInt("foodLevel", 20);
            shellTag.putFloat("saturationLevel", 5.0F);
            shellTag.putFloat("exhaustion", 0.0F);
            shellTag.putInt("experienceLevel", 0);
            shellTag.putFloat("experienceProgress", 0.0F);
            shellTag.putInt("totalExperience", 0);
            shellTag.putString("world", WorldUtil.getId(serverLevel).toString());
            shellTag.putFloat("yawDelta", 0.0F);

            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", constructorPos.getX());
            posTag.putInt("y", constructorPos.getY());
            posTag.putInt("z", constructorPos.getZ());
            shellTag.put("pos", posTag);

            SyncRegistries.push(serverLevel.registryAccess());
            try {
                shellState = ShellState.fromNbt(shellTag);
            } finally {
                SyncRegistries.pop();
            }
            shellState.bindTo(constructor);
        }

        constructor.setShellState(shellState);
        constructor.setChanged();
        constructor.sync();
        return true;
    }

    private final class InteractorPeripheral implements IPeripheral {
        @Override
        public String getType() {
            return "interactor";
        }

        @Override
        public java.util.Set<String> getAdditionalTypes() {
            return java.util.Collections.singleton("interactor");
        }

        @Override
        public boolean equals(IPeripheral other) {
            return this == other;
        }

        @Override
        public Object getTarget() {
            return InteractorBlockEntity.this;
        }

        @LuaFunction(mainThread = true)
        public final boolean interact(String uuidString) {
            return InteractorBlockEntity.this.interact(uuidString);
        }
    }

    public Object getPeripheral() {
        if (!ModList.get().isLoaded("computercraft")) {
            return null;
        }
        if (this.peripheral == null) {
            this.peripheral = new InteractorPeripheral();
        }
        return this.peripheral;
    }
}
