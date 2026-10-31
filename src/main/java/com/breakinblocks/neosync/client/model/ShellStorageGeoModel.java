package com.breakinblocks.neosync.client.model;

import com.breakinblocks.neosync.NeoSync;
import com.breakinblocks.neosync.client.texture.TrimTextureGenerator;
import com.breakinblocks.neosync.common.block.entity.ShellStorageBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animation.AnimationState;

@OnlyIn(Dist.CLIENT)
public class ShellStorageGeoModel extends AbstractShellContainerGeoModel<ShellStorageBlockEntity> {
    private static final ResourceLocation MODEL_ID = ResourceLocation.fromNamespaceAndPath(NeoSync.MOD_ID, "geo/shell_storage.geo.json");
    private static final ResourceLocation ANIMATION_ID = ResourceLocation.fromNamespaceAndPath(NeoSync.MOD_ID, "animations/shell_storage.animation.json");
    public static final ResourceLocation SHELL_STORAGE_TEXTURE_ID = ResourceLocation.fromNamespaceAndPath(NeoSync.MOD_ID, "textures/block/shell_storage.png");
    public static final TrimTextureGenerator STORAGE_TRIM_TEXTURES = new TrimTextureGenerator(
            SHELL_STORAGE_TEXTURE_ID,
            ResourceLocation.fromNamespaceAndPath(NeoSync.MOD_ID, "textures/block/zero_point_shell_storage.png"));

    @Override
    public ResourceLocation getModelResource(ShellStorageBlockEntity animatable) {
        return MODEL_ID;
    }

    @Override
    public ResourceLocation getTextureResource(ShellStorageBlockEntity animatable) {
        return STORAGE_TRIM_TEXTURES.getTexture(animatable.getColor(), SHELL_STORAGE_TEXTURE_ID);
    }

    @Override
    public ResourceLocation getAnimationResource(ShellStorageBlockEntity animatable) {
        return ANIMATION_ID;
    }

    @Override
    public void setCustomAnimations(ShellStorageBlockEntity animatable, long instanceId, AnimationState<ShellStorageBlockEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        boolean inWorld = animatable.hasLevel();
        DoubleBlockHalf half = inWorld ? animatable.getBlockType(animatable.getBlockState()) : DoubleBlockHalf.LOWER;
        boolean showBottom = !inWorld || half == DoubleBlockHalf.LOWER;
        boolean showTop = !inWorld || half == DoubleBlockHalf.UPPER;

        this.getBone("led_panel").ifPresent(bone -> {
            bone.setHidden(!showBottom);
            bone.setChildrenHidden(!showBottom);
        });
        this.getBone("headConnector").ifPresent(bone -> bone.setHidden(!showTop));
    }
}
