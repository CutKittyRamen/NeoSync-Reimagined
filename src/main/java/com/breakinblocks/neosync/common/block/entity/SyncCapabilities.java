package com.breakinblocks.neosync.common.block.entity;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.breakinblocks.neosync.NeoSync;

@EventBusSubscriber(modid = NeoSync.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class SyncCapabilities {
    private SyncCapabilities() {}

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                SyncBlockEntities.SHELL_STORAGE.get(),
                (be, side) -> be);

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                SyncBlockEntities.SHELL_CONSTRUCTOR.get(),
                (be, side) -> be);

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                SyncBlockEntities.SHELL_CONSTRUCTOR.get(),
                (be, side) -> com.breakinblocks.neosync.common.config.SyncConfig.getInstance().enableFluidConsumption() ? be : null);

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                SyncBlockEntities.TREADMILL.get(),
                (be, side) -> be);

        try {
            @SuppressWarnings("unchecked")
            net.neoforged.neoforge.capabilities.BlockCapability peripheralCap =
                    (net.neoforged.neoforge.capabilities.BlockCapability) 
                    Class.forName("dan200.computercraft.api.peripheral.PeripheralCapabilities").getField("CAPABILITY").get(null);
            
            event.registerBlockEntity(
                    peripheralCap,
                    SyncBlockEntities.SAMPLER.get(),
                    (be, side) -> be.getPeripheral());

            event.registerBlockEntity(
                    peripheralCap,
                    SyncBlockEntities.INTERACTOR.get(),
                    (be, side) -> be.getPeripheral());
        } catch (Exception e) {
            // Ignore if CC is not present or capability changed
        }
    }
}
