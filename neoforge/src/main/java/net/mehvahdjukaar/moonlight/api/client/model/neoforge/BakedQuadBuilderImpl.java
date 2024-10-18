package net.mehvahdjukaar.moonlight.api.client.model.neoforge;

import net.mehvahdjukaar.moonlight.api.client.model.BakedQuadBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.function.Consumer;

public class BakedQuadBuilderImpl implements BakedQuadBuilder {

    public static BakedQuadBuilder create(TextureAtlasSprite sprite, @Nullable Matrix4f transformation, Consumer<BakedQuad> quadConsumer) {
        return new BakedQuadBuilderImpl(sprite, transformation, quadConsumer);
    }

    private final QuadBakingVertexConsumer inner;
    private final TextureAtlasSprite sprite;
    private final Consumer<BakedQuad> quadConsumer;
    private final Matrix4f globalTransform;
    private final Matrix3f normalTransf;

    private int emissivity = 0;
    private boolean autoDirection = false;

    private int vertexIndex = -1;


    private BakedQuadBuilderImpl(TextureAtlasSprite sprite, @Nullable Matrix4f transformation, Consumer<BakedQuad> quadConsumer) {
        this.inner = new QuadBakingVertexConsumer();
        this.globalTransform = transformation;// == null ? null : new Matrix4f(new Matrix3f(transformation)); //gets rid of translation
        this.sprite = sprite;
        this.quadConsumer = quadConsumer;
        this.inner.setShade(true);
        this.inner.setHasAmbientOcclusion(true);
        this.inner.setSprite(sprite);
        this.normalTransf = transformation == null ? null :
                new Matrix3f(transformation).invert().transpose();
    }

    @Override
    public void close() {
        tryBaking();
    }

    private void tryBaking() {
        if (vertexIndex == 3) {
            vertexIndex = -1;

            BakedQuad quad = inner.bakeQuad();
            if (emissivity != 0) {
                QuadTransformers.settingEmissivity(emissivity).processInPlace(quad);
            }
            quadConsumer.accept(quad);
        }
    }

    @Override
    public BakedQuadBuilderImpl addVertex(float x, float y, float z) {
        tryBaking();
        vertexIndex++;
        if (globalTransform != null) {
            inner.addVertex(new Matrix4f(globalTransform), x, y, z);
        } else {
            inner.addVertex(x, y, z);
        }
        return this;
    }

    @Override
    public BakedQuadBuilderImpl setColor(int red, int green, int blue, int alpha) {
        inner.setColor(red, green, blue, alpha);
        return this;
    }

    //given in sprite coords
    @Override
    public BakedQuadBuilderImpl setUv(float u, float v) {
        inner.setUv(sprite.getU(u * 16), sprite.getV(v * 16));
        return this;
    }

    @Override
    public BakedQuadBuilderImpl setUv1(int u, int v) {
        inner.setUv1(u, v);
        return this;
    }

    @Override
    public BakedQuadBuilderImpl setUv2(int u, int v) {
        inner.setUv2(u, v);
        return this;
    }

    @Override
    public BakedQuadBuilderImpl setNormal(float x, float y, float z) {
        if (globalTransform != null) {
            Vector3f normal = normalTransf.transform(new Vector3f(x, y, z));
            normal.normalize();
            inner.setNormal(normal.x, normal.y, normal.z);
        } else inner.setNormal(x, y, z);
        if (autoDirection) {
            this.setDirection(Direction.getNearest(x, y, z));
        }
        return this;
    }

    @Override
    public BakedQuadBuilder setDirection(Direction direction) {
        if (globalTransform != null) {
            direction = Direction.rotate(globalTransform, direction);
        }
        this.inner.setDirection(direction);
        return this;
    }

    @Override
    public BakedQuadBuilder setAmbientOcclusion(boolean ambientOcclusion) {
        this.inner.setHasAmbientOcclusion(ambientOcclusion);
        return this;
    }

    @Override
    public BakedQuadBuilder setTint(int tintIndex) {
        inner.setTintIndex(tintIndex);
        return this;
    }

    @Override
    public BakedQuadBuilder setShade(boolean shade) {
        this.inner.setShade(shade);
        return this;
    }

    @Override
    public BakedQuadBuilder lightEmission(int light) {
        this.emissivity = light;
        return this;
    }

    @Override
    public BakedQuadBuilder setAutoDirection() {
        this.autoDirection = true;
        return this;
    }
}