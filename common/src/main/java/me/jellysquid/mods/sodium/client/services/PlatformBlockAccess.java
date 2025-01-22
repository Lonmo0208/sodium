package me.jellysquid.mods.sodium.client.services;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public interface PlatformBlockAccess {
    PlatformBlockAccess INSTANCE = Services.load(PlatformBlockAccess.class);

    static PlatformBlockAccess getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the light emission of the current block.
     * @param state The current block
     * @param level The current level slice
     * @param pos The block position
     * @return The light emission of the current block (default 0)
     */
    int getLightEmission(BlockState state, BlockAndTintGetter level, BlockPos pos);

    /**
     * Checks if the block should stop drawing a specific side based on the adjacent block.
     * @param level The level slice.
     * @param selfState The block currently being drawn.
     * @param otherState The adjacent block.
     * @param selfPos The current block position.
     * @param otherPos The other block position.
     * @param facing The direction between the two blocks.
     * @return If the block's face should be skipped.
     */
    boolean shouldSkipRender(BlockGetter level, BlockState selfState, BlockState otherState, BlockPos selfPos, BlockPos otherPos, Direction facing);

    /**
     * Returns if the fluid should render fluid overlays if a block is adjacent to it.
     * @param block The block adjacent to the fluid being rendered
     * @param level The level slice
     * @param pos The position of the adjacent block
     * @param fluidState The fluid
     * @return True if the fluid should render an overlay.
     */
    boolean shouldShowFluidOverlay(BlockState block, BlockAndTintGetter level, BlockPos pos, FluidState fluidState);

    /**
     * Gets the specialized render data for this block entity.
     * @param blockEntity The block entity to get the render data of.
     * @return The specialized render data for this block entity. If the platform does not support it or there is no data, null.
     */
    @Nullable
    Object getBlockEntityData(BlockEntity blockEntity);

    /**
     * @return If the platform can return block entity data
     */
    boolean platformHasBlockData();

    /**
     * Gets the block shade.
     * @param quad The quad being rendered.
     * @param level The level.
     * @param shade If directional lighting should be added.
     * @return the block shade
     */
    float getAccurateBlockShade(ModelQuadView quad, BlockAndTintGetter level, boolean shade);

    /**
     * If the block contains forced ambient occlusion.
     * @param model The model being rendered
     * @param state The current block
     * @param data Any model data
     * @param renderType The current render type being drawn
     * @param level The level slice
     * @param pos The current position
     * @return If ambient occlusion is forced, or {@code DEFAULT}
     */
    TriState usesAmbientOcclusion(BakedModel model, BlockState state, Object data, RenderType renderType, BlockAndTintGetter level, BlockPos pos);
}
