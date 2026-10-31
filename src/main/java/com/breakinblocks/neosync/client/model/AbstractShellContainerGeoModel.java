package com.breakinblocks.neosync.client.model;

import com.breakinblocks.neosync.common.block.entity.AbstractShellContainerBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractShellContainerGeoModel<T extends AbstractShellContainerBlockEntity> extends GeoModel<T> {
    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture) {
        return RenderType.entityCutout(texture);
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        boolean inWorld = animatable.hasLevel();
        DoubleBlockHalf half = inWorld ? animatable.getBlockType(animatable.getBlockState()) : DoubleBlockHalf.LOWER;
        boolean showBottom = !inWorld || half == DoubleBlockHalf.LOWER;
        boolean showTop = !inWorld || half == DoubleBlockHalf.UPPER;

        this.getBone("bottom_half").ifPresent(bone -> bone.setHidden(!showBottom));
        this.getBone("top_half").ifPresent(bone -> bone.setHidden(!showTop));
        this.getBone("door_left_bottom").ifPresent(bone -> bone.setHidden(!showBottom));
        this.getBone("door_left_top").ifPresent(bone -> bone.setHidden(!showTop));
        this.getBone("door_right_bottom").ifPresent(bone -> bone.setHidden(!showBottom));
        this.getBone("door_right_top").ifPresent(bone -> bone.setHidden(!showTop));
    }
}
