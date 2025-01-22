package me.jellysquid.mods.sodium.neoforge.mixin;

import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FaceBakery.class)
public abstract class FaceBakeryMixin {
    @Inject(method = "fillVertex", at = @At(value = "HEAD"), cancellable = true)
    private void fillVertexEdit(int[] is, int i, Vector3f vector3f, TextureAtlasSprite textureAtlasSprite, BlockFaceUV blockFaceUV, CallbackInfo ci) {
        ci.cancel();
        // Revert the whole Forge fix.

        int j = i * 8;
        is[j] = Float.floatToRawIntBits(vector3f.x());
        is[j + 1] = Float.floatToRawIntBits(vector3f.y());
        is[j + 2] = Float.floatToRawIntBits(vector3f.z());
        is[j + 3] = -1;
        is[j + 4] = Float.floatToRawIntBits(textureAtlasSprite.getU(blockFaceUV.getU(i)));
        is[j + 4 + 1] = Float.floatToRawIntBits(textureAtlasSprite.getV(blockFaceUV.getV(i)));
    }
}