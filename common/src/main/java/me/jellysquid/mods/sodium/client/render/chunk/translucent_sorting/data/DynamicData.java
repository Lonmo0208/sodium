package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.trigger.GeometryPlanes;
import net.minecraft.core.SectionPos;
import org.joml.Vector3dc;

public abstract class DynamicData extends MixedDirectionData {
    private GeometryPlanes geometryPlanes;
    private final Vector3dc initialCameraPos;

    DynamicData(SectionPos sectionPos, VertexRange range, int quadCount, GeometryPlanes geometryPlanes, Vector3dc initialCameraPos) {
        super(sectionPos, range, quadCount);
        this.geometryPlanes = geometryPlanes;
        this.initialCameraPos = initialCameraPos;
    }

    @Override
    public SortType getSortType() {
        return SortType.DYNAMIC;
    }

    public GeometryPlanes getGeometryPlanes() {
        return this.geometryPlanes;
    }

    public void discardGeometryPlanes() {
        this.geometryPlanes = null;
    }

    public Vector3dc getInitialCameraPos() {
        return this.initialCameraPos;
    }
}
