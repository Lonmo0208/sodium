package me.jellysquid.mods.sodium.client.world.biome;

import me.jellysquid.mods.sodium.client.world.BiomeSeedProvider;
import me.jellysquid.mods.sodium.client.world.LevelSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public class LevelBiomeSlice {
    private static final int SIZE = 3 * 4;
    private final Holder<Biome>[] biomes = new Holder[SIZE * SIZE * SIZE];
    private final boolean[] uniform = new boolean[SIZE * SIZE * SIZE];
    private final BiasMap bias = new BiasMap();
    private long biomeZoomSeed;
    private int blockX, blockY, blockZ;

    public void update(ClientLevel level, ChunkRenderContext context) {
        this.blockX = context.getOrigin().minBlockX() - 16;
        this.blockY = context.getOrigin().minBlockY() - 16;
        this.blockZ = context.getOrigin().minBlockZ() - 16;
        this.biomeZoomSeed = BiomeSeedProvider.getBiomeZoomSeed(level);
        this.copyBiomeData(level, context);
        this.calculateBias();
        this.calculateUniform();
    }

    private void copyBiomeData(Level level, ChunkRenderContext context) {
        var defaultValue = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);

        for (int sectionX = 0; sectionX < 3; sectionX++) {
            for (int sectionY = 0; sectionY < 3; sectionY++) {
                for (int sectionZ = 0; sectionZ < 3; sectionZ++) {
                    this.copySectionBiomeData(context, sectionX, sectionY, sectionZ, defaultValue);
                }
            }
        }
    }

    private void copySectionBiomeData(ChunkRenderContext context, int sectionX, int sectionY, int sectionZ, Holder<Biome> defaultBiome) {
        var section = context.getSections()[LevelSlice.getLocalSectionIndex(sectionX, sectionY, sectionZ)];
        var biomeData = section.getBiomeData();

        for (int relCellX = 0; relCellX < 4; relCellX++) {
            for (int relCellY = 0; relCellY < 4; relCellY++) {
                for (int relCellZ = 0; relCellZ < 4; relCellZ++) {
                    int cellX = (sectionX * 4) + relCellX;
                    int cellY = (sectionY * 4) + relCellY;
                    int cellZ = (sectionZ * 4) + relCellZ;
                    int idx = dataArrayIndex(cellX, cellY, cellZ);
                    this.biomes[idx] = biomeData == null ? defaultBiome : biomeData.get(relCellX, relCellY, relCellZ);
                }
            }
        }
    }

    private void calculateUniform() {
        for (int cellX = 2; cellX < 10; cellX++) {
            for (int cellY = 2; cellY < 10; cellY++) {
                for (int cellZ = 2; cellZ < 10; cellZ++) {
                    this.uniform[dataArrayIndex(cellX, cellY, cellZ)] = this.hasUniformNeighbors(cellX, cellY, cellZ);
                }
            }
        }
    }

    private void calculateBias() {
        int originX = this.blockX >> 2;
        int originY = this.blockY >> 2;
        int originZ = this.blockZ >> 2;
        long seed = this.biomeZoomSeed;

        for (int relCellX = 1; relCellX < 11; relCellX++) {
            int cellX = originX + relCellX;
            long seedX = LinearCongruentialGenerator.next(seed, cellX);

            for (int relCellY = 1; relCellY < 11; relCellY++) {
                int cellY = originY + relCellY;
                long seedXY = LinearCongruentialGenerator.next(seedX, cellY);

                for (int relCellZ = 1; relCellZ < 11; relCellZ++) {
                    int cellZ = originZ + relCellZ;
                    long seedXYZ = LinearCongruentialGenerator.next(seedXY, cellZ);
                    this.calculateBias(dataArrayIndex(relCellX, relCellY, relCellZ), cellX, cellY, cellZ, seedXYZ);
                }
            }
        }
    }

    private void calculateBias(int cellIndex, int cellX, int cellY, int cellZ, long seed) {
        seed = LinearCongruentialGenerator.next(seed, cellX);
        seed = LinearCongruentialGenerator.next(seed, cellY);
        seed = LinearCongruentialGenerator.next(seed, cellZ);

        int gradX = getBias(seed); seed = LinearCongruentialGenerator.next(seed, this.biomeZoomSeed);
        int gradY = getBias(seed); seed = LinearCongruentialGenerator.next(seed, this.biomeZoomSeed);
        int gradZ = getBias(seed);

        this.bias.set(cellIndex, gradX, gradY, gradZ);
    }

    private boolean hasUniformNeighbors(int cellX, int cellY, int cellZ) {
        Biome biome = this.biomes[dataArrayIndex(cellX, cellY, cellZ)].value();

        for (int adjCellX = cellX - 1; adjCellX <= cellX + 1; adjCellX++) {
            for (int adjCellY = cellY - 1; adjCellY <= cellY + 1; adjCellY++) {
                for (int adjCellZ = cellZ - 1; adjCellZ <= cellZ + 1; adjCellZ++) {
                    if (this.biomes[dataArrayIndex(adjCellX, adjCellY, adjCellZ)].value() != biome) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public Holder<Biome> getBiome(int blockX, int blockY, int blockZ) {
        int relBlockX = blockX - this.blockX;
        int relBlockY = blockY - this.blockY;
        int relBlockZ = blockZ - this.blockZ;

        int centerIndex = dataArrayIndex(QuartPos.fromBlock(relBlockX - 2), QuartPos.fromBlock(relBlockY - 2), QuartPos.fromBlock(relBlockZ - 2));

        return this.uniform[centerIndex] ? this.biomes[centerIndex] : this.getBiomeUsingVoronoi(relBlockX, relBlockY, relBlockZ);
    }

    private Holder<Biome> getBiomeUsingVoronoi(int blockX, int blockY, int blockZ) {
        int x = blockX - 2;
        int y = blockY - 2;
        int z = blockZ - 2;

        int originIntX = QuartPos.fromBlock(x);
        int originIntY = QuartPos.fromBlock(y);
        int originIntZ = QuartPos.fromBlock(z);

        float originFracX = QuartPos.quartLocal(x) * 0.25f;
        float originFracY = QuartPos.quartLocal(y) * 0.25f;
        float originFracZ = QuartPos.quartLocal(z) * 0.25f;

        float closestDistance = Float.POSITIVE_INFINITY;
        int closestArrayIndex = 0;

        for (int index = 0; index < 8; index++) {
            boolean dirX = (index & 4) != 0;
            boolean dirY = (index & 2) != 0;
            boolean dirZ = (index & 1) != 0;

            int cellIntX = originIntX + (dirX ? 1 : 0);
            int cellIntY = originIntY + (dirY ? 1 : 0);
            int cellIntZ = originIntZ + (dirZ ? 1 : 0);

            float cellFracX = originFracX - (dirX ? 1.0f : 0.0f);
            float cellFracY = originFracY - (dirY ? 1.0f : 0.0f);
            float cellFracZ = originFracZ - (dirZ ? 1.0f : 0.0f);

            int biasIndex = dataArrayIndex(cellIntX, cellIntY, cellIntZ);

            float biasX = biasToVector(this.bias.getX(biasIndex));
            float biasY = biasToVector(this.bias.getY(biasIndex));
            float biasZ = biasToVector(this.bias.getZ(biasIndex));

            float distance = Mth.square(cellFracX + biasX) + Mth.square(cellFracY + biasY) + Mth.square(cellFracZ + biasZ);

            if (closestDistance > distance) {
                closestArrayIndex = biasIndex;
                closestDistance = distance;
            }
        }

        return this.biomes[closestArrayIndex];
    }

    private static int dataArrayIndex(int cellX, int cellY, int cellZ) {
        return (cellX * SIZE * SIZE) + (cellY * SIZE) + cellZ;
    }

    private static float biasToVector(int bias) {
        return (bias * (1.0f / 1024.0f)) * 0.9f;
    }

    private static int getBias(long l) {
        return (int) (((l >> 24) & 1023) - 512);
    }

    public static class BiasMap {
        private final short[] data = new short[SIZE * SIZE * SIZE * 3];

        public void set(int index, int x, int y, int z) {
            this.data[(index * 3) + 0] = (short) x;
            this.data[(index * 3) + 1] = (short) y;
            this.data[(index * 3) + 2] = (short) z;
        }

        public int getX(int index) {
            return this.data[(index * 3) + 0];
        }

        public int getY(int index) {
            return this.data[(index * 3) + 1];
        }

        public int getZ(int index) {
            return this.data[(index * 3) + 2];
        }
    }
}