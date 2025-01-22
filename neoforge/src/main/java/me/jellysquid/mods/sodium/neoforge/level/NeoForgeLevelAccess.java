package me.jellysquid.mods.sodium.neoforge.level;

import com.mojang.blaze3d.vertex.PoseStack;
import me.jellysquid.mods.sodium.client.model.color.ColorProviderRegistry;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import me.jellysquid.mods.sodium.client.services.PlatformLevelAccess;
import me.jellysquid.mods.sodium.neoforge.render.FluidRendererImpl;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraftforge.client.ForgeHooksClient;
import org.joml.Matrix4f;

public class NeoForgeLevelAccess implements PlatformLevelAccess {
    @Override
    public FluidRenderer createPlatformFluidRenderer(ColorProviderRegistry colorRegistry, LightPipelineProvider lightPipelineProvider) {
        return new FluidRendererImpl(colorRegistry, lightPipelineProvider);
    }

    @Override
    public boolean tryRenderFluid() {
        return false;
    }

    @Override
    public void runChunkLayerEvents(RenderType renderType, LevelRenderer levelRenderer, PoseStack modelMatrix, Matrix4f projectionMatrix, int renderTick, Camera camera, Frustum frustum) {
        ForgeHooksClient.dispatchRenderStage(renderType, levelRenderer, modelMatrix, projectionMatrix, renderTick, camera, frustum);
    }
}
