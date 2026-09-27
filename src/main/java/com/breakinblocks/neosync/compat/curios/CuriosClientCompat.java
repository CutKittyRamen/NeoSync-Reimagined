package com.breakinblocks.neosync.compat.curios;

import com.mojang.logging.LogUtils;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public final class CuriosClientCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CuriosClientCompat() {}

    @Nullable
    @SuppressWarnings("unchecked")
    public static RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> createShellLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        if (!CuriosCompat.isLoaded()) {
            return null;
        }

        try {
            Class<?> cls = Class.forName("com.breakinblocks.neosync.compat.curios.client.ShellCuriosLayer");
            return (RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>)
                    cls.getDeclaredConstructor(RenderLayerParent.class).newInstance(parent);
        } catch (Throwable t) {
            LOGGER.warn("Curios is loaded but stored shells will render without their curios", t);
            return null;
        }
    }
}
