package com.breakinblocks.neosync.common.block.entity;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SamplerBlockEntity extends BlockEntity {

    private UUID sampledUUID;
    private final Set<Object> computers = ConcurrentHashMap.newKeySet();
    private Object peripheral;

    public SamplerBlockEntity(BlockPos pos, BlockState state) {
        super(SyncBlockEntities.SAMPLER.get(), pos, state);
    }

    public void setSampledUUID(UUID uuid) {
        this.sampledUUID = uuid;
        if (uuid != null && ModList.get().isLoaded("computercraft")) {
            queueSampledEvent(uuid.toString());
        }
    }

    private void queueSampledEvent(String uuidStr) {
        for (Object obj : this.computers) {
            if (obj instanceof IComputerAccess computer) {
                computer.queueEvent("player_sampled", computer.getAttachmentName(), uuidStr);
            }
        }
    }

    public final String getSampledUUID() {
        UUID uuid = this.sampledUUID;
        this.sampledUUID = null;
        return uuid != null ? uuid.toString() : null;
    }

    private final class SamplerPeripheral implements IPeripheral {
        @Override
        public String getType() {
            return "sampler";
        }

        @Override
        public java.util.Set<String> getAdditionalTypes() {
            return java.util.Collections.singleton("sampler");
        }

        @Override
        public void attach(IComputerAccess computer) {
            SamplerBlockEntity.this.computers.add(computer);
        }

        @Override
        public void detach(IComputerAccess computer) {
            SamplerBlockEntity.this.computers.remove(computer);
        }

        @Override
        public boolean equals(IPeripheral other) {
            return this == other;
        }

        @Override
        public Object getTarget() {
            return SamplerBlockEntity.this;
        }

        @LuaFunction(mainThread = true)
        public final String getSampledUUID() {
            return SamplerBlockEntity.this.getSampledUUID();
        }
    }

    public Object getPeripheral() {
        if (!ModList.get().isLoaded("computercraft")) {
            return null;
        }
        if (this.peripheral == null) {
            this.peripheral = new SamplerPeripheral();
        }
        return this.peripheral;
    }
}
