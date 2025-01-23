package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Arrays;

/**
 * Represents a quad for the purposes of translucency sorting. Called TQuad to
 * avoid confusion with other quad classes.
 */
public class TQuad {
    /**
     * The quantization factor with which the normals are quantized such that there
     * are fewer possible unique normals. The factor describes the number of steps
     * in each direction per dimension that the components of the normals can have.
     * It determines the density of the grid on the surface of a unit cube centered
     * at the origin onto which the normals are projected. The normals are snapped
     * to the nearest grid point.
     */
    private static final int QUANTIZATION_FACTOR = 4;

    private ModelQuadFacing facing;
    private final float[] extents;
    private final int packedNormal;
    private float dotProduct;
    private Vector3fc center; // null on aligned quads
    private Vector3fc quantizedNormal;

    private TQuad(ModelQuadFacing facing, float[] extents, Vector3fc center, int packedNormal) {
        this.facing = facing;
        this.extents = extents;
        this.center = center;
        this.packedNormal = packedNormal;

        if (this.facing.isAligned()) {
            this.dotProduct = getAlignedDotProduct(this.facing, this.extents);
        } else {
            float normX = NormI8.unpackX(this.packedNormal);
            float normY = NormI8.unpackY(this.packedNormal);
            float normZ = NormI8.unpackZ(this.packedNormal);
            this.dotProduct = this.getCenter().dot(normX, normY, normZ);
        }
    }

    private static float getAlignedDotProduct(ModelQuadFacing facing, float[] extents) {
        return extents[facing.ordinal()] * facing.getSign();
    }

    static TQuad fromAligned(ModelQuadFacing facing, float[] extents, Vector3fc center) {
        return new TQuad(facing, extents, center, ModelQuadFacing.PACKED_ALIGNED_NORMALS[facing.ordinal()]);
    }

    static TQuad fromUnaligned(ModelQuadFacing facing, float[] extents, Vector3fc center, int packedNormal) {
        return new TQuad(facing, extents, center, packedNormal);
    }

    public ModelQuadFacing getFacing() {
        return this.facing;
    }

    /**
     * Calculates the facing of the quad based on the quantized normal. This updates the dot product to be consistent with the new facing. Since this method computed and allocates the quantized normal, it should be used sparingly and only when the quantized normal is calculated anyway. Additionally, it can modify the facing and not product of the quad which the caller should be aware of.
     *
     * @return the (potentially changed) facing of the quad
     */
    public ModelQuadFacing useQuantizedFacing() {
        if (!this.facing.isAligned()) {
            // quantize the normal, get the new facing and get fix the dot product to match
            this.getQuantizedNormal();
            this.facing = ModelQuadFacing.fromNormal(this.quantizedNormal.x(), this.quantizedNormal.y(), this.quantizedNormal.z());
            if (this.facing.isAligned()) {
                this.dotProduct = getAlignedDotProduct(this.facing, this.extents);
            } else {
                this.dotProduct = this.getCenter().dot(this.quantizedNormal);
            }
        }

        return this.facing;
    }

    public float[] getExtents() {
        return this.extents;
    }

    public Vector3fc getCenter() {
        // calculate aligned quad center on demand
        if (this.center == null) {
            this.center = new Vector3f(
                    (this.extents[0] + this.extents[3]) / 2,
                    (this.extents[1] + this.extents[4]) / 2,
                    (this.extents[2] + this.extents[5]) / 2);
        }
        return this.center;
    }

    public float getDotProduct() {
        return this.dotProduct;
    }

    public int getPackedNormal() {
        return this.packedNormal;
    }

    public Vector3fc getQuantizedNormal() {
        if (this.quantizedNormal == null) {
            if (this.facing.isAligned()) {
                this.quantizedNormal = this.facing.getAlignedNormal();
            } else {
                this.computeQuantizedNormal();
            }
        }
        return this.quantizedNormal;
    }

    private void computeQuantizedNormal() {
        float normX = NormI8.unpackX(this.packedNormal);
        float normY = NormI8.unpackY(this.packedNormal);
        float normZ = NormI8.unpackZ(this.packedNormal);

        // normalize onto the surface of a cube by dividing by the length of the longest
        // component
        float infNormLength = Math.max(Math.abs(normX), Math.max(Math.abs(normY), Math.abs(normZ)));
        if (infNormLength != 0 && infNormLength != 1) {
            normX /= infNormLength;
            normY /= infNormLength;
            normZ /= infNormLength;
        }

        // quantize the coordinates on the surface of the cube.
        // in each axis the number of values is 2 * QUANTIZATION_FACTOR + 1.
        // the total number of normals is the number of points on that cube's surface.
        var normal = new Vector3f(
                (int) (normX * QUANTIZATION_FACTOR),
                (int) (normY * QUANTIZATION_FACTOR),
                (int) (normZ * QUANTIZATION_FACTOR));
        normal.normalize();
        this.quantizedNormal = normal;
    }

    int getQuadHash() {
        // the hash code needs to be particularly collision resistant
        int result = 1;
        result = 31 * result + Arrays.hashCode(this.extents);
        if (this.facing.isAligned()) {
            result = 31 * result + this.facing.hashCode();
        } else {
            result = 31 * result + this.packedNormal;
        }
        result = 31 * result + Float.hashCode(this.dotProduct);
        return result;
    }

    public boolean extentsEqual(float[] other) {
        return extentsEqual(this.extents, other);
    }

    public static boolean extentsEqual(float[] a, float[] b) {
        for (int i = 0; i < 6; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean extentsIntersect(float[] extentsA, float[] extentsB) {
        for (int axis = 0; axis < 3; axis++) {
            var opposite = axis + 3;

            if (extentsA[axis] <= extentsB[opposite]
                    || extentsB[axis] <= extentsA[opposite]) {
                return false;
            }
        }
        return true;
    }

    public static boolean extentsIntersect(TQuad a, TQuad b) {
        return extentsIntersect(a.extents, b.extents);
    }
}
