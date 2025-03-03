package me.jellysquid.mods.sodium.client.render.frapi.render;

import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache;
import me.jellysquid.mods.sodium.client.render.frapi.SodiumRenderer;
import me.jellysquid.mods.sodium.client.render.frapi.helper.ColorHelper;
import me.jellysquid.mods.sodium.client.render.frapi.mesh.EncodingFormat;
import me.jellysquid.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import me.jellysquid.mods.sodium.client.services.PlatformBlockAccess;
import me.jellysquid.mods.sodium.client.services.PlatformModelAccess;
import me.jellysquid.mods.sodium.client.services.SodiumModelData;
import me.jellysquid.mods.sodium.client.world.LevelSlice;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Base class for the functions that can be shared between the terrain and non-terrain pipelines.
 *
 * <p>Make sure to set the {@link #lighters} in the subclass constructor.
 */
public abstract class AbstractBlockRenderContext extends AbstractRenderContext {
    private static final RenderMaterial[] STANDARD_MATERIALS;
    private static final RenderMaterial TRANSLUCENT_MATERIAL = SodiumRenderer.INSTANCE.materialFinder().blendMode(BlendMode.TRANSLUCENT).find();

    static {
        STANDARD_MATERIALS = new RenderMaterial[TriState.values().length];

        TriState[] values = TriState.values();
        for (int i = 0; i < values.length; i++) {
            TriState state = values[i];
            STANDARD_MATERIALS[i] = SodiumRenderer.INSTANCE.materialFinder().ambientOcclusion(state).find();
        }
    }
    private final MutableQuadViewImpl editorQuad = new MutableQuadViewImpl() {
        {
            data = new int[EncodingFormat.TOTAL_STRIDE];
            clear();
        }

        @Override
        public void emitDirectly() {
            if (type == null) {
                throw new IllegalStateException("No render type is set but an FRAPI object was asked to render!");
            }
            renderQuad(this);
        }
    };

    @Deprecated
    private final BakedModelConsumerImpl vanillaModelConsumer = new BakedModelConsumerImpl();

    /**
     * The world which the block is being rendered in.
     */
    protected BlockAndTintGetter level;
    /**
     * The level slice used for rendering
     */
    protected LevelSlice slice;
    /**
     * The state of the block being rendered.
     */
    protected BlockState state;
    /**
     * The position (in world space) of the block being rendered.
     */
    protected BlockPos pos;

    /**
     * The current render type being rendered.
     */
    protected RenderType type;

    /**
     * The current model's model data.
     */
    protected SodiumModelData modelData;

    private final BlockOcclusionCache occlusionCache = new BlockOcclusionCache();
    private boolean enableCulling = true;
    // Cull cache (as it's checked per-quad instead of once per side like in vanilla)
    private int cullCompletionFlags;
    private int cullResultFlags;

    protected RandomSource random;
    protected long randomSeed;
    protected final Supplier<RandomSource> randomSupplier = () -> {
        random.setSeed(randomSeed);
        return random;
    };

    /**
     * Must be set by the subclass constructor.
     */
    protected LightPipelineProvider lighters;
    protected final QuadLightData quadLightData = new QuadLightData();
    protected boolean useAmbientOcclusion;
    // Default AO mode for model (can be overridden by material property)
    protected LightMode defaultLightMode;

    @Override
    public QuadEmitter getEmitter() {
        this.editorQuad.clear();
        return this.editorQuad;
    }

    @Override
    public boolean isFaceCulled(@Nullable Direction face) {
        if (face == null || !this.enableCulling) {
            return false;
        }

        final int mask = 1 << face.get3DDataValue();

        if ((this.cullCompletionFlags & mask) == 0) {
            this.cullCompletionFlags |= mask;

            if (this.occlusionCache.shouldDrawSide(this.state, this.level, this.pos, face)) {
                this.cullResultFlags |= mask;
                return false;
            } else {
                return true;
            }
        } else {
            return (this.cullResultFlags & mask) == 0;
        }
    }

    @Override
    public ItemDisplayContext itemTransformationMode() {
        throw new UnsupportedOperationException("itemTransformationMode can only be called on an item render context.");
    }

    @Deprecated
    @Override
    public BakedModelConsumer bakedModelConsumer() {
        return this.vanillaModelConsumer;
    }

    /**
     * Pipeline entrypoint - handles transform and culling checks.
     */
    private void renderQuad(MutableQuadViewImpl quad) {
        if (!this.transform(quad)) {
            return;
        }

        if (this.isFaceCulled(quad.cullFace())) {
            return;
        }

        this.processQuad(quad);
    }

    /**
     * Quad pipeline function - after transform and culling checks.
     * Can also be used as entrypoint to skip some logic if the transform and culling checks have already been performed.
     */
    protected abstract void processQuad(MutableQuadViewImpl quad);

    protected void prepareCulling(boolean enableCulling) {
        this.enableCulling = enableCulling;
        this.cullCompletionFlags = 0;
        this.cullResultFlags = 0;
    }

    protected void prepareAoInfo(boolean modelAo) {
        this.useAmbientOcclusion = Minecraft.useAmbientOcclusion();
        // Ignore the incorrect IDEA warning here.
        this.defaultLightMode = this.useAmbientOcclusion && modelAo && PlatformBlockAccess.getInstance().getLightEmission(state, level, pos) == 0 ? LightMode.SMOOTH : LightMode.FLAT;
    }

    // TODO: normal-based (enhanced) AO for smooth lighting pipeline
    // TODO: normal-based diffuse for both lighting pipelines
    protected void shadeQuad(MutableQuadViewImpl quad, LightMode lightMode, boolean emissive) {
        LightPipeline lighter = this.lighters.getLighter(lightMode);
        QuadLightData data = this.quadLightData;
        lighter.calculate(quad, this.pos, data, quad.cullFace(), quad.lightFace(), quad.hasShade(), false);

        if (emissive) {
            for (int i = 0; i < 4; i++) {
                quad.lightmap(i, LightTexture.FULL_BRIGHT);
            }
        } else {
            int[] lightmaps = data.lm;

            for (int i = 0; i < 4; i++) {
                quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), lightmaps[i]));
            }
        }
    }

    /* Handling of vanilla models - this is the hot path for non-modded models */
    public void bufferDefaultModel(BakedModel model, @Nullable BlockState state) {
        MutableQuadViewImpl editorQuad = this.editorQuad;


        // If there is no transform, we can check the culling face once for all the quads,
        // and we don't need to check for transforms per-quad.
        boolean noTransform = !this.hasTransform();

        for (int i = 0; i <= ModelHelper.NULL_FACE_ID; i++) {
            final Direction cullFace = ModelHelper.faceFromIndex(i);

            RandomSource random = this.randomSupplier.get();
            TriState ao = PlatformBlockAccess.getInstance().usesAmbientOcclusion(model, state, modelData, type, slice, pos);
            if (noTransform) {
                if (!this.isFaceCulled(cullFace)) {
                    final List<BakedQuad> quads = PlatformModelAccess.getInstance().getQuads(level, pos, model, state, cullFace, random, type, modelData);
                    final int count = quads.size();

                    for (int j = 0; j < count; j++) {
                        final BakedQuad q = quads.get(j);
                        editorQuad.fromVanilla(q, (type == RenderType.tripwire() || type == RenderType.translucent()) ? TRANSLUCENT_MATERIAL : STANDARD_MATERIALS[ao.ordinal()], cullFace);
                        // Call processQuad instead of emit for efficiency
                        // (avoid unnecessarily clearing data, trying to apply transforms, and performing cull check again)

                        this.processQuad(editorQuad);
                    }
                }
            } else {
                final List<BakedQuad> quads = PlatformModelAccess.getInstance().getQuads(level, pos, model, state, cullFace, random, type, modelData);
                final int count = quads.size();

                for (int j = 0; j < count; j++) {
                    final BakedQuad q = quads.get(j);
                    editorQuad.fromVanilla(q, (type == RenderType.tripwire() || type == RenderType.translucent()) ? TRANSLUCENT_MATERIAL : STANDARD_MATERIALS[ao.ordinal()], cullFace);
                    // Call renderQuad instead of emit for efficiency
                    // (avoid unnecessarily clearing data)
                    this.renderQuad(editorQuad);
                }
            }
        }

        editorQuad.clear();
    }

    @Deprecated
    private class BakedModelConsumerImpl implements BakedModelConsumer {
        @Override
        public void accept(BakedModel model) {
            accept(model, AbstractBlockRenderContext.this.state);
        }

        @Override
        public void accept(BakedModel model, @Nullable BlockState state) {
            AbstractBlockRenderContext.this.bufferDefaultModel(model, state);
        }
    }
}
