package me.jellysquid.mods.sodium.mixin.features.render.gui.font;

import com.mojang.blaze3d.vertex.VertexConsumer;
import me.jellysquid.mods.sodium.client.render.vertex.VertexConsumerUtils;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.vertex.format.common.GlyphVertex;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BakedGlyph.class)
public class BakedGlyphMixin {
    @Shadow
    @Final
    private float left;

    @Shadow
    @Final
    private float right;

    @Shadow
    @Final
    private float up;

    @Shadow
    @Final
    private float down;

    @Shadow
    @Final
    private float u0;

    @Shadow
    @Final
    private float v0;

    @Shadow
    @Final
    private float v1;

    @Shadow
    @Final
    private float u1;

    /**
     * @reason Use intrinsics
     * @author JellySquid
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void drawFast(boolean italic, float x, float y, Matrix4f matrix, VertexConsumer vertexConsumer, float red, float green, float blue, float alpha, int light, CallbackInfo ci) {
        var writer = VertexConsumerUtils.convertOrLog(vertexConsumer);

        if (writer == null) {
            return;
        }

        ci.cancel();

        float x1 = x + this.left;
        float x2 = x + this.right;
        float y1 = this.up - 3.0F;
        float y2 = this.down - 3.0F;
        float h1 = y + y1;
        float h2 = y + y2;
        float w1 = italic ? 1.0F - 0.25F * y1 : 0.0F;
        float w2 = italic ? 1.0F - 0.25F * y2 : 0.0F;

        int color = ColorABGR.pack(red, green, blue, alpha);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * GlyphVertex.STRIDE);
            long ptr = buffer;

            write(ptr, matrix, x1 + w1, h1, 0.0F, color, this.u0, this.v0, light);
            ptr += GlyphVertex.STRIDE;

            write(ptr, matrix, x1 + w2, h2, 0.0F, color, this.u0, this.v1, light);
            ptr += GlyphVertex.STRIDE;

            write(ptr, matrix, x2 + w2, h2, 0.0F, color, this.u1, this.v1, light);
            ptr += GlyphVertex.STRIDE;

            write(ptr, matrix, x2 + w1, h1, 0.0F, color, this.u1, this.v0, light);
            ptr += GlyphVertex.STRIDE;

            writer.push(stack, buffer, 4, GlyphVertex.FORMAT);
        }
    }

    @Unique
    private static void write(long buffer,
                              Matrix4f matrix, float x, float y, float z, int color, float u, float v, int light) {
        float x2 = MatrixHelper.transformPositionX(matrix, x, y, z);
        float y2 = MatrixHelper.transformPositionY(matrix, x, y, z);
        float z2 = MatrixHelper.transformPositionZ(matrix, x, y, z);

        GlyphVertex.put(buffer, x2, y2, z2, color, u, v, light);
    }

}
