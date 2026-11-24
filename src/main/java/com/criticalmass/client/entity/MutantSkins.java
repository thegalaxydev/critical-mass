package com.criticalmass.client.entity;

import com.criticalmass.CriticalMass;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class MutantSkins {
    private static final int[] HIDE = {0x263D27, 0x35522C, 0x496B33, 0x60863D, 0x7B9F4B, 0x97B55E, 0xB3C87A, 0xCCD89B};
    private static final int[] DEAD = {0x2C2D22, 0x40402F, 0x57553D, 0x6F6C4E, 0x888562, 0xA09D78};
    private static final int[] FLESH = {0x240E12, 0x45191E, 0x69292B, 0x8C3F39, 0xAE5F4E};
    private static final int[] PUS = {0x6A8A1C, 0x9CC02A, 0xCBE448, 0xEEFA9A};
    private static final float TINT = 0.5F;
    private static final float MOTTLE = 0.1F;
    private static final Map<ResourceLocation, Skin> MADE = new HashMap<>();

    public record Skin(ResourceLocation colour, @Nullable ResourceLocation glow) {
    }

    private MutantSkins() {
    }

    public static Skin of(ResourceLocation original) {
        Skin made = MADE.get(original);
        if (made == null) {
            made = make(original);
            MADE.put(original, made);
        }
        return made;
    }

    public static void clear() {
        TextureManager textures = Minecraft.getInstance().getTextureManager();
        for (Skin skin : MADE.values()) {
            if (skin.glow() != null) {
                textures.release(skin.colour());
                textures.release(skin.glow());
            }
        }
        MADE.clear();
    }

    private static Skin make(ResourceLocation original) {
        Minecraft minecraft = Minecraft.getInstance();
        Optional<Resource> resource = minecraft.getResourceManager().getResource(original);
        if (resource.isEmpty()) {
            return new Skin(original, null);
        }
        try (InputStream in = resource.get().open(); NativeImage source = NativeImage.read(in)) {
            NativeImage colour = new NativeImage(source.getWidth(), source.getHeight(), true);
            NativeImage glow = new NativeImage(source.getWidth(), source.getHeight(), true);
            new Painter(source, colour, glow, original.toString().hashCode()).paint();
            String path = original.getNamespace() + "/" + original.getPath();
            Skin skin = new Skin(CriticalMass.asResource("irradiated/" + path), CriticalMass.asResource("irradiated_glow/" + path));
            minecraft.getTextureManager().register(skin.colour(), new DynamicTexture(colour));
            minecraft.getTextureManager().register(skin.glow(), new DynamicTexture(glow));
            return skin;
        } catch (IOException | RuntimeException failed) {
            CriticalMass.LOGGER.warn("Could not make an irradiated skin from {}", original, failed);
            return new Skin(original, null);
        }
    }

    private static final class Painter {
        private final NativeImage source;
        private final NativeImage colour;
        private final NativeImage glow;
        private final Random random;
        private final int width;
        private final int height;
        private final int unit;

        Painter(NativeImage source, NativeImage colour, NativeImage glow, long seed) {
            this.source = source;
            this.colour = colour;
            this.glow = glow;
            this.random = new Random(seed);
            this.width = source.getWidth();
            this.height = source.getHeight();
            this.unit = Math.max(1, width / 64);
        }

        private boolean solid(int x, int y) {
            return x >= 0 && y >= 0 && x < width && y < height && (source.getPixelRGBA(x, y) >>> 24) >= 200;
        }

        private static int rgb(int abgr) {
            return (abgr & 0xFF) << 16 | abgr & 0xFF00 | abgr >> 16 & 0xFF;
        }

        private static int abgr(int alpha, int rgb) {
            return alpha << 24 | (rgb & 0xFF) << 16 | rgb & 0xFF00 | rgb >> 16 & 0xFF;
        }

        private static float light(int rgb) {
            return ((rgb >> 16 & 0xFF) * 0.299F + (rgb >> 8 & 0xFF) * 0.587F + (rgb & 0xFF) * 0.114F) / 255F;
        }

        private static int mix(int from, int to, float share) {
            int r = (int) Mth.lerp(share, from >> 16 & 0xFF, to >> 16 & 0xFF);
            int g = (int) Mth.lerp(share, from >> 8 & 0xFF, to >> 8 & 0xFF);
            int b = (int) Mth.lerp(share, from & 0xFF, to & 0xFF);
            return r << 16 | g << 8 | b;
        }

        private static int ramp(int[] tones, float t) {
            float at = Mth.clamp(t, 0F, 1F) * (tones.length - 1);
            int low = (int) at;
            return low >= tones.length - 1 ? tones[tones.length - 1] : mix(tones[low], tones[low + 1], at - low);
        }

        private void set(int x, int y, int rgb, float lit) {
            if (!solid(x, y)) {
                return;
            }
            colour.setPixelRGBA(x, y, abgr(colour.getPixelRGBA(x, y) >>> 24, rgb));
            glow.setPixelRGBA(x, y, lit > 0 ? abgr(255, mix(0, rgb, lit)) : 0);
        }

        private int skin(int x, int y) {
            return rgb(colour.getPixelRGBA(x, y));
        }

        private int[] spot() {
            for (int tries = 0; tries < 60; tries++) {
                int x = random.nextInt(width);
                int y = random.nextInt(height);
                if (solid(x, y)) {
                    return new int[] {x, y};
                }
            }
            return null;
        }

        void paint() {
            int area = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = source.getPixelRGBA(x, y);
                    int alpha = pixel >>> 24;
                    if (alpha == 0) {
                        colour.setPixelRGBA(x, y, pixel);
                        continue;
                    }
                    int rgb = rgb(pixel);
                    colour.setPixelRGBA(x, y, abgr(alpha, mix(rgb, ramp(HIDE, light(rgb)), TINT)));
                    if (alpha >= 200) {
                        area++;
                    }
                }
            }
            mottle();
            int cell = unit * unit;
            for (int i = Math.max(1, area / (420 * cell)); i > 0; i--) {
                patch(false);
            }
            for (int i = Math.max(1, area / (300 * cell)); i > 0; i--) {
                patch(true);
            }
            for (int i = Math.max(3, area / (110 * cell)); i > 0; i--) {
                vein();
            }
            for (int i = Math.max(2, area / (260 * cell)); i > 0; i--) {
                boil();
            }
        }

        private void mottle() {
            int[] above = new int[width];
            int[] row = new int[width];
            for (int y = 0; y < height; y += unit) {
                for (int x = 0; x < width;) {
                    int step = random.nextFloat() < 0.55F ? above[x] : random.nextInt(4) - 1 >> 1 == 0 ? 0 : random.nextBoolean() ? 1 : -1;
                    for (int run = (1 + random.nextInt(4)) * unit; run > 0 && x < width; run--) {
                        row[x++] = step;
                    }
                }
                for (int x = 0; x < width; x++) {
                    if (row[x] == 0) {
                        continue;
                    }
                    for (int dy = 0; dy < unit && y + dy < height; dy++) {
                        int pixel = colour.getPixelRGBA(x, y + dy);
                        if (pixel >>> 24 != 0) {
                            colour.setPixelRGBA(x, y + dy, abgr(pixel >>> 24, mix(rgb(pixel), row[x] > 0 ? 0xE6EBC8 : 0x101A0C, MOTTLE)));
                        }
                    }
                }
                System.arraycopy(row, 0, above, 0, width);
            }
        }

        private void patch(boolean wound) {
            int[] at = spot();
            if (at == null) {
                return;
            }
            float radius = unit * (wound ? 1.3F + random.nextFloat() : 2.2F + random.nextFloat() * 1.6F);
            int reach = Mth.ceil(radius) + unit;
            for (int y = at[1] - reach; y <= at[1] + reach; y++) {
                for (int x = at[0] - reach; x <= at[0] + reach; x++) {
                    if (!solid(x, y)) {
                        continue;
                    }
                    float distance = Mth.sqrt((x - at[0]) * (x - at[0]) + (y - at[1]) * (y - at[1])) + (random.nextFloat() - 0.5F) * unit * 0.8F;
                    if (distance <= radius) {
                        set(x, y, wound ? ramp(FLESH, 0.15F + 0.75F * distance / radius) : mix(skin(x, y), ramp(DEAD, light(skin(x, y)) * 1.2F), 0.8F), 0);
                    } else if (wound && distance <= radius + unit) {
                        set(x, y, mix(skin(x, y), 0x16200F, 0.5F), 0);
                    }
                }
            }
        }

        private void vein() {
            int[] at = spot();
            if (at == null) {
                return;
            }
            float x = at[0] + 0.5F;
            float y = at[1] + 0.5F;
            float heading = random.nextFloat() * Mth.TWO_PI;
            for (int step = (6 + random.nextInt(9)) * unit; step > 0; step--) {
                if (!solid((int) x, (int) y)) {
                    return;
                }
                for (int dy = 0; dy < Math.max(1, unit / 2); dy++) {
                    for (int dx = 0; dx < Math.max(1, unit / 2); dx++) {
                        set((int) x + dx, (int) y + dy, PUS[1], 0.7F);
                    }
                }
                heading += (random.nextFloat() - 0.5F) * 1.1F;
                x += Mth.cos(heading);
                y += Mth.sin(heading);
            }
        }

        private void boil() {
            int[] at = spot();
            if (at == null) {
                return;
            }
            for (int dy = 0; dy < unit * 2; dy++) {
                for (int dx = 0; dx < unit * 2; dx++) {
                    int tone = 3 - (dx >= unit ? 1 : 0) - (dy >= unit ? 1 : 0);
                    set(at[0] + dx, at[1] + dy, PUS[tone], 0.6F + 0.13F * tone);
                }
            }
            for (int dx = 0; dx < unit * 2; dx++) {
                for (int dy = 0; dy < unit; dy++) {
                    int x = at[0] + dx;
                    int y = at[1] + unit * 2 + dy;
                    if (solid(x, y)) {
                        set(x, y, mix(skin(x, y), 0x16200F, 0.45F), 0);
                    }
                }
            }
        }
    }
}
