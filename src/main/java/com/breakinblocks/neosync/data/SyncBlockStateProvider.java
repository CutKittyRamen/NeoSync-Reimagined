package com.breakinblocks.neosync.data;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import com.breakinblocks.neosync.NeoSync;
import com.breakinblocks.neosync.common.block.SyncBlocks;

public class SyncBlockStateProvider extends BlockStateProvider {
    public SyncBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, NeoSync.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        ModelFile pumpkinModel = models().getExistingFile(ResourceLocation.withDefaultNamespace("block/pumpkin"));
        simpleBlock(SyncBlocks.SAMPLER.get(), pumpkinModel);
        simpleBlockItem(SyncBlocks.SAMPLER.get(), pumpkinModel);

        ModelFile jackOLanternModel = models().getExistingFile(ResourceLocation.withDefaultNamespace("block/jack_o_lantern"));
        directionalBlock(SyncBlocks.INTERACTOR.get(), jackOLanternModel);
        simpleBlockItem(SyncBlocks.INTERACTOR.get(), jackOLanternModel);
    }
}
