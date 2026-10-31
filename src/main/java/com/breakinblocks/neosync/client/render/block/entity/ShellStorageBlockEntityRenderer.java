package com.breakinblocks.neosync.client.render.block.entity;

import com.breakinblocks.neosync.api.shell.ShellState;
import com.breakinblocks.neosync.client.entity.ClientShellEntities;
import com.breakinblocks.neosync.common.block.entity.ShellEntity;
import com.breakinblocks.neosync.client.model.ShellStorageGeoModel;
import com.breakinblocks.neosync.common.block.AbstractShellContainerBlock;
import com.breakinblocks.neosync.common.block.SyncBlocks;
import com.breakinblocks.neosync.common.block.entity.ShellStorageBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.GeoBone;

@OnlyIn(Dist.CLIENT)
public class ShellStorageBlockEntityRenderer extends AbstractShellContainerBlockEntityRenderer<ShellStorageBlockEntity> {
    private static final BlockState DEFAULT_STATE = SyncBlocks.SHELL_STORAGE.get().defaultBlockState()
            .setValue(AbstractShellContainerBlock.HALF, DoubleBlockHalf.LOWER)
            .setValue(AbstractShellContainerBlock.FACING, Direction.SOUTH)
            .setValue(AbstractShellContainerBlock.OPEN, false);

    public ShellStorageBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new ShellStorageGeoModel());
    }

    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay, int colour) {
        if ("ledLight".equals(bone.getName()) && this.animatable != null) {
            DyeColor indicatorColor = this.animatable.getIndicatorColor();
            colour = 0xFF000000 | (indicatorColor.getTextureDiffuseColor() & 0x00FFFFFF);
        }
        super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour);
    }

    @Override
    protected ShellEntity createEntity(ShellState shellState, ShellStorageBlockEntity blockEntity, float tickDelta) {
        ShellEntity entity = ClientShellEntities.get(shellState);
        entity.isActive = shellState.getProgress() >= ShellState.PROGRESS_DONE;
        entity.pitchProgress = entity.isActive ? blockEntity.getConnectorProgress(tickDelta) : 0;
        return entity;
    }

    @Override
    protected BlockState getDefaultState() {
        return DEFAULT_STATE;
    }
}
