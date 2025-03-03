package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import net.minecraft.core.SectionPos;

/**
 * Super class for translucent data that contains an actual buffer.
 */
public abstract class PresentTranslucentData extends TranslucentData {
    protected final int quadCount;
    private int quadHash;

    PresentTranslucentData(SectionPos sectionPos, int quadCount) {
        super(sectionPos);
        this.quadCount = quadCount;
    }

    public abstract VertexRange[] getVertexRanges();

    public abstract Sorter getSorter();

    public void setQuadHash(int hash) {
        this.quadHash = hash;
    }

    public int getQuadHash() {
        return this.quadHash;
    }

    public int getQuadCount() {
        return this.quadCount;
    }
}
