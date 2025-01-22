package me.jellysquid.mods.sodium.neoforge.texture;

import me.jellysquid.mods.sodium.client.services.PlatformTextureAccess;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class NeoForgeTextureAccess implements PlatformTextureAccess {
    @Override
    public TextureAtlasSprite findInBlockAtlas(float u, float v) {
        return SpriteFinderCache.forBlockAtlas().find(u, v);
    }
}
