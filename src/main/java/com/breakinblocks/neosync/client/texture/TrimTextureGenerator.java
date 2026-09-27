package com.breakinblocks.neosync.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public final class TrimTextureGenerator implements TextureGenerator {
    private final ResourceLocation baseTextureId;
    private final ResourceLocation trimmedTextureId;

    public TrimTextureGenerator(ResourceLocation baseTextureId, ResourceLocation trimmedTextureId) {
        this.baseTextureId = baseTextureId;
        this.trimmedTextureId = trimmedTextureId;
    }

    public ResourceLocation getTexture(@Nullable DyeColor color, ResourceLocation fallback) {
        if (color == null) {
            return fallback;
        }

        ResourceLocation[] textures = GeneratedTextureManager.getTextures(this);
        return textures.length > color.getId() ? textures[color.getId()] : fallback;
    }

    @Override
    public Stream<AbstractTexture> generateTextures() {
        NativeImage base = load(this.baseTextureId);
        NativeImage trimmed = load(this.trimmedTextureId);
        if (base == null || trimmed == null || base.getWidth() != trimmed.getWidth() || base.getHeight() != trimmed.getHeight()) {
            if (base != null) {
                base.close();
            }
            if (trimmed != null) {
                trimmed.close();
            }
            return Stream.empty();
        }

        try {
            int cyanMax = maxComponent(DyeColor.CYAN.getTextureDiffuseColor());
            List<AbstractTexture> textures = new ArrayList<>(16);
            for (int i = 0; i < 16; ++i) {
                textures.add(generate(base, trimmed, DyeColor.byId(i), cyanMax));
            }
            return textures.stream();
        } finally {
            base.close();
            trimmed.close();
        }
    }

    private static AbstractTexture generate(NativeImage base, NativeImage trimmed, DyeColor dye, int cyanMax) {
        int width = trimmed.getWidth();
        int height = trimmed.getHeight();
        NativeImage img = new NativeImage(width, height, false);

        int dyeColor = dye.getTextureDiffuseColor();
        int dyeR = (dyeColor >> 16) & 0xFF;
        int dyeG = (dyeColor >> 8) & 0xFF;
        int dyeB = dyeColor & 0xFF;
        int dyeMax = Math.max(dyeR, Math.max(dyeG, dyeB));
        float maxScale = dyeMax == 0 ? 0F : 255F / dyeMax;

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int trimPixel = trimmed.getPixelRGBA(x, y);
                if (trimPixel == base.getPixelRGBA(x, y)) {
                    img.setPixelRGBA(x, y, trimPixel);
                    continue;
                }

                int a = (trimPixel >> 24) & 0xFF;
                int r = trimPixel & 0xFF;
                int g = (trimPixel >> 8) & 0xFF;
                int b = (trimPixel >> 16) & 0xFF;

                float scale = Math.min(Math.max(r, Math.max(g, b)) / (float)cyanMax, maxScale);
                int outR = Math.round(dyeR * scale);
                int outG = Math.round(dyeG * scale);
                int outB = Math.round(dyeB * scale);
                img.setPixelRGBA(x, y, (a << 24) | (outB << 16) | (outG << 8) | outR);
            }
        }

        return new DynamicTexture(img);
    }

    private static int maxComponent(int argb) {
        return Math.max((argb >> 16) & 0xFF, Math.max((argb >> 8) & 0xFF, argb & 0xFF));
    }

    @Nullable
    private static NativeImage load(ResourceLocation id) {
        try (InputStream stream = Minecraft.getInstance().getResourceManager().open(id)) {
            return NativeImage.read(stream);
        } catch (IOException e) {
            return null;
        }
    }
}
