package com.breakinblocks.neosync.common.block.entity;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class SamplerBlockEntity extends BlockEntity {

    private UUID sampledUUID;

    public SamplerBlockEntity(BlockPos pos, BlockState state) {
        super(SyncBlockEntities.SAMPLER.get(), pos, state);
    }

    public void setSampledUUID(UUID uuid) {
        this.sampledUUID = uuid;
        setChanged();
    }

    @LuaFunction
    public final String getSampledUUID() {
        return this.sampledUUID != null ? this.sampledUUID.toString() : null;
    }

    private Object peripheral;

    public Object getPeripheral() {
        if (peripheral == null) {
            peripheral = new IPeripheral() {
                @Override
                public String getType() {
                    return "sampler";
                }

                @Override
                public boolean equals(IPeripheral other) {
                    return this == other;
                }

                @Override
                public Object getTarget() {
                    return SamplerBlockEntity.this;
                }
            };
        }
        return peripheral;
    }
}
