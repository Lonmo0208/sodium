package me.jellysquid.mods.sodium.client.render.immediate.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ModelVertex;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.apache.commons.lang3.ArrayUtils;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class EntityRenderer {
    // 原始常量定义
    private static final int NUM_CUBE_VERTICES = 8;
    private static final int NUM_CUBE_FACES = 6;
    private static final int NUM_FACE_VERTICES = 4;

    private static final int
            FACE_NEG_Y = 0, // DOWN
            FACE_POS_Y = 1, // UP
            FACE_NEG_Z = 2, // NORTH
            FACE_POS_Z = 3, // SOUTH
            FACE_NEG_X = 4, // WEST
            FACE_POS_X = 5; // EAST

    private static final int
            VERTEX_X1_Y1_Z1 = 0,
            VERTEX_X2_Y1_Z1 = 1,
            VERTEX_X2_Y2_Z1 = 2,
            VERTEX_X1_Y2_Z1 = 3,
            VERTEX_X1_Y1_Z2 = 4,
            VERTEX_X2_Y1_Z2 = 5,
            VERTEX_X2_Y2_Z2 = 6,
            VERTEX_X1_Y2_Z2 = 7;

    // 内存优化
    private static final long SCRATCH_BUFFER = MemoryUtil.nmemAlignedAlloc(64, NUM_CUBE_FACES * NUM_FACE_VERTICES * ModelVertex.STRIDE);
    private static int LAST_MATRIX_HASH;

    // 顶点数据缓存
    private static final Vector3f[] CUBE_CORNERS = createCubeCorners();
    private static final Vector3f[][][] POSITION_CACHE = buildPositionCache();
    private static final Vector2f[][] VERTEX_TEXTURES = new Vector2f[NUM_CUBE_FACES][NUM_FACE_VERTICES];
    private static final Vector2f[][] VERTEX_TEXTURES_MIRRORED = new Vector2f[NUM_CUBE_FACES][NUM_FACE_VERTICES];
    private static final int[][] NORMAL_CACHE = new int[2][NUM_CUBE_FACES];

    static {
        // 初始化纹理坐标数组
        for (int i = 0; i < NUM_CUBE_FACES; i++) {
            for (int j = 0; j < NUM_FACE_VERTICES; j++) {
                VERTEX_TEXTURES[i][j] = new Vector2f();
                VERTEX_TEXTURES_MIRRORED[i][j] = new Vector2f();
            }
        }

        initNormalCache();
    }

    // 保留原始渲染接口
    public static void render(PoseStack poseStack, VertexBufferWriter writer, ModelPart part, int light, int overlay, int color) {
        ModelPartData accessor = ModelPartData.from(part);
        if (!accessor.isVisible()) return;

        ModelCuboid[] cuboids = accessor.getCuboids();
        ModelPart[] children = accessor.getChildren();
        if (ArrayUtils.isEmpty(cuboids) && ArrayUtils.isEmpty(children)) return;

        poseStack.pushPose();
        part.translateAndRotate(poseStack);

        if (!accessor.isHidden()) {
            renderCuboids(poseStack.last(), writer, cuboids, light, overlay, color);
        }

        renderChildren(poseStack, writer, children, light, overlay, color);
        poseStack.popPose();
    }

    // 新增优化接口
    public static void renderCuboid(PoseStack.Pose matrices, VertexBufferWriter writer, ModelCuboid cuboid, int light, int overlay, int color) {
        updateNormalCache(matrices);
        prepareVertices(matrices, cuboid);

        int vertexCount = emitOptimizedQuads(cuboid, color, overlay, light);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            writer.push(stack, SCRATCH_BUFFER, vertexCount, ModelVertex.FORMAT);
        }
    }

    // 共享核心逻辑
    private static void renderCuboids(PoseStack.Pose matrices, VertexBufferWriter writer, ModelCuboid[] cuboids, int light, int overlay, int color) {
        updateNormalCache(matrices);

        for (ModelCuboid cuboid : cuboids) {
            prepareVertices(matrices, cuboid);

            int vertexCount = emitOptimizedQuads(cuboid, color, overlay, light);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                writer.push(stack, SCRATCH_BUFFER, vertexCount, ModelVertex.FORMAT);
            }
        }
    }

    // 修复的顶点发射逻辑
    private static int emitOptimizedQuads(ModelCuboid cuboid, int color, int overlay, int light) {
        int mirrorFlag = cuboid.mirror ? 1 : 0;
        int vertexCount = 0;
        long ptr = SCRATCH_BUFFER;

        for (int face = 0; face < NUM_CUBE_FACES; face++) {
            if (!cuboid.shouldDrawFace(face)) continue;

            final Vector3f[] positions = POSITION_CACHE[mirrorFlag][face];
            final Vector2f[] textures = mirrorFlag == 1 ? VERTEX_TEXTURES_MIRRORED[face] : VERTEX_TEXTURES[face];
            final int normal = NORMAL_CACHE[mirrorFlag][face];

            // 使用原始写入方式
            ModelVertex.write(ptr, positions[0].x, positions[0].y, positions[0].z, color, textures[0].x, textures[0].y, overlay, light, normal);
            ptr += ModelVertex.STRIDE;

            ModelVertex.write(ptr, positions[1].x, positions[1].y, positions[1].z, color, textures[1].x, textures[1].y, overlay, light, normal);
            ptr += ModelVertex.STRIDE;

            ModelVertex.write(ptr, positions[2].x, positions[2].y, positions[2].z, color, textures[2].x, textures[2].y, overlay, light, normal);
            ptr += ModelVertex.STRIDE;

            ModelVertex.write(ptr, positions[3].x, positions[3].y, positions[3].z, color, textures[3].x, textures[3].y, overlay, light, normal);
            ptr += ModelVertex.STRIDE;

            vertexCount += 4;
        }
        return vertexCount;
    }

    // 修复的初始化方法
    private static Vector3f[] createCubeCorners() {
        Vector3f[] corners = new Vector3f[NUM_CUBE_VERTICES];
        for (int i = 0; i < NUM_CUBE_VERTICES; i++) {
            corners[i] = new Vector3f();
        }
        return corners;
    }

    private static Vector3f[][][] buildPositionCache() {
        final int[][] CUBE_VERTICES = {
                {5, 4, 0, 1}, {2, 3, 7, 6}, {1, 0, 3, 2},
                {4, 5, 6, 7}, {5, 1, 2, 6}, {0, 4, 7, 3}
        };

        Vector3f[][][] cache = new Vector3f[2][NUM_CUBE_FACES][NUM_FACE_VERTICES];
        for (int face = 0; face < NUM_CUBE_FACES; face++) {
            for (int vert = 0; vert < NUM_FACE_VERTICES; vert++) {
                cache[0][face][vert] = CUBE_CORNERS[CUBE_VERTICES[face][vert]];
                cache[1][face][vert] = CUBE_CORNERS[CUBE_VERTICES[face][3 - vert]];
            }
        }
        return cache;
    }

    // 修复的法线更新方法
    private static void updateNormalCache(PoseStack.Pose matrices) {
        int hash = matrices.normal().hashCode();
        if (hash == LAST_MATRIX_HASH) return;

        LAST_MATRIX_HASH = hash;
        updateNormals(matrices.normal(), NORMAL_CACHE[0], false);
        updateNormals(matrices.normal(), NORMAL_CACHE[1], true);
    }

    private static void updateNormals(Matrix3f matrix, int[] cache, boolean mirrored) {
        cache[FACE_NEG_Y] = MatrixHelper.transformNormal(matrix, true, Direction.DOWN);
        cache[FACE_POS_Y] = MatrixHelper.transformNormal(matrix, true, Direction.UP);
        cache[FACE_NEG_Z] = MatrixHelper.transformNormal(matrix, true, Direction.NORTH);
        cache[FACE_POS_Z] = MatrixHelper.transformNormal(matrix, true, Direction.SOUTH);

        if (mirrored) {
            cache[FACE_NEG_X] = MatrixHelper.transformNormal(matrix, true, Direction.EAST);
            cache[FACE_POS_X] = MatrixHelper.transformNormal(matrix, true, Direction.WEST);
        } else {
            cache[FACE_NEG_X] = MatrixHelper.transformNormal(matrix, true, Direction.WEST);
            cache[FACE_POS_X] = MatrixHelper.transformNormal(matrix, true, Direction.EAST);
        }
    }

    // 保留原始顶点准备方法
    private static void prepareVertices(PoseStack.Pose matrices, ModelCuboid cuboid) {
        updateCubeCorner(CUBE_CORNERS[VERTEX_X1_Y1_Z1], cuboid.x1, cuboid.y1, cuboid.z1, matrices.pose());
        updateCubeCorner(CUBE_CORNERS[VERTEX_X2_Y1_Z1], cuboid.x2, cuboid.y1, cuboid.z1, matrices.pose());
        updateCubeCorner(CUBE_CORNERS[VERTEX_X2_Y2_Z1], cuboid.x2, cuboid.y2, cuboid.z1, matrices.pose());
        updateCubeCorner(CUBE_CORNERS[VERTEX_X1_Y2_Z1], cuboid.x1, cuboid.y2, cuboid.z1, matrices.pose());
        updateCubeCorner(CUBE_CORNERS[VERTEX_X1_Y1_Z2], cuboid.x1, cuboid.y1, cuboid.z2, matrices.pose());
        updateCubeCorner(CUBE_CORNERS[VERTEX_X2_Y1_Z2], cuboid.x2, cuboid.y1, cuboid.z2, matrices.pose());
        updateCubeCorner(CUBE_CORNERS[VERTEX_X2_Y2_Z2], cuboid.x2, cuboid.y2, cuboid.z2, matrices.pose());
        updateCubeCorner(CUBE_CORNERS[VERTEX_X1_Y2_Z2], cuboid.x1, cuboid.y2, cuboid.z2, matrices.pose());

        updateTextureCoordinates(cuboid);
    }

    // 修复的纹理坐标更新
    private static void updateTextureCoordinates(ModelCuboid cuboid) {
        buildVertexTexCoord(VERTEX_TEXTURES[FACE_NEG_Y], cuboid.u1, cuboid.v0, cuboid.u2, cuboid.v1);
        buildVertexTexCoord(VERTEX_TEXTURES[FACE_POS_Y], cuboid.u2, cuboid.v1, cuboid.u3, cuboid.v0);
        buildVertexTexCoord(VERTEX_TEXTURES[FACE_NEG_Z], cuboid.u1, cuboid.v1, cuboid.u2, cuboid.v2);
        buildVertexTexCoord(VERTEX_TEXTURES[FACE_POS_Z], cuboid.u4, cuboid.v1, cuboid.u5, cuboid.v2);
        buildVertexTexCoord(VERTEX_TEXTURES[FACE_NEG_X], cuboid.u2, cuboid.v1, cuboid.u4, cuboid.v2);
        buildVertexTexCoord(VERTEX_TEXTURES[FACE_POS_X], cuboid.u0, cuboid.v1, cuboid.u1, cuboid.v2);

        // 更新镜像版本
        for (int face = 0; face < NUM_CUBE_FACES; face++) {
            for (int vert = 0; vert < NUM_FACE_VERTICES; vert++) {
                VERTEX_TEXTURES_MIRRORED[face][vert].set(VERTEX_TEXTURES[face][3 - vert]);
            }
        }
    }

    // 保留辅助方法
    private static void updateCubeCorner(Vector3f vec, float x, float y, float z, Matrix4f matrix) {
        vec.set(
                MatrixHelper.transformPositionX(matrix, x, y, z),
                MatrixHelper.transformPositionY(matrix, x, y, z),
                MatrixHelper.transformPositionZ(matrix, x, y, z)
        );
    }

    private static void buildVertexTexCoord(Vector2f[] uvs, float u1, float v1, float u2, float v2) {
        uvs[0].set(u2, v1);
        uvs[1].set(u1, v1);
        uvs[2].set(u1, v2);
        uvs[3].set(u2, v2);
    }

    private static void initNormalCache() {
        System.arraycopy(NORMAL_CACHE[0], 0, NORMAL_CACHE[1], 0, NUM_CUBE_FACES);
        NORMAL_CACHE[1][FACE_NEG_X] = NORMAL_CACHE[0][FACE_POS_X];
        NORMAL_CACHE[1][FACE_POS_X] = NORMAL_CACHE[0][FACE_NEG_X];
    }

    private static void renderChildren(PoseStack poseStack, VertexBufferWriter writer, ModelPart[] children, int light, int overlay, int color) {
        for (ModelPart child : children) {
            render(poseStack, writer, child, light, overlay, color);
        }
    }
}