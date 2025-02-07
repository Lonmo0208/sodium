package me.jellysquid.mods.sodium.client.render.immediate.model;

import com.mojang.blaze3d.vertex.PoseStack;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorU8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ModelVertex;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public class BakedModelEncoder {
    private static int mergeLighting(int stored, int calculated) {
        if (stored == 0) return calculated;

        int blockLight = Math.max(stored & 0xFFFF, calculated & 0xFFFF);
        int skyLight = Math.max((stored >> 16) & 0xFFFF, (calculated >> 16) & 0xFFFF);
        return blockLight | (skyLight << 16);
    }

    public static void writeQuadVertices(VertexBufferWriter writer, PoseStack.Pose matrices, ModelQuadView quad, int color, int light, int overlay) {
        // 获取顶点数组
        float[] vertices = quad.getVertices();
        int vertexCount = vertices.length;
        float[] defaultBrightness = new float[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            defaultBrightness[i] = 1.0f;
        }
        writeQuadVertices(writer, matrices, quad,
                ColorU8.byteToNormalizedFloat(ColorABGR.unpackRed(color)),
                ColorU8.byteToNormalizedFloat(ColorABGR.unpackGreen(color)),
                ColorU8.byteToNormalizedFloat(ColorABGR.unpackBlue(color)),
                ColorU8.byteToNormalizedFloat(ColorABGR.unpackAlpha(color)),
                defaultBrightness,
                false,
                new int[]{light},
                overlay);
    }

    public static void writeQuadVertices(VertexBufferWriter writer, PoseStack.Pose matrices, ModelQuadView quad, float r, float g, float b, float a, float[] brightnessTable, boolean colorize, int[] light, int overlay) {
        Matrix3f matNormal = matrices.normal();
        Matrix4f matPosition = matrices.pose();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * ModelVertex.STRIDE);
            long ptr = buffer;

            // 如果 brightnessTable 为 null，使用默认亮度
            if (brightnessTable == null) {
                float[] vertices = quad.getVertices();
                int vertexCount = vertices.length;
                brightnessTable = new float[vertexCount];
                for (int i = 0; i < vertexCount; i++) {
                    brightnessTable[i] = 1.0f;
                }
            }

            // 确保 brightnessTable 的长度与 quad 的顶点数相匹配
            float[] vertices = quad.getVertices();
            int vertexCount = vertices.length;
            if (brightnessTable.length < vertexCount) {
                // 如果 brightnessTable 的长度小于需要的顶点数，扩展数组
                float[] newBrightnessTable = new float[vertexCount];
                for (int i = 0; i < brightnessTable.length; i++) {
                    newBrightnessTable[i] = brightnessTable[i];
                }
                for (int i = brightnessTable.length; i < vertexCount; i++) {
                    newBrightnessTable[i] = 1.0f;
                }
                brightnessTable = newBrightnessTable;
            }

            for (int i = 0; i < vertexCount; i++) {
                // 位置向量
                float x = quad.getX(i);
                float y = quad.getY(i);
                float z = quad.getZ(i);

                // 变换后的位置向量
                float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
                float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
                float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);

                // 法向量变换
                int normal = MatrixHelper.transformNormal(matNormal, true, quad.getAccurateNormal(i));

                // 计算光照
                int newLight = mergeLighting(quad.getLight(i), light[i]);

                // 计算颜色
                float fR, fG, fB;
                if (colorize) {
                    int color = quad.getColor(i);
                    float oR = ColorU8.byteToNormalizedFloat(ColorABGR.unpackRed(color));
                    float oG = ColorU8.byteToNormalizedFloat(ColorABGR.unpackGreen(color));
                    float oB = ColorU8.byteToNormalizedFloat(ColorABGR.unpackBlue(color));

                    fR = oR * brightnessTable[i] * r;
                    fG = oG * brightnessTable[i] * g;
                    fB = oB * brightnessTable[i] * b;
                } else {
                    fR = brightnessTable[i] * r;
                    fG = brightnessTable[i] * g;
                    fB = brightnessTable[i] * b;
                }

                int finalColor = ColorABGR.pack(fR, fG, fB, a);

                ModelVertex.write(ptr, xt, yt, zt, finalColor, quad.getTexU(i), quad.getTexV(i), overlay, newLight, normal);
                ptr += ModelVertex.STRIDE;
            }

            writer.push(stack, buffer, vertexCount, ModelVertex.FORMAT);
        }
    }
}