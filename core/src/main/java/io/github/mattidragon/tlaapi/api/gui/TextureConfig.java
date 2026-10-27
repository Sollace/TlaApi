package io.github.mattidragon.tlaapi.api.gui;

import net.minecraft.util.Identifier;

import java.util.Objects;

import com.google.common.base.Preconditions;

/**
 * Represents a texture configuration.
 * @see Builder
 */
public record TextureConfig (
        Identifier lightTexture,
        Identifier darkTexture,
        int width,
        int height,
        int u,
        int v,
        int regionWidth,
        int regionHeight,
        int textureWidth,
        int textureHeight) {
    public static final int DEFAULT_TEXTURE_SIZE = 256;
    /**
     * Creates a new texture config builder.
     *
     * @see Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for texture configs.
     * The builder begins with most values uninitialized, requiring you to set them,
     * but some are already set:
     * <ul>
     *     <li>Texture size is set tp 256x256</li>
     *     <li>UV is set to 0, 0</li>
     * </ul>
     */
    public static class Builder {
        private Identifier lightTexture;
        private Identifier darkTexture;
        private int width = -1;
        private int height = -1;
        private int u = 0;
        private int v = 0;
        private int regionWidth = -1;
        private int regionHeight = -1;
        private int textureWidth = DEFAULT_TEXTURE_SIZE;
        private int textureHeight = DEFAULT_TEXTURE_SIZE;

        private Builder() { }

        /**
         * Configures the texture to use the same texture for light and dark mode.
         */
        public Builder texture(Identifier texture) {
            Objects.requireNonNull(texture, "Texture cannot be null");
            return texture(texture, texture);
        }

        /**
         * Configures the texture to use different textures for light and dark mode.
         * Dark mode here refers to the dark theme of REI. You should match its color palette when applicable.
         */
        public Builder texture(Identifier lightTexture, Identifier darkTexture) {
            this.lightTexture = Objects.requireNonNull(lightTexture, "Light texture cannot be null");
            this.darkTexture = Objects.requireNonNull(darkTexture, "Dark texture cannot be null");
            return this;
        }

        /**
         * Sets the UV coordinates of the texture in the file.
         * The default is 0, 0.
         */
        public Builder uv(int u, int v) {
            Preconditions.checkArgument(u >= 0 && v >= 0, "UV coordinates must be non-negative");
            this.u = u;
            this.v = v;
            return this;
        }

        /**
         * Sets the size of the texture in the file.
         * If the region size is not set, this will also set the region size.
         */
        public Builder size(int width, int height) {
            Preconditions.checkArgument(width * height > 0, "Size must be positive");
            this.width = width;
            this.height = height;
            if (regionWidth == -1)
                this.regionWidth = width;
            if (regionHeight == -1)
                this.regionHeight = height;
            return this;
        }

        /**
         * Sets the size of the texture such that it takes up a whole file.
         */
        public Builder fullSize(int width, int height) {
            size(width, height);
            textureSize(width, height);
            return this;
        }

        /**
         * Sets the size of the texture in the file.
         */
        public Builder regionSize(int regionWidth, int regionHeight) {
            Preconditions.checkArgument(regionWidth * regionHeight > 0, "Region size must be positive");
            this.regionWidth = regionWidth;
            this.regionHeight = regionHeight;
            return this;
        }

        /**
         * Sets the size of the texture file.
         * The default is 256x256.
         */
        public Builder textureSize(int textureWidth, int textureHeight) {
            Preconditions.checkArgument(textureWidth * textureHeight > 0, "Texture size must be positive");
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            return this;
        }

        /**
         * Builds the texture config.
         *
         * @throws IllegalStateException If the size, region size, or texture is not set.
         */
        public TextureConfig build() {
            Preconditions.checkState(width >= 0 && height >= 0, "Size not set");
            Preconditions.checkState(regionWidth >= 0 && regionHeight >= 0, "Region size not set");
            Preconditions.checkState(lightTexture != null && darkTexture != null, "Texture not set");
            return new TextureConfig(lightTexture, darkTexture, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
        }
    }

    public <T> T apply(RemappingFunction<T> remappingFunction) {
        return remappingFunction.apply(lightTexture, darkTexture, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
    }

    public interface RemappingFunction<T> {
        T apply(
            Identifier lightTexture, Identifier darkTexture,
            int width, int height,
            int u, int v,
            int regionWidth, int regionHeight,
            int textureWidth, int textureHeight
        );
    }

    public interface UnitRemappingFunction<T> extends RemappingFunction<T> {
        T apply(
            Identifier lightTexture,
            int width, int height,
            int u, int v,
            int regionWidth, int regionHeight,
            int textureWidth, int textureHeight
        );

        @Override
        default T apply(
                Identifier lightTexture, Identifier darkTexture,
                int width, int height,
                int u, int v,
                int regionWidth, int regionHeight,
                int textureWidth, int textureHeight
            ) {
            return apply(lightTexture, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
        }
    }
}

