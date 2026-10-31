package com.breakinblocks.neosync.client.render.block.entity;

import com.breakinblocks.neosync.api.shell.ShellState;
import com.breakinblocks.neosync.common.block.entity.ShellEntity;
import com.breakinblocks.neosync.client.model.AbstractShellContainerGeoModel;
import com.breakinblocks.neosync.common.block.AbstractShellContainerBlock;
import com.breakinblocks.neosync.common.block.entity.AbstractShellContainerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractShellContainerBlockEntityRenderer<T extends AbstractShellContainerBlockEntity> extends GeoBlockRenderer<T> {
    protected final BlockEntityRendererProvider.Context context;

    public AbstractShellContainerBlockEntityRenderer(BlockEntityRendererProvider.Context context, AbstractShellContainerGeoModel<T> model) {
        super(model);
        this.context = context;
        this.withScale(0.5f);
    }

    @Override
    public void render(T blockEntity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        BlockState blockState = this.getBlockState(blockEntity);

        // Only render the shell entity from the lower half.
        if (blockEntity.hasLevel() && AbstractShellContainerBlock.isBottom(blockState)) {
            ShellState shellState = blockEntity.getShellState();

            if (shellState != null) {
                this.renderShell(shellState, blockEntity, tickDelta, blockState, matrices, vertexConsumers, light);
            }
        }

        super.render(blockEntity, tickDelta, matrices, vertexConsumers, light, overlay);
    }

    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int renderColor) {
        if (!isReRender && animatable.hasLevel() && !AbstractShellContainerBlock.isBottom(this.getBlockState(animatable))) {
            poseStack.translate(0.0D, -1.0D, 0.0D);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, renderColor);
    }

    protected void renderShell(ShellState shellState, T blockEntity, float tickDelta, BlockState blockState, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        float yaw = this.getBlockFacing(blockState).getOpposite().toYRot();
        ShellEntity shellEntity = this.createEntity(shellState, blockEntity, tickDelta);

        EntityRenderer<? super ShellEntity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(shellEntity);
        renderer.render(shellEntity, yaw, 0, matrices, vertexConsumers, light);
    }

    @Override
    protected Direction getFacing(T blockEntity) {
        return this.getBlockFacing(this.getBlockState(blockEntity)).getOpposite();
    }

    protected Direction getBlockFacing(BlockState blockState) {
        return blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    protected BlockState getBlockState(T blockEntity) {
        return blockEntity.hasLevel() ? blockEntity.getBlockState() : this.getDefaultState();
    }

    @Override
    public long getInstanceId(T animatable) {
        return animatable.hasLevel() ? super.getInstanceId(animatable) : -1L;
    }

    protected abstract ShellEntity createEntity(ShellState shellState, T blockEntity, float tickDelta);

    protected abstract BlockState getDefaultState();
}
