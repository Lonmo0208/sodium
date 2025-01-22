package me.jellysquid.mods.sodium.fabric.texture;

import me.jellysquid.mods.sodium.client.services.PlatformTextureAccess;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class FabricTextureAccess implements PlatformTextureAccess {
    @Override
    public TextureAtlasSprite findInBlockAtlas(float texU, float texV) {
        return SpriteFinderCache.forBlockAtlas().find(texU, texV);
    }
}
