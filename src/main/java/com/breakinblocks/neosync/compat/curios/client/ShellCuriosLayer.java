package com.breakinblocks.neosync.compat.curios.client;

import com.breakinblocks.neosync.common.block.entity.ShellEntity;
import com.breakinblocks.neosync.compat.curios.CuriosShellStateComponent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class ShellCuriosLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<ResourceLocation> BROKEN = ConcurrentHashMap.newKeySet();

    private final RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent;

    public ShellCuriosLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
        this.parent = parent;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(player instanceof ShellEntity shell)) {
            return;
        }

        CuriosShellStateComponent component = shell.getState().getComponent().as(CuriosShellStateComponent.class);
        if (component == null) {
            return;
        }

        for (CuriosShellStateComponent.Worn worn : component.getWorn()) {
            if (BROKEN.contains(BuiltInRegistries.ITEM.getKey(worn.stack().getItem()))) {
                continue;
            }

            poseStack.pushPose();
            try {
                SlotContext slotContext = new SlotContext(worn.identifier(), player, worn.index(), worn.cosmetic(), worn.renderable());
                CuriosRendererRegistry.getRenderer(worn.stack().getItem()).ifPresent(renderer -> renderer.render(
                        worn.stack(), slotContext, poseStack, this.parent, bufferSource, light,
                        limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch));
            } catch (Throwable t) {
                this.reportBroken(worn.stack(), t);
            } finally {
                poseStack.popPose();
            }
        }
    }

    private void reportBroken(ItemStack stack, Throwable t) {
        ResourceLocation item = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (BROKEN.add(item)) {
            LOGGER.warn("Curio {} could not be rendered on a stored shell and will be skipped from now on", item, t);
        }
    }
}
