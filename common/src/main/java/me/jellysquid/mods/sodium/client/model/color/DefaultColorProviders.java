package me.jellysquid.mods.sodium.client.model.color;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.blender.BlendedColorProvider;
import me.jellysquid.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.Arrays;

public class DefaultColorProviders {
    public static ColorProvider<BlockState> adapt(BlockColor color) {
        return new VanillaAdapter(color);
    }

    private static ThreadLocal<BlockPos.MutableBlockPos> blockPosHolder = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    public static class GrassColorProvider<T> extends BlendedColorProvider<T> {
        public static final ColorProvider<BlockState> BLOCKS = new GrassColorProvider<>();

        private GrassColorProvider() {

        }

        @Override
        protected int getColor(LevelSlice slice, int x, int y, int z) {
            return BiomeColors.getAverageGrassColor(slice, blockPosHolder.get().set(x, y, z));
        }
    }

    public static class FoliageColorProvider<T> extends BlendedColorProvider<T> {
        public static final ColorProvider<BlockState> BLOCKS = new FoliageColorProvider<>();

        private FoliageColorProvider() {

        }

        @Override
        protected int getColor(LevelSlice slice, int x, int y, int z) {
            return BiomeColors.getAverageFoliageColor(slice, blockPosHolder.get().set(x, y, z));
        }
    }

    public static class WaterColorProvider<T> extends BlendedColorProvider<T> {
        public static final ColorProvider<BlockState> BLOCKS = new WaterColorProvider<>();
        public static final ColorProvider<FluidState> FLUIDS = new WaterColorProvider<>();

        private WaterColorProvider() {

        }

        @Override
        protected int getColor(LevelSlice slice, int x, int y, int z) {
            return BiomeColors.getAverageWaterColor(slice, blockPosHolder.get().set(x, y, z));
        }
    }

    private static class VanillaAdapter implements ColorProvider<BlockState> {
        private final BlockColor color;

        private VanillaAdapter(BlockColor color) {
            this.color = color;
        }

        @Override
        public void getColors(LevelSlice slice, BlockPos pos, BlockState state, ModelQuadView quad, int[] output) {
            Arrays.fill(output, this.color.getColor(state, slice, pos, quad.getColorIndex()));
        }
    }
}
