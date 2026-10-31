package com.breakinblocks.neosync;

import com.breakinblocks.neosync.common.block.SyncBlocks;
import com.breakinblocks.neosync.common.block.entity.SyncBlockEntities;
import com.breakinblocks.neosync.common.command.SyncCommands;
import com.breakinblocks.neosync.common.config.SyncConfig;
import com.breakinblocks.neosync.common.item.SyncCreativeTabs;
import com.breakinblocks.neosync.common.item.SyncItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import com.breakinblocks.neosync.compat.curios.CuriosCompat;
import com.breakinblocks.neosync.compat.mekanism.MekanismCompat;
import net.neoforged.fml.config.ModConfig;
import com.breakinblocks.neosync.compat.dragonsurvival.NeoSyncDragonSurvivalCompat;

@Mod(NeoSync.MOD_ID)
public class NeoSync {
    public static final String MOD_ID = "neosync";
    public static final String NAME = "NeoSync";

    public NeoSync(IEventBus modEventBus, ModContainer container, Dist dist) {
        container.registerConfig(ModConfig.Type.COMMON, SyncConfig.SPEC, MOD_ID + "-common.toml");

        SyncBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        SyncCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        SyncBlocks.BLOCKS.register(modEventBus);
        SyncItems.ITEMS.register(modEventBus);

        SyncCommands.init();

        CuriosCompat.init();
        MekanismCompat.init();
        NeoSyncDragonSurvivalCompat.init();
    }

    public static ResourceLocation locate(String location) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, location);
    }
}
