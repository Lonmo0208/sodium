package me.jellysquid.mods.sodium.api.vertex.format;

import com.mojang.blaze3d.vertex.VertexFormat;
import me.jellysquid.mods.sodium.api.internal.DependencyInjection;

public interface VertexFormatRegistry {
    VertexFormatRegistry INSTANCE = DependencyInjection.load(VertexFormatRegistry.class,
            "me.jellysquid.mods.sodium.client.render.vertex.VertexFormatRegistryImpl");

    static VertexFormatRegistry instance() {
        return INSTANCE;
    }

    VertexFormatDescription get(VertexFormat format);
}