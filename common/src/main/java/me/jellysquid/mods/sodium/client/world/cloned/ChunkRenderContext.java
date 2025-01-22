package me.jellysquid.mods.sodium.client.world.cloned;

import me.jellysquid.mods.sodium.client.services.SodiumModelDataContainer;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class ChunkRenderContext {
    private final SectionPos origin;
    private final ClonedChunkSection[] sections;
    private final BoundingBox volume;
    private final SodiumModelDataContainer modelData;

    public ChunkRenderContext(SectionPos origin, ClonedChunkSection[] sections, BoundingBox volume, SodiumModelDataContainer modelData) {
        this.origin = origin;
        this.sections = sections;
        this.volume = volume;
        this.modelData = modelData;
    }

    public ClonedChunkSection[] getSections() {
        return this.sections;
    }

    public SectionPos getOrigin() {
        return this.origin;
    }

    public BoundingBox getVolume() {
        return this.volume;
    }

    public SodiumModelDataContainer getModelData() {
        return modelData;
    }
}
