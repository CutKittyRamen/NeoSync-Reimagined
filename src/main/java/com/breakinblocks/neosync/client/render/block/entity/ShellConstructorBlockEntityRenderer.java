package com.breakinblocks.neosync.client.render.block.entity;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.breakinblocks.neosync.NeoSync;
import com.breakinblocks.neosync.api.shell.ShellState;
import com.breakinblocks.neosync.client.model.AbstractShellContainerModel;
import com.breakinblocks.neosync.client.model.ShellConstructorModel;
import com.breakinblocks.neosync.client.texture.TrimTextureGenerator;
import com.breakinblocks.neosync.common.block.AbstractShellContainerBlock;
import com.breakinblocks.neosync.common.block.SyncBlocks;
import com.breakinblocks.neosync.common.block.entity.ShellConstructorBlockEntity;
import com.breakinblocks.neosync.common.block.entity.ShellEntity;

@OnlyIn(Dist.CLIENT)
public class ShellConstructorBlockEntityRenderer extends AbstractShellContainerBlockEntityRenderer<ShellConstructorBlockEntity> {
    private static final ResourceLocation SHELL_CONSTRUCTOR_TEXTURE_ID = ResourceLocation.fromNamespaceAndPath(NeoSync.MOD_ID, "textures/block/shell_constructor.png");
    protected static final TrimTextureGenerator CONSTRUCTOR_TRIM_TEXTURES = new TrimTextureGenerator(
            SHELL_CONSTRUCTOR_TEXTURE_ID,
            ResourceLocation.fromNamespaceAndPath(NeoSync.MOD_ID, "textures/block/zero_point_shell_constructor.png"));
    private static final BlockState DEFAULT_STATE = SyncBlocks.SHELL_CONSTRUCTOR.get().defaultBlockState()
            .setValue(AbstractShellContainerBlock.HALF, DoubleBlockHalf.LOWER)
            .setValue(AbstractShellContainerBlock.FACING, Direction.SOUTH);

    private final ShellConstructorModel model;

    public ShellConstructorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        this.model = new ShellConstructorModel();
    }

    @Override
    protected AbstractShellContainerModel getShellContainerModel(ShellConstructorBlockEntity blockEntity, BlockState blockState, float tickDelta) {
        this.model.buildProgress = blockEntity.getShellState() == null ? 0F : blockEntity.getShellState().getProgress();
        this.model.showInnerParts = blockEntity.hasLevel();
        return this.model;
    }

    @Override
    protected ShellEntity createEntity(ShellState shellState, ShellConstructorBlockEntity blockEntity, float tickDelta) {
        ShellEntity entity = shellState.asEntity();
        entity.isActive = false;
        entity.pitchProgress = 0;
        return entity;
    }

    @Override
    protected BlockState getDefaultState() {
        return DEFAULT_STATE;
    }

    @Override
    protected ResourceLocation getTextureId(ShellConstructorBlockEntity blockEntity) {
        return CONSTRUCTOR_TRIM_TEXTURES.getTexture(blockEntity.getColor(), SHELL_CONSTRUCTOR_TEXTURE_ID);
    }
}