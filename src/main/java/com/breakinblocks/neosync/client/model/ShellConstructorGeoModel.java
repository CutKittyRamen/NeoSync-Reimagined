package com.breakinblocks.neosync.client.model;

import com.breakinblocks.neosync.NeoSync;
import com.breakinblocks.neosync.api.shell.ShellState;
import com.breakinblocks.neosync.client.texture.TrimTextureGenerator;
import com.breakinblocks.neosync.common.block.entity.ShellConstructorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animation.AnimationState;

@OnlyIn(Dist.CLIENT)
public class ShellConstructorGeoModel extends AbstractShellContainerGeoModel<ShellConstructorBlockEntity> {
    private static final ResourceLocation MODEL_ID = ResourceLocation.fromNamespaceAndPath(NeoSync.MOD_ID, "geo/shell_constructor.geo.json");
    private static final ResourceLocation ANIMATION_ID = ResourceLocation.fromNamespaceAndPath(NeoSync.MOD_ID, "animations/shell_constructor.animation.json");
    public static final ResourceLocation SHELL_CONSTRUCTOR_TEXTURE_ID = ResourceLocation.fromNamespaceAndPath(NeoSync.MOD_ID, "textures/block/shell_constructor.png");
    public static final TrimTextureGenerator CONSTRUCTOR_TRIM_TEXTURES = new TrimTextureGenerator(
            SHELL_CONSTRUCTOR_TEXTURE_ID,
            ResourceLocation.fromNamespaceAndPath(NeoSync.MOD_ID, "textures/block/zero_point_shell_constructor.png"));

    @Override
    public ResourceLocation getModelResource(ShellConstructorBlockEntity animatable) {
        return MODEL_ID;
    }

    @Override
    public ResourceLocation getTextureResource(ShellConstructorBlockEntity animatable) {
        return CONSTRUCTOR_TRIM_TEXTURES.getTexture(animatable.getColor(), SHELL_CONSTRUCTOR_TEXTURE_ID);
    }

    @Override
    public ResourceLocation getAnimationResource(ShellConstructorBlockEntity animatable) {
        return ANIMATION_ID;
    }

    @Override
    public void setCustomAnimations(ShellConstructorBlockEntity animatable, long instanceId, AnimationState<ShellConstructorBlockEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        boolean inWorld = animatable.hasLevel();
        DoubleBlockHalf half = inWorld ? animatable.getBlockType(animatable.getBlockState()) : DoubleBlockHalf.LOWER;
        boolean showInnerParts = inWorld && half == DoubleBlockHalf.LOWER;

        this.getBone("printers").ifPresent(bone -> {
            bone.setHidden(!showInnerParts);
            bone.setChildrenHidden(!showInnerParts);
        });
        this.getBone("sprayers").ifPresent(bone -> {
            bone.setHidden(!showInnerParts);
            bone.setChildrenHidden(!showInnerParts);
        });
        this.getBone("sprayer_masts_lower").ifPresent(bone -> {
            ShellState state = animatable.getShellState();
            boolean showLowerMasts = showInnerParts && state != null && state.getProgress() > 0F && state.getProgress() < ShellState.PROGRESS_DONE;
            bone.setHidden(!showLowerMasts);
            bone.setChildrenHidden(!showLowerMasts);
        });
    }
}
