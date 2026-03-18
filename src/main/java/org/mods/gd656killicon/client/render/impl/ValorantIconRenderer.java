package org.mods.gd656killicon.client.render.impl;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.config.ElementTextureDefinition;
import org.mods.gd656killicon.client.config.ValorantStyleCatalog;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.textures.IconTextureAnimationManager.TextureFrame;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;
import org.mods.gd656killicon.client.textures.ModTextures;
import org.mods.gd656killicon.common.KillType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ValorantIconRenderer implements IHudRenderer {
    private static final long DEFAULT_DISPLAY_DURATION = 2600L;
    private static final long FIVE_PLUS_FADE_IN_DURATION_MS = 300L;
    private static final long PARTICLE_BASE_DURATION_MS = 620L;
    private static final long BAR_BOUNCE_START_MS = 20L;
    private static final long BAR_BOUNCE_PEAK_MS = 180L;
    private static final long BAR_BOUNCE_HOLD_END_MS = 270L;
    private static final long BAR_BOUNCE_END_MS = 620L;
    private static final long BAR_SPIN_START_MS = 750L;
    private static final long BAR_SPIN_DURATION_180_MS = 700L;
    private static final long BAR_SPIN_DURATION_360_MS = 1000L;
    private static final long FADE_OUT_DURATION_MS = 220L;

    private static final float DEFAULT_ICON_ENTRY_OFFSET_PX = -16.0f;
    private static final float DEFAULT_ICON_ENTRY_DURATION_SECONDS = 0.10f;
    private static final float BAR_BOUNCE_DISTANCE_PX = 9.0f;
    private static final float VISUAL_CENTER_Y_OFFSET_PX = 6.0f;
    private static final float GLOBAL_VISUAL_SCALE = 0.95f;
    private static final float ICON_BASE_SIZE = 116.0f;
    private static final float BAR_BASE_SIZE = 32.0f;
    private static final float DEFAULT_PARTICLE_SCALE = 1.35f;
    private static final float DEFAULT_PARTICLE_SPEED = 1.0f;
    private static final float PARTICLE_BASE_SIZE = 112.0f;
    private static final float PARTICLE_FRAME_ANCHOR_X_RATIO = 0.0f;
    private static final float PARTICLE_FRAME_ANCHOR_Y_RATIO = 206.0f / 256.0f;
    private static final float BAR_RING_RADIUS = 36.0f;
    private static final float GAIA_DEFAULT_BRIGHTNESS = 1.3f;
    private static final float GAIA_DEFAULT_CONTRAST = 1.1f;
    private static final int DEFAULT_ICON_FLASH_COLOR = 0xFF2A36;
    private static final int MAX_ICON_FLASH_COUNT = 12;
    private static final int PRIME_PARTICLE_SMALL_COLOR = 0xFFD138;
    private static final int GAIA_PARTICLE_SMALL_COLOR = 0xE2505C;
    private static final int DEFAULT_ACCENT_COLOR = 0xE2505C;
    private static final float DEFAULT_ICON_FLASH_HOLD_DURATION_SECONDS = 0.08f;
    private static final float ICON_FLASH_MAX_ALPHA = 0.9f;
    private static final float ICON_TEXTURE_SOURCE_SIZE = 1729.0f;
    private static final float DEFAULT_HERO_FLAME_SOURCE_SCALE = 8.0f;
    private static final float DEFAULT_LARGE_SPARKS_SOURCE_SCALE = 6.8f;
    private static final float DEFAULT_X_SPARKS_SOURCE_SCALE = 11.0f;
    private static final float HERO_FLAME_Y_OFFSET_PX = -30.0f;
    private static final float LARGE_SPARKS_Y_OFFSET_PX = 2.0f;
    private static final float X_SPARKS_Y_OFFSET_PX = -8.0f;
    private static final ParticleSheetSpec[] BASE_PARTICLE_SHEETS = new ParticleSheetSpec[]{
        new ParticleSheetSpec("killicon_valorant_particle_base_t1.png", 256, 256, 49, 40.0f, 0.0f, PARTICLE_FRAME_ANCHOR_Y_RATIO),
        new ParticleSheetSpec("killicon_valorant_particle_base_t2.png", 256, 256, 49, 40.0f, 0.0f, PARTICLE_FRAME_ANCHOR_Y_RATIO),
        new ParticleSheetSpec("killicon_valorant_particle_base_t3.png", 256, 256, 42, 40.0f, 0.0f, PARTICLE_FRAME_ANCHOR_Y_RATIO)
    };
    private static final ParticleOverlaySpec HERO_FLAME_PARTICLE = new ParticleOverlaySpec(
        "hero_flame",
        "killicon_valorant_particle_hero_flame.png",
        200,
        224,
        20,
        35.0f,
        0.4934375f,
        0.49079242f,
        DEFAULT_HERO_FLAME_SOURCE_SCALE,
        0.0f,
        HERO_FLAME_Y_OFFSET_PX,
        false
    );
    private static final ParticleOverlaySpec LARGE_SPARKS_PARTICLE = new ParticleOverlaySpec(
        "large_sparks",
        "killicon_valorant_particle_large_sparks.png",
        256,
        256,
        52,
        40.0f,
        0.49052733f,
        0.5185547f,
        DEFAULT_LARGE_SPARKS_SOURCE_SCALE,
        0.0f,
        LARGE_SPARKS_Y_OFFSET_PX,
        true
    );
    private static final ParticleOverlaySpec X_SPARKS_PARTICLE = new ParticleOverlaySpec(
        "x_sparks",
        "killicon_valorant_particle_x_sparks.png",
        84,
        256,
        29,
        40.0f,
        0.48015872f,
        0.56722003f,
        DEFAULT_X_SPARKS_SOURCE_SCALE,
        0.0f,
        X_SPARKS_Y_OFFSET_PX,
        false
    );
    private static final float[][] BAR_LAYOUT_ANGLES = new float[][]{
        {0.0f},
        {-90.0f, 90.0f},
        {0.0f, -120.0f, 120.0f},
        {0.0f, -90.0f, 90.0f, 180.0f},
        {0.0f, -72.0f, 72.0f, -144.0f, 144.0f},
        {0.0f, -60.0f, 60.0f, -120.0f, 120.0f, 180.0f}
    };
    private static final float[][] GLOW_OFFSETS = new float[][]{
        {-1.0f, 0.0f},
        {1.0f, 0.0f},
        {0.0f, -1.0f},
        {0.0f, 1.0f},
        {-0.70710677f, -0.70710677f},
        {-0.70710677f, 0.70710677f},
        {0.70710677f, -0.70710677f},
        {0.70710677f, 0.70710677f}
    };
    private static final Map<String, ResourceLocation> PROCESSED_TEXTURE_CACHE = new HashMap<>();
    private static final Map<String, TextureMetrics> TEXTURE_ASPECT_CACHE = new HashMap<>();

    private float configScale = 1.0f;
    private float configXOffset = 0.0f;
    private float configYOffset = 80.0f;
    private float configBarXOffset = 0.0f;
    private float configBarYOffset = 0.0f;
    private float configBarRadiusOffset = 0.0f;
    private float configIconScale = 1.0f;
    private float configIconXOffset = 0.0f;
    private float configIconYOffset = 0.0f;
    private float configFrameScale = 1.0f;
    private boolean configRingEnabled = true;
    private float configRingScale = 1.0f;
    private float configBladeScale = 1.0f;
    private float configBarScale = 1.0f;
    private float configHeadshotScale = 1.0f;
    private float configFrameXOffset = 0.0f;
    private float configFrameYOffset = 0.0f;
    private float configRingXOffset = 0.0f;
    private float configRingYOffset = 0.0f;
    private float configBladeXOffset = 0.0f;
    private float configBladeYOffset = 0.0f;
    private float configHeadshotXOffset = 0.0f;
    private float configHeadshotYOffset = 0.0f;
    private float configHeadshotAnimInitialScale = 1.8f;
    private float configHeadshotAnimDuration = 0.25f;
    private float configHeadshotAnimFlickerSpeed = 18.0f;
    private float configHeadshotAnimScaleCurve = 0.6f;
    private int configHeadshotAnimFlickerColor = 0xFFFFFF;
    private float configBarEntryInitialScale = 1.6f;
    private float configBarEntryDuration = 0.18f;
    private float configBarEntryScaleCurve = 0.7f;
    private float configBrightness = 1.0f;
    private float configContrast = 1.0f;
    private boolean configIconGlowEnabled = false;
    private int configIconGlowColor = 0xFFFFFF;
    private float configIconGlowIntensity = 0.45f;
    private float configIconGlowSize = 4.0f;
    private boolean configIconAntialiasing = false;
    private float configIconEntryCurve = 1.0f;
    private long displayDuration = DEFAULT_DISPLAY_DURATION;
    private JsonObject currentConfig;

    private long startTime = -1L;
    private boolean visible = false;
    private int comboCount = 1;
    private int selectedBaseParticleIndex = 0;
    private boolean headshotTrigger = false;
    private float spinDirection = 1.0f;

    private record ParticleSheetSpec(
        String texturePath,
        int frameWidth,
        int frameHeight,
        int totalFrames,
        float framesPerSecond,
        float anchorXRatio,
        float anchorYRatio
    ) {
    }

    private record ParticleOverlaySpec(
        String configKeyPrefix,
        String texturePath,
        int frameWidth,
        int frameHeight,
        int totalFrames,
        float framesPerSecond,
        float anchorXRatio,
        float anchorYRatio,
        float defaultSourceScale,
        float offsetXPx,
        float offsetYPx,
        boolean additive
    ) {
    }

    public static void clearProcessedTextureCache() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            PROCESSED_TEXTURE_CACHE.clear();
            TEXTURE_ASPECT_CACHE.clear();
            return;
        }

        for (ResourceLocation texture : PROCESSED_TEXTURE_CACHE.values()) {
            minecraft.getTextureManager().release(texture);
        }
        PROCESSED_TEXTURE_CACHE.clear();
        TEXTURE_ASPECT_CACHE.clear();
    }

    @Override
    public void trigger(TriggerContext context) {
        if (context.comboCount() <= 0) {
            return;
        }

        JsonObject config = ConfigManager.getElementConfig("kill_icon", "valorant");
        if (config == null) {
            return;
        }

        boolean enabled = !config.has("visible") || config.get("visible").getAsBoolean();
        if (!enabled) {
            visible = false;
            return;
        }

        loadConfig(config);
        comboCount = Mth.clamp(context.comboCount(), 1, 6);
        selectedBaseParticleIndex = ThreadLocalRandom.current().nextInt(BASE_PARTICLE_SHEETS.length);
        headshotTrigger = context.type() == KillType.HEADSHOT;
        spinDirection = ThreadLocalRandom.current().nextBoolean() ? 1.0f : -1.0f;
        startTime = System.currentTimeMillis();
        visible = true;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float partialTick) {
        if (!visible || startTime < 0L) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        float centerX = screenWidth / 2.0f + configXOffset;
        float centerY = screenHeight - configYOffset + VISUAL_CENTER_Y_OFFSET_PX;
        renderAt(guiGraphics, partialTick, centerX, centerY);
    }

    public void renderAt(GuiGraphics guiGraphics, float partialTick, float centerX, float centerY) {
        if (!visible || startTime < 0L) {
            return;
        }

        JsonObject liveConfig = ConfigManager.getElementConfig("kill_icon", "valorant");
        if (liveConfig != null) {
            loadConfig(liveConfig);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > displayDuration) {
            visible = false;
            startTime = -1L;
            return;
        }

        float alpha = resolveAlpha(elapsed);
        float iconOffsetY = resolveIconOffsetY(elapsed);
        float barTravel = resolveBarTravel(elapsed);
        float barRotation = resolveBarRotation(elapsed);
        FlashState flash = resolveFlashState(elapsed);

        String emblemTexturePath = ElementTextureDefinition.getSelectedTextureFileName(
            ConfigManager.getCurrentPresetId(),
            "kill_icon/valorant",
            "emblem",
            currentConfig
        );
        String frameTexturePath = ElementTextureDefinition.getSelectedTextureFileName(
            ConfigManager.getCurrentPresetId(),
            "kill_icon/valorant",
            "frame",
            currentConfig
        );
        String ringTexturePath = ElementTextureDefinition.getSelectedTextureFileName(
            ConfigManager.getCurrentPresetId(),
            "kill_icon/valorant",
            "ring",
            currentConfig
        );
        String bladeTexturePath = ElementTextureDefinition.getSelectedTextureFileName(
            ConfigManager.getCurrentPresetId(),
            "kill_icon/valorant",
            "blade",
            currentConfig
        );
        String barTexturePath = ElementTextureDefinition.getSelectedTextureFileName(
            ConfigManager.getCurrentPresetId(),
            "kill_icon/valorant",
            "bar",
            currentConfig
        );
        String headshotTexturePath = ElementTextureDefinition.getSelectedTextureFileName(
            ConfigManager.getCurrentPresetId(),
            "kill_icon/valorant",
            "headshot",
            currentConfig
        );
        String styleId = resolveStyleId();
        boolean gaiaSkin = isGaiaSkin(styleId, emblemTexturePath, barTexturePath);
        boolean bladeEnabled = ValorantStyleCatalog.usesBlade(styleId);
        String activeRingTexturePath = configRingEnabled ? ringTexturePath : null;
        String activeBladeTexturePath = bladeEnabled ? bladeTexturePath : null;
        float iconLayerBase = resolveIconLayerBase(emblemTexturePath, frameTexturePath, activeRingTexturePath, activeBladeTexturePath);

        float emblemWidth = ICON_BASE_SIZE * configIconScale * resolveTextureRatio("emblem", "texture_frame_width_ratio", emblemTexturePath, iconLayerBase);
        float emblemHeight = ICON_BASE_SIZE * configIconScale * resolveTextureRatio("emblem", "texture_frame_height_ratio", emblemTexturePath, iconLayerBase);
        float frameWidth = ICON_BASE_SIZE * configFrameScale * resolveTextureRatio("frame", "texture_frame_width_ratio", frameTexturePath, iconLayerBase);
        float frameHeight = ICON_BASE_SIZE * configFrameScale * resolveTextureRatio("frame", "texture_frame_height_ratio", frameTexturePath, iconLayerBase);
        float ringWidth = ICON_BASE_SIZE * configRingScale * resolveTextureRatio("ring", "texture_frame_width_ratio", activeRingTexturePath, iconLayerBase);
        float ringHeight = ICON_BASE_SIZE * configRingScale * resolveTextureRatio("ring", "texture_frame_height_ratio", activeRingTexturePath, iconLayerBase);
        float bladeWidth = ICON_BASE_SIZE * configBladeScale * resolveTextureRatio("blade", "texture_frame_width_ratio", activeBladeTexturePath, iconLayerBase);
        float bladeHeight = ICON_BASE_SIZE * configBladeScale * resolveTextureRatio("blade", "texture_frame_height_ratio", activeBladeTexturePath, iconLayerBase);
        float headshotWidth = ICON_BASE_SIZE * configHeadshotScale * resolveTextureRatio("headshot", "texture_frame_width_ratio", headshotTexturePath, iconLayerBase);
        float headshotHeight = ICON_BASE_SIZE * configHeadshotScale * resolveTextureRatio("headshot", "texture_frame_height_ratio", headshotTexturePath, iconLayerBase);
        float barBaseSize = BAR_BASE_SIZE * configBarScale;
        float barWidth = barBaseSize * resolveTextureRatio("bar", "texture_frame_width_ratio", barTexturePath, 0.0f);
        float barHeight = barBaseSize * resolveTextureRatio("bar", "texture_frame_height_ratio", barTexturePath, 0.0f);
        float effectiveBrightness = resolveEffectiveBrightness(gaiaSkin);
        float effectiveContrast = resolveEffectiveContrast(gaiaSkin);
        boolean accentTintEnabled = isAccentTintEnabled();
        int accentColor = resolveAccentColor();
        int defaultParticleColor = resolveDefaultParticleColor(accentTintEnabled, accentColor, gaiaSkin);
        int xSparksDefaultColor = resolveConfiguredColor("color_icon_flash", DEFAULT_ICON_FLASH_COLOR);
        int headshotOverlayColor = resolveConfiguredColor("color_headshot_overlay", DEFAULT_ICON_FLASH_COLOR);
        boolean frameEntryLocked = isEntryMotionFrameLocked(styleId);
        float rootMotionOffsetY = frameEntryLocked ? 0.0f : iconOffsetY;
        float emblemMotionOffsetY = frameEntryLocked ? iconOffsetY : 0.0f;
        float resolvedHeadshotXOffset = ValorantStyleCatalog.getHeadshotOffsetX(styleId) + configHeadshotXOffset;
        float resolvedHeadshotYOffset = ValorantStyleCatalog.getHeadshotOffsetY(styleId) + configHeadshotYOffset;
        float barRadiusOffset = configBarRadiusOffset;
        float barCenterXOffset = configBarXOffset;
        float barCenterYOffset = configBarYOffset;
        float[] barLayoutAngles = BAR_LAYOUT_ANGLES[Mth.clamp(comboCount, 1, 6) - 1];

        try {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, centerY + rootMotionOffsetY, 0.0f);
            float renderScale = configScale * GLOBAL_VISUAL_SCALE;
            guiGraphics.pose().scale(renderScale, renderScale, 1.0f);

            renderBaseParticles(guiGraphics, elapsed, alpha, effectiveBrightness, effectiveContrast, defaultParticleColor);

            renderBarRing(
                guiGraphics,
                barTexturePath,
                barWidth,
                barHeight,
                barTravel,
                alpha,
                barRotation,
                barRadiusOffset,
                barCenterXOffset,
                barCenterYOffset,
                barLayoutAngles,
                accentTintEnabled,
                accentColor,
                effectiveBrightness,
                effectiveContrast,
                elapsed
            );
            RenderSystem.defaultBlendFunc();

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(configFrameXOffset, configFrameYOffset, 0.0f);
            drawColorizableTexture(
                guiGraphics,
                frameTexturePath,
                frameWidth,
                frameHeight,
                alpha,
                0xFFFFFF,
                false,
                accentTintEnabled,
                accentColor,
                effectiveBrightness,
                effectiveContrast
            );
            if (configRingEnabled && ringTexturePath != null && !ringTexturePath.isEmpty()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(configRingXOffset, configRingYOffset, 0.0f);
                drawColorizableTexture(
                    guiGraphics,
                    ringTexturePath,
                    ringWidth,
                    ringHeight,
                    alpha,
                    0xFFFFFF,
                    false,
                    accentTintEnabled,
                    accentColor,
                    effectiveBrightness,
                    effectiveContrast
                );
                guiGraphics.pose().popPose();
            }
            if (activeBladeTexturePath != null && !activeBladeTexturePath.isEmpty()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(configBladeXOffset, configBladeYOffset, 0.0f);
                float bladeRotation = resolveFrameBladeRotation(styleId, elapsed);
                if (Math.abs(bladeRotation) > 0.01f) {
                    guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(bladeRotation));
                }
                drawColorizableTexture(
                    guiGraphics,
                    activeBladeTexturePath,
                    bladeWidth,
                    bladeHeight,
                    alpha,
                    0xFFFFFF,
                    false,
                    accentTintEnabled,
                    accentColor,
                    effectiveBrightness,
                    effectiveContrast
                );
                guiGraphics.pose().popPose();
            }
            guiGraphics.pose().popPose();

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(configIconXOffset, configIconYOffset + emblemMotionOffsetY, 0.0f);
            drawColorizableTexture(
                guiGraphics,
                emblemTexturePath,
                emblemWidth,
                emblemHeight,
                alpha,
                0xFFFFFF,
                false,
                accentTintEnabled,
                accentColor,
                effectiveBrightness,
                effectiveContrast
            );
            if (flash.alpha() > 0.0f) {
                drawColorizableTexture(
                    guiGraphics,
                    emblemTexturePath,
                    emblemWidth,
                    emblemHeight,
                    alpha * flash.alpha(),
                    flash.color(),
                    false,
                    accentTintEnabled,
                    accentColor,
                    effectiveBrightness,
                    effectiveContrast
                );
            }
            if (headshotTrigger) {
                HeadshotAnimState hsAnim = resolveHeadshotAnimState(elapsed, headshotOverlayColor);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(resolvedHeadshotXOffset, resolvedHeadshotYOffset, 0.0f);
                if (Math.abs(hsAnim.scale() - 1.0f) > 0.001f) {
                    guiGraphics.pose().scale(hsAnim.scale(), hsAnim.scale(), 1.0f);
                }
                drawColorizableTexture(
                    guiGraphics,
                    headshotTexturePath,
                    headshotWidth,
                    headshotHeight,
                    alpha,
                    hsAnim.color(),
                    false,
                    false,
                    0xFFFFFF,
                    1.0f,
                    1.0f
                );
                guiGraphics.pose().popPose();
            }
            guiGraphics.pose().popPose();
            RenderSystem.defaultBlendFunc();

            renderOverlayParticles(
                guiGraphics,
                HERO_FLAME_PARTICLE,
                elapsed,
                alpha,
                emblemWidth,
                emblemHeight,
                effectiveBrightness,
                effectiveContrast,
                defaultParticleColor,
                emblemMotionOffsetY
            );
            if (comboCount >= 5) {
                renderOverlayParticles(
                    guiGraphics,
                    LARGE_SPARKS_PARTICLE,
                    elapsed,
                    alpha,
                    emblemWidth,
                    emblemHeight,
                    effectiveBrightness,
                    effectiveContrast,
                    defaultParticleColor,
                    emblemMotionOffsetY
                );
            }
            if (headshotTrigger) {
                renderOverlayParticles(
                    guiGraphics,
                    X_SPARKS_PARTICLE,
                    elapsed,
                    alpha,
                    emblemWidth,
                    emblemHeight,
                    effectiveBrightness,
                    effectiveContrast,
                    xSparksDefaultColor,
                    emblemMotionOffsetY
                );
            }

            guiGraphics.pose().popPose();
        } finally {
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private void loadConfig(JsonObject config) {
        currentConfig = config;
        configScale = config.has("scale") ? config.get("scale").getAsFloat() : 1.0f;
        configXOffset = config.has("x_offset") ? config.get("x_offset").getAsFloat() : 0.0f;
        configYOffset = config.has("y_offset") ? config.get("y_offset").getAsFloat() : 80.0f;
        configBarXOffset = config.has("bar_x_offset") ? config.get("bar_x_offset").getAsFloat() : 0.0f;
        configBarYOffset = config.has("bar_y_offset") ? config.get("bar_y_offset").getAsFloat() : 0.0f;
        configBarRadiusOffset = config.has("bar_radius_offset") ? config.get("bar_radius_offset").getAsFloat() : 0.0f;
        configIconScale = config.has("icon_scale") ? config.get("icon_scale").getAsFloat() : 1.0f;
        configIconXOffset = config.has("icon_x_offset") ? config.get("icon_x_offset").getAsFloat() : 0.0f;
        configIconYOffset = config.has("icon_y_offset") ? config.get("icon_y_offset").getAsFloat() : 0.0f;
        configFrameScale = config.has("frame_scale") ? config.get("frame_scale").getAsFloat() : 1.0f;
        configRingEnabled = !config.has("enable_ring") || config.get("enable_ring").getAsBoolean();
        configRingScale = config.has("ring_scale") ? config.get("ring_scale").getAsFloat() : configFrameScale;
        configBladeScale = config.has("blade_scale") ? config.get("blade_scale").getAsFloat() : configFrameScale;
        configBarScale = config.has("bar_scale") ? config.get("bar_scale").getAsFloat() : 1.0f;
        configHeadshotScale = config.has("headshot_scale") ? config.get("headshot_scale").getAsFloat() : 1.0f;
        configFrameXOffset = config.has("frame_x_offset") ? config.get("frame_x_offset").getAsFloat() : 0.0f;
        configFrameYOffset = config.has("frame_y_offset") ? config.get("frame_y_offset").getAsFloat() : 0.0f;
        configRingXOffset = config.has("ring_x_offset") ? config.get("ring_x_offset").getAsFloat() : configFrameXOffset;
        configRingYOffset = config.has("ring_y_offset") ? config.get("ring_y_offset").getAsFloat() : configFrameYOffset;
        configBladeXOffset = config.has("blade_x_offset") ? config.get("blade_x_offset").getAsFloat() : configFrameXOffset;
        configBladeYOffset = config.has("blade_y_offset") ? config.get("blade_y_offset").getAsFloat() : configFrameYOffset;
        configHeadshotXOffset = config.has("headshot_x_offset") ? config.get("headshot_x_offset").getAsFloat() : 0.0f;
        configHeadshotYOffset = config.has("headshot_y_offset") ? config.get("headshot_y_offset").getAsFloat() : 0.0f;
        configHeadshotAnimInitialScale = Mth.clamp(config.has("headshot_anim_initial_scale") ? config.get("headshot_anim_initial_scale").getAsFloat() : 1.8f, 1.0f, 5.0f);
        configHeadshotAnimDuration = Math.max(0.0f, config.has("headshot_anim_duration") ? config.get("headshot_anim_duration").getAsFloat() : 0.25f);
        configHeadshotAnimFlickerSpeed = Math.max(0.0f, config.has("headshot_anim_flicker_speed") ? config.get("headshot_anim_flicker_speed").getAsFloat() : 18.0f);
        configHeadshotAnimScaleCurve = Mth.clamp(config.has("headshot_anim_scale_curve") ? config.get("headshot_anim_scale_curve").getAsFloat() : 0.6f, 0.0f, 1.0f);
        configHeadshotAnimFlickerColor = resolveConfiguredColor("color_headshot_anim_flicker", 0xFFFFFF);
        configBarEntryInitialScale = Mth.clamp(config.has("bar_entry_initial_scale") ? config.get("bar_entry_initial_scale").getAsFloat() : 1.6f, 1.0f, 5.0f);
        configBarEntryDuration = Math.max(0.0f, config.has("bar_entry_duration") ? config.get("bar_entry_duration").getAsFloat() : 0.18f);
        configBarEntryScaleCurve = Mth.clamp(config.has("bar_entry_scale_curve") ? config.get("bar_entry_scale_curve").getAsFloat() : 0.7f, 0.0f, 1.0f);
        configBrightness = config.has("brightness") ? config.get("brightness").getAsFloat() : 1.0f;
        configContrast = config.has("contrast") ? config.get("contrast").getAsFloat() : 1.0f;
        configIconGlowEnabled = config.has("enable_icon_glow") && config.get("enable_icon_glow").getAsBoolean();
        configIconGlowColor = resolveConfiguredColor("color_icon_glow", 0xFFFFFF);
        configIconGlowIntensity = Mth.clamp(config.has("icon_glow_intensity") ? config.get("icon_glow_intensity").getAsFloat() : 0.45f, 0.0f, 1.0f);
        configIconGlowSize = Math.max(0.0f, config.has("icon_glow_size") ? config.get("icon_glow_size").getAsFloat() : 4.0f);
        configIconAntialiasing = config.has("enable_icon_antialiasing") && config.get("enable_icon_antialiasing").getAsBoolean();
        configIconEntryCurve = Mth.clamp(config.has("icon_entry_curve") ? config.get("icon_entry_curve").getAsFloat() : 1.0f, 0.0f, 1.0f);
        displayDuration = resolveDisplayDuration(config);
    }

    private long resolveDisplayDuration(JsonObject config) {
        if (config != null && config.has("display_duration")) {
            long configuredDuration = (long) (config.get("display_duration").getAsFloat() * 1000.0f);
            return configuredDuration > 0L ? configuredDuration : DEFAULT_DISPLAY_DURATION;
        }
        return DEFAULT_DISPLAY_DURATION;
    }

    private long resolveDurationMillis(String key, float fallbackSeconds) {
        float seconds = getFloatConfig(key, fallbackSeconds);
        return Math.max(0L, Math.round(seconds * 1000.0f));
    }

    private float resolveAlpha(long elapsed) {
        float alpha = 1.0f;
        if (comboCount >= 5 && elapsed < FIVE_PLUS_FADE_IN_DURATION_MS) {
            float progress = (float) elapsed / (float) FIVE_PLUS_FADE_IN_DURATION_MS;
            alpha *= easeOutQuad(progress);
        }
        if (displayDuration <= FADE_OUT_DURATION_MS || elapsed <= displayDuration - FADE_OUT_DURATION_MS) {
            return alpha;
        }
        float progress = (float) (elapsed - (displayDuration - FADE_OUT_DURATION_MS)) / (float) FADE_OUT_DURATION_MS;
        return alpha * (1.0f - Mth.clamp(progress, 0.0f, 1.0f));
    }

    private float resolveIconOffsetY(long elapsed) {
        if (comboCount < 5) {
            return 0.0f;
        }
        float startOffset = getFloatConfig("icon_entry_offset_y", DEFAULT_ICON_ENTRY_OFFSET_PX);
        long durationMs = resolveDurationMillis("icon_entry_duration", DEFAULT_ICON_ENTRY_DURATION_SECONDS);
        if (durationMs <= 0L || Math.abs(startOffset) < 0.01f || elapsed >= durationMs) {
            return 0.0f;
        }
        float normalizedProgress = Mth.clamp((float) elapsed / (float) durationMs, 0.0f, 1.0f);
        float easedProgress = easeOutCubic(normalizedProgress);
        float progress = Mth.lerp(configIconEntryCurve, normalizedProgress, easedProgress);
        return Mth.lerp(progress, startOffset, 0.0f);
    }

    private float resolveBarTravel(long elapsed) {
        if (elapsed < BAR_BOUNCE_START_MS || elapsed >= BAR_BOUNCE_END_MS) {
            return 0.0f;
        }
        if (elapsed <= BAR_BOUNCE_PEAK_MS) {
            float progress = (float) (elapsed - BAR_BOUNCE_START_MS) / (float) (BAR_BOUNCE_PEAK_MS - BAR_BOUNCE_START_MS);
            return Mth.lerp(easeOutSoft(progress), 0.0f, BAR_BOUNCE_DISTANCE_PX);
        }
        if (elapsed <= BAR_BOUNCE_HOLD_END_MS) {
            return BAR_BOUNCE_DISTANCE_PX;
        }
        float progress = (float) (elapsed - BAR_BOUNCE_HOLD_END_MS) / (float) (BAR_BOUNCE_END_MS - BAR_BOUNCE_HOLD_END_MS);
        return Mth.lerp(easeInQuart(progress), BAR_BOUNCE_DISTANCE_PX, 0.0f);
    }

    private float resolveBarRotation(long elapsed) {
        if (comboCount <= 1 || elapsed <= BAR_SPIN_START_MS) {
            return 0.0f;
        }
        long rotationDuration = comboCount >= 5 ? BAR_SPIN_DURATION_360_MS : BAR_SPIN_DURATION_180_MS;
        float targetRotation = comboCount >= 5 ? 360.0f : 180.0f;
        float progress = (float) (elapsed - BAR_SPIN_START_MS) / (float) rotationDuration;
        progress = Mth.clamp(progress, 0.0f, 1.0f);
        return targetRotation * easeOutCubic(progress) * spinDirection;
    }

    private FlashState resolveFlashState(long elapsed) {
        int flashCount = Mth.clamp(getIntConfig("icon_flash_count", 4), 0, MAX_ICON_FLASH_COUNT);
        if (flashCount <= 0) {
            return FlashState.none();
        }
        long holdDurationMs = resolveDurationMillis("icon_flash_hold_duration", DEFAULT_ICON_FLASH_HOLD_DURATION_SECONDS);
        if (holdDurationMs <= 0L) {
            return FlashState.none();
        }

        long gapDurationMs = Math.max(20L, Math.round(holdDurationMs * 0.75f));
        long cycleDurationMs = holdDurationMs + gapDurationMs;
        long totalDurationMs = holdDurationMs + cycleDurationMs * (flashCount - 1L);
        if (elapsed >= totalDurationMs) {
            return FlashState.none();
        }

        long cycleElapsed = elapsed % cycleDurationMs;
        if (cycleElapsed >= holdDurationMs) {
            return FlashState.none();
        }

        float holdMs = (float) holdDurationMs;
        float localElapsed = (float) cycleElapsed;
        float edgeDurationMs = Math.min(35.0f, holdMs * 0.25f);
        float pulseAlpha = 1.0f;
        if (edgeDurationMs > 1.0f) {
            if (localElapsed < edgeDurationMs) {
                pulseAlpha = localElapsed / edgeDurationMs;
            } else if (holdMs - localElapsed < edgeDurationMs) {
                pulseAlpha = (holdMs - localElapsed) / edgeDurationMs;
            }
        }
        int color = resolveConfiguredColor("color_icon_flash", DEFAULT_ICON_FLASH_COLOR);
        return new FlashState(color, Mth.clamp(pulseAlpha, 0.0f, 1.0f) * ICON_FLASH_MAX_ALPHA);
    }

    private float resolveFrameRatio(String textureKey, String suffixKey) {
        if (currentConfig == null || textureKey == null) {
            return 1.0f;
        }
        String key = "anim_" + textureKey + "_" + suffixKey;
        if (!currentConfig.has(key)) {
            return 1.0f;
        }
        float value = currentConfig.get(key).getAsFloat();
        return value > 0.0f ? value : 1.0f;
    }

    private float resolveTextureRatio(String textureKey, String suffixKey, String texturePath, float sharedBaseDimension) {
        float configuredRatio = resolveFrameRatio(textureKey, suffixKey);
        TextureMetrics metrics = resolveTextureMetrics(texturePath);
        if (metrics == null) {
            return configuredRatio;
        }

        float baseDimension = sharedBaseDimension > 0.0f
            ? sharedBaseDimension
            : metrics.maxDimension();
        float defaultRatio = suffixKey.contains("width")
            ? metrics.width() / baseDimension
            : metrics.height() / baseDimension;
        if (currentConfig == null) {
            return defaultRatio;
        }

        String configKey = "anim_" + textureKey + "_" + suffixKey;
        if (!currentConfig.has(configKey)) {
            return defaultRatio;
        }

        boolean usingSharedBase = sharedBaseDimension > 0.0f
            && Math.abs(sharedBaseDimension - metrics.maxDimension()) > 0.001f;
        if (Math.abs(configuredRatio - 1.0f) <= 0.001f && (usingSharedBase || !metrics.square())) {
            return defaultRatio;
        }
        return configuredRatio;
    }

    private float resolveIconLayerBase(String emblemTexturePath, String frameTexturePath, String ringTexturePath, String bladeTexturePath) {
        float base = 0.0f;
        TextureMetrics emblemMetrics = resolveTextureMetrics(emblemTexturePath);
        TextureMetrics frameMetrics = resolveTextureMetrics(frameTexturePath);
        TextureMetrics ringMetrics = resolveTextureMetrics(ringTexturePath);
        TextureMetrics bladeMetrics = resolveTextureMetrics(bladeTexturePath);
        if (emblemMetrics != null) {
            base = Math.max(base, emblemMetrics.maxDimension());
        }
        if (frameMetrics != null) {
            base = Math.max(base, frameMetrics.maxDimension());
        }
        if (ringMetrics != null) {
            base = Math.max(base, ringMetrics.maxDimension());
        }
        if (bladeMetrics != null) {
            base = Math.max(base, bladeMetrics.maxDimension());
        }
        return base;
    }

    private TextureMetrics resolveTextureMetrics(String texturePath) {
        if (texturePath == null || texturePath.isEmpty()) {
            return null;
        }

        String presetId = ConfigManager.getCurrentPresetId();
        String cacheKey = presetId + ":" + texturePath;
        TextureMetrics cached = TEXTURE_ASPECT_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            byte[] bytes = ExternalTextureManager.readTextureBytes(presetId, texturePath);
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
            if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
                return null;
            }
            TextureMetrics metrics = new TextureMetrics(
                source.getWidth(),
                source.getHeight(),
                Math.max(source.getWidth(), source.getHeight()),
                source.getWidth() == source.getHeight()
            );
            TEXTURE_ASPECT_CACHE.put(cacheKey, metrics);
            return metrics;
        } catch (IOException ignored) {
            return null;
        }
    }

    private int resolveAccentColor() {
        if (currentConfig != null) {
            if (currentConfig.has("color_accent")) {
                return resolveConfiguredColor("color_accent", DEFAULT_ACCENT_COLOR);
            }
            if (currentConfig.has("color_gaia_accent")) {
                return resolveConfiguredColor("color_gaia_accent", DEFAULT_ACCENT_COLOR);
            }
        }
        return DEFAULT_ACCENT_COLOR;
    }

    private int resolveDefaultParticleColor(boolean accentTintEnabled, int accentColor, boolean gaiaSkin) {
        return accentTintEnabled
            ? accentColor
            : (gaiaSkin ? GAIA_PARTICLE_SMALL_COLOR : PRIME_PARTICLE_SMALL_COLOR);
    }

    private boolean isAccentTintEnabled() {
        if (currentConfig != null && currentConfig.has("enable_accent_tint")) {
            return currentConfig.get("enable_accent_tint").getAsBoolean();
        }
        if (currentConfig != null && currentConfig.has("color_gaia_accent")) {
            String legacyColor = currentConfig.get("color_gaia_accent").getAsString();
            return legacyColor != null && !legacyColor.equalsIgnoreCase(String.format("#%06X", DEFAULT_ACCENT_COLOR));
        }
        return false;
    }

    private int resolveConfiguredColor(String key, int fallback) {
        if (currentConfig == null || key == null || !currentConfig.has(key)) {
            return fallback;
        }
        String raw = currentConfig.get(key).getAsString();
        if (raw != null && raw.matches("^#[0-9A-Fa-f]{6}$")) {
            try {
                return Integer.parseInt(raw.substring(1), 16);
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private boolean getBooleanConfig(String key, boolean fallback) {
        return currentConfig != null && key != null && currentConfig.has(key)
            ? currentConfig.get(key).getAsBoolean()
            : fallback;
    }

    private float getFloatConfig(String key, float fallback) {
        return currentConfig != null && key != null && currentConfig.has(key)
            ? currentConfig.get(key).getAsFloat()
            : fallback;
    }

    private int getIntConfig(String key, int fallback) {
        return currentConfig != null && key != null && currentConfig.has(key)
            ? currentConfig.get(key).getAsInt()
            : fallback;
    }

    private int resolveLayerParticleColor(String keyPrefix, int fallbackColor) {
        if (getBooleanConfig("enable_custom_color_" + keyPrefix, false)) {
            return resolveConfiguredColor("color_" + keyPrefix, fallbackColor);
        }
        return fallbackColor;
    }

    private void renderBaseParticles(
        GuiGraphics guiGraphics,
        long elapsed,
        float alpha,
        float brightness,
        float contrast,
        int defaultColor
    ) {
        if (!getBooleanConfig("enable_base_particle", true)) {
            return;
        }

        float opacity = Mth.clamp(getFloatConfig("base_particle_opacity", 1.0f), 0.0f, 1.0f);
        ParticleSheetSpec baseSheet = BASE_PARTICLE_SHEETS[Mth.clamp(selectedBaseParticleIndex, 0, BASE_PARTICLE_SHEETS.length - 1)];
        long particleDurationMs = resolveParticleDurationMs(
            baseSheet.totalFrames(),
            baseSheet.framesPerSecond(),
            getFloatConfig("base_particle_speed", DEFAULT_PARTICLE_SPEED)
        );
        if (alpha <= 0.0f || elapsed >= particleDurationMs || opacity <= 0.01f) {
            return;
        }

        TextureFrame frame = resolveParticleFrame(elapsed, particleDurationMs, baseSheet.frameWidth(), baseSheet.frameHeight(), baseSheet.totalFrames());
        float particleSize = PARTICLE_BASE_SIZE * Math.max(0.01f, getFloatConfig("base_particle_scale", DEFAULT_PARTICLE_SCALE));
        float layerAlpha = alpha * opacity;
        int particleColor = resolveLayerParticleColor("base_particle", defaultColor);
        ResourceLocation texture = resolveParticleTexture(baseSheet.texturePath(), brightness, contrast);

        renderMirroredParticleBurst(
            guiGraphics,
            texture,
            frame,
            particleSize,
            particleSize,
            getFloatConfig("base_particle_center_x_offset", 0.0f),
            getFloatConfig("base_particle_x_offset", 0.0f),
            getFloatConfig("base_particle_y_offset", 0.0f),
            layerAlpha,
            particleColor,
            false,
            baseSheet.anchorXRatio(),
            baseSheet.anchorYRatio()
        );
    }

    private void renderOverlayParticles(
        GuiGraphics guiGraphics,
        ParticleOverlaySpec spec,
        long elapsed,
        float alpha,
        float iconWidth,
        float iconHeight,
        float brightness,
        float contrast,
        int defaultColor,
        float extraYOffset
    ) {
        String keyPrefix = spec.configKeyPrefix();
        if (!getBooleanConfig("enable_" + keyPrefix, true)) {
            return;
        }

        float opacity = Mth.clamp(getFloatConfig(keyPrefix + "_opacity", 1.0f), 0.0f, 1.0f);
        long particleDurationMs = resolveParticleDurationMs(
            spec.totalFrames(),
            spec.framesPerSecond(),
            getFloatConfig(keyPrefix + "_speed", DEFAULT_PARTICLE_SPEED)
        );
        if (alpha <= 0.0f || elapsed >= particleDurationMs || opacity <= 0.01f) {
            return;
        }

        float sourceScale = Math.max(0.01f, getFloatConfig(keyPrefix + "_scale", spec.defaultSourceScale()));
        float drawWidth = iconWidth * sourceScale * ((float) spec.frameWidth() / ICON_TEXTURE_SOURCE_SIZE);
        float drawHeight = iconHeight * sourceScale * ((float) spec.frameHeight() / ICON_TEXTURE_SOURCE_SIZE);
        if (drawWidth <= 0.0f || drawHeight <= 0.0f) {
            return;
        }

        TextureFrame frame = resolveParticleFrame(elapsed, particleDurationMs, spec.frameWidth(), spec.frameHeight(), spec.totalFrames());
        ResourceLocation texture = resolveParticleTexture(spec.texturePath(), brightness, contrast);
        float layerAlpha = alpha * opacity;
        int particleColor = resolveLayerParticleColor(keyPrefix, defaultColor);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(
            getFloatConfig(keyPrefix + "_x_offset", spec.offsetXPx()),
            getFloatConfig(keyPrefix + "_y_offset", spec.offsetYPx()) + extraYOffset,
            0.0f
        );
        drawAnchoredResourceTextureFrame(
            guiGraphics,
            texture,
            frame,
            drawWidth,
            drawHeight,
            spec.anchorXRatio(),
            spec.anchorYRatio(),
            layerAlpha,
            particleColor,
            spec.additive(),
            false
        );
        guiGraphics.pose().popPose();
    }

    private long resolveParticleDurationMs(int totalFrames, float framesPerSecond, float speedMultiplier) {
        float speed = Mth.clamp(speedMultiplier, 0.1f, 4.0f);
        float baseDurationMs = framesPerSecond <= 0.0f
            ? PARTICLE_BASE_DURATION_MS
            : (1000.0f * totalFrames) / framesPerSecond;
        return Math.max(150L, Math.round(baseDurationMs / speed));
    }

    private TextureFrame resolveParticleFrame(long elapsed, long particleDurationMs, int frameWidth, int frameHeight, int totalFrames) {
        float progress = Mth.clamp((float) elapsed / (float) particleDurationMs, 0.0f, 0.9999f);
        int frameIndex = Mth.clamp((int) (progress * totalFrames), 0, totalFrames - 1);
        return new TextureFrame(
            0,
            frameIndex * frameHeight,
            frameWidth,
            frameHeight,
            frameWidth,
            frameHeight * totalFrames
        );
    }

    private void renderMirroredParticleBurst(
        GuiGraphics guiGraphics,
        ResourceLocation texture,
        TextureFrame frame,
        float drawWidth,
        float drawHeight,
        float centerOffsetX,
        float mirrorOffsetX,
        float centerOffsetY,
        float alpha,
        int rgb,
        boolean additive,
        float anchorXRatio,
        float anchorYRatio
    ) {
        if (alpha <= 0.0f || texture == null) {
            return;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerOffsetX + mirrorOffsetX, centerOffsetY, 0.0f);
        drawAnchoredResourceTextureFrame(
            guiGraphics,
            texture,
            frame,
            drawWidth,
            drawHeight,
            anchorXRatio,
            anchorYRatio,
            alpha,
            rgb,
            additive,
            false
        );
        guiGraphics.pose().popPose();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerOffsetX - mirrorOffsetX, centerOffsetY, 0.0f);
        drawAnchoredResourceTextureFrame(
            guiGraphics,
            texture,
            frame,
            drawWidth,
            drawHeight,
            1.0f - anchorXRatio,
            anchorYRatio,
            alpha,
            rgb,
            additive,
            true
        );
        guiGraphics.pose().popPose();
    }

    private ResourceLocation resolveParticleTexture(String texturePath, float brightness, float contrast) {
        if (!needsTextureProcessing(texturePath, false, brightness, contrast)) {
            return ModTextures.get(texturePath);
        }
        ResourceLocation processedTexture = getOrCreateProcessedTexture(
            ConfigManager.getCurrentPresetId(),
            texturePath,
            false,
            0xFFFFFF,
            brightness,
            contrast
        );
        return processedTexture != null ? processedTexture : ModTextures.get(texturePath);
    }

    private void drawCenteredTexture(
        GuiGraphics guiGraphics,
        String texturePath,
        float drawWidth,
        float drawHeight,
        float alpha,
        int rgb,
        boolean additive
    ) {
        if (texturePath == null) {
            return;
        }
        ResourceLocation texture = ModTextures.get(texturePath);
        if (texture == null) {
            return;
        }
        applyTextureFiltering(texture);
        if (additive) {
            RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE
            );
        } else {
            RenderSystem.defaultBlendFunc();
        }
        float red = ((rgb >> 16) & 0xFF) / 255.0f;
        float green = ((rgb >> 8) & 0xFF) / 255.0f;
        float blue = (rgb & 0xFF) / 255.0f;
        RenderSystem.setShaderColor(red, green, blue, alpha);
        guiGraphics.blit(
            texture,
            Math.round(-drawWidth / 2.0f),
            Math.round(-drawHeight / 2.0f),
            0,
            0,
            Math.round(drawWidth),
            Math.round(drawHeight),
            Math.round(drawWidth),
            Math.round(drawHeight)
        );
    }

    private void drawColorizableTexture(
        GuiGraphics guiGraphics,
        String texturePath,
        float drawWidth,
        float drawHeight,
        float alpha,
        int rgb,
        boolean additive,
        boolean accentTintEnabled,
        int accentColor,
        float brightness,
        float contrast
    ) {
        ResourceLocation processedTexture = null;
        boolean useProcessedTexture = needsTextureProcessing(texturePath, accentTintEnabled, brightness, contrast);
        if (useProcessedTexture) {
            processedTexture = getOrCreateProcessedTexture(
                ConfigManager.getCurrentPresetId(),
                texturePath,
                accentTintEnabled,
                accentColor,
                brightness,
                contrast
            );
        }

        if (configIconGlowEnabled) {
            if (processedTexture != null) {
                drawGlowResourceTexture(guiGraphics, processedTexture, drawWidth, drawHeight, alpha);
            } else {
                drawGlowTexture(guiGraphics, texturePath, drawWidth, drawHeight, alpha, accentTintEnabled, accentColor, brightness, contrast);
            }
        }

        if (!useProcessedTexture || processedTexture == null) {
            drawCenteredTexture(guiGraphics, texturePath, drawWidth, drawHeight, alpha, rgb, additive);
            return;
        }

        drawCenteredResourceTexture(guiGraphics, processedTexture, drawWidth, drawHeight, alpha, rgb, additive);
    }

    private void drawCenteredResourceTexture(
        GuiGraphics guiGraphics,
        ResourceLocation texture,
        float drawWidth,
        float drawHeight,
        float alpha,
        int rgb,
        boolean additive
    ) {
        if (texture == null) {
            return;
        }
        applyTextureFiltering(texture);
        if (additive) {
            RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE
            );
        } else {
            RenderSystem.defaultBlendFunc();
        }
        float red = ((rgb >> 16) & 0xFF) / 255.0f;
        float green = ((rgb >> 8) & 0xFF) / 255.0f;
        float blue = (rgb & 0xFF) / 255.0f;
        RenderSystem.setShaderColor(red, green, blue, alpha);
        guiGraphics.blit(
            texture,
            Math.round(-drawWidth / 2.0f),
            Math.round(-drawHeight / 2.0f),
            0,
            0,
            Math.round(drawWidth),
            Math.round(drawHeight),
            Math.round(drawWidth),
            Math.round(drawHeight)
        );
    }

    private void drawGlowTexture(
        GuiGraphics guiGraphics,
        String texturePath,
        float drawWidth,
        float drawHeight,
        float alpha,
        boolean accentTintEnabled,
        int accentColor,
        float brightness,
        float contrast
    ) {
        if (texturePath == null || texturePath.isEmpty()) {
            return;
        }
        boolean useProcessedTexture = needsTextureProcessing(texturePath, accentTintEnabled, brightness, contrast);
        if (useProcessedTexture) {
            ResourceLocation processedTexture = getOrCreateProcessedTexture(
                ConfigManager.getCurrentPresetId(),
                texturePath,
                accentTintEnabled,
                accentColor,
                brightness,
                contrast
            );
            if (processedTexture != null) {
                drawGlowResourceTexture(guiGraphics, processedTexture, drawWidth, drawHeight, alpha);
                return;
            }
        }
        ResourceLocation texture = ModTextures.get(texturePath);
        if (texture != null) {
            drawGlowResourceTexture(guiGraphics, texture, drawWidth, drawHeight, alpha);
        }
    }

    private void drawGlowResourceTexture(
        GuiGraphics guiGraphics,
        ResourceLocation texture,
        float drawWidth,
        float drawHeight,
        float alpha
    ) {
        if (texture == null || !configIconGlowEnabled || configIconGlowSize <= 0.01f || configIconGlowIntensity <= 0.001f || alpha <= 0.001f) {
            return;
        }

        float outerSpread = Math.max(0.5f, configIconGlowSize);
        float innerSpread = outerSpread * 0.55f;
        float glowAlpha = Mth.clamp(alpha * configIconGlowIntensity, 0.0f, 1.0f);
        int color = configIconGlowColor;

        for (float[] offset : GLOW_OFFSETS) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(offset[0] * outerSpread, offset[1] * outerSpread, 0.0f);
            drawCenteredResourceTexture(guiGraphics, texture, drawWidth, drawHeight, glowAlpha * 0.16f, color, true);
            guiGraphics.pose().popPose();
        }

        for (float[] offset : GLOW_OFFSETS) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(offset[0] * innerSpread, offset[1] * innerSpread, 0.0f);
            drawCenteredResourceTexture(guiGraphics, texture, drawWidth, drawHeight, glowAlpha * 0.11f, color, true);
            guiGraphics.pose().popPose();
        }

        drawCenteredResourceTexture(guiGraphics, texture, drawWidth, drawHeight, glowAlpha * 0.09f, color, true);
    }

    private void applyTextureFiltering(ResourceLocation texture) {
        if (texture == null) {
            return;
        }
        AbstractTexture abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(texture);
        if (abstractTexture != null) {
            abstractTexture.setFilter(configIconAntialiasing, false);
        }
    }

    private void drawAnchoredResourceTextureFrame(
        GuiGraphics guiGraphics,
        ResourceLocation texture,
        TextureFrame frame,
        float drawWidth,
        float drawHeight,
        float anchorXRatio,
        float anchorYRatio,
        float alpha,
        int rgb,
        boolean additive,
        boolean flipX
    ) {
        if (texture == null || frame == null) {
            return;
        }
        if (additive) {
            RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE
            );
        } else {
            RenderSystem.defaultBlendFunc();
        }
        float red = ((rgb >> 16) & 0xFF) / 255.0f;
        float green = ((rgb >> 8) & 0xFF) / 255.0f;
        float blue = (rgb & 0xFF) / 255.0f;
        float x0 = -drawWidth * anchorXRatio;
        float y0 = -drawHeight * anchorYRatio;
        float x1 = x0 + drawWidth;
        float y1 = y0 + drawHeight;
        float u0 = frame.u / (float) frame.totalWidth;
        float u1 = (frame.u + frame.width) / (float) frame.totalWidth;
        float v0 = frame.v / (float) frame.totalHeight;
        float v1 = (frame.v + frame.height) / (float) frame.totalHeight;
        if (flipX) {
            float swapped = u0;
            u0 = u1;
            u1 = swapped;
        }

        Matrix4f matrix = guiGraphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(red, green, blue, alpha);

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(matrix, x0, y1, 0.0f).uv(u0, v1).endVertex();
        builder.vertex(matrix, x1, y1, 0.0f).uv(u1, v1).endVertex();
        builder.vertex(matrix, x1, y0, 0.0f).uv(u1, v0).endVertex();
        builder.vertex(matrix, x0, y0, 0.0f).uv(u0, v0).endVertex();
        BufferUploader.drawWithShader(builder.end());
    }

    private void renderBarRing(
        GuiGraphics guiGraphics,
        String barTexturePath,
        float barWidth,
        float barHeight,
        float barTravel,
        float alpha,
        float barRotation,
        float barRadiusOffset,
        float barCenterXOffset,
        float barCenterYOffset,
        float[] layoutAngles,
        boolean accentTintEnabled,
        int accentColor,
        float brightness,
        float contrast,
        long elapsed
    ) {
        float distance = BAR_RING_RADIUS + barRadiusOffset + barTravel;
        float entryScale = resolveBarEntryScale(elapsed);

        for (float layoutAngle : layoutAngles) {
            guiGraphics.pose().pushPose();
            if (barCenterXOffset != 0.0f || barCenterYOffset != 0.0f) {
                guiGraphics.pose().translate(barCenterXOffset, barCenterYOffset, 0.0f);
            }
            guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(layoutAngle + barRotation));
            guiGraphics.pose().translate(0.0f, -distance, 0.0f);
            if (Math.abs(entryScale - 1.0f) > 0.001f) {
                guiGraphics.pose().scale(entryScale, entryScale, 1.0f);
            }

            drawColorizableTexture(
                guiGraphics,
                barTexturePath,
                barWidth,
                barHeight,
                alpha,
                0xFFFFFF,
                false,
                accentTintEnabled,
                accentColor,
                brightness,
                contrast
            );
            RenderSystem.defaultBlendFunc();
            guiGraphics.pose().popPose();
        }
    }

    private float resolveBarEntryScale(long elapsed) {
        long durationMs = Math.round(configBarEntryDuration * 1000.0f);
        if (durationMs <= 0L || configBarEntryInitialScale <= 1.0f) {
            return 1.0f;
        }
        if (elapsed >= durationMs) {
            return 1.0f;
        }
        float t = Mth.clamp((float) elapsed / (float) durationMs, 0.0f, 1.0f);
        float easedT = Mth.lerp(configBarEntryScaleCurve, t, easeOutCubic(t));
        return Mth.lerp(easedT, configBarEntryInitialScale, 1.0f);
    }

    private ResourceLocation getOrCreateProcessedTexture(
        String presetId,
        String texturePath,
        boolean accentTintEnabled,
        int accentColor,
        float brightness,
        float contrast
    ) {
        String cacheKey = String.format(
            Locale.ROOT,
            "%s:%s:%b:%06X:%.3f:%.3f",
            presetId,
            texturePath,
            accentTintEnabled,
            accentColor & 0xFFFFFF,
            brightness,
            contrast
        );
        ResourceLocation cached = PROCESSED_TEXTURE_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            byte[] bytes = ExternalTextureManager.readTextureBytes(presetId, texturePath);
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
            if (source == null) {
                return null;
            }

            BufferedImage processed = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);

            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int argb = source.getRGB(x, y);
                    processed.setRGB(x, y, processTexturePixel(argb, accentTintEnabled, accentColor, brightness, contrast));
                }
            }

            ResourceLocation processedTexture = registerBufferedTexture("valorant_fx", cacheKey, processed);
            if (processedTexture == null) {
                return null;
            }
            PROCESSED_TEXTURE_CACHE.put(cacheKey, processedTexture);
            return processedTexture;
        } catch (IOException ignored) {
            return null;
        }
    }

    private ResourceLocation registerBufferedTexture(String prefix, String cacheKey, BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        DynamicTexture texture = new DynamicTexture(NativeImage.read(new ByteArrayInputStream(output.toByteArray())));
        String dynamicName = "gd656killicon_" + prefix + "_" + Integer.toHexString(cacheKey.hashCode());
        return Minecraft.getInstance().getTextureManager().register(dynamicName, texture);
    }

    private boolean isAccentTintableTexture(String texturePath) {
        return texturePath != null && !texturePath.isEmpty();
    }

    private boolean needsTextureProcessing(String texturePath, boolean accentTintEnabled, float brightness, float contrast) {
        if (texturePath == null || texturePath.isEmpty()) {
            return false;
        }
        if (accentTintEnabled && isAccentTintableTexture(texturePath)) {
            return true;
        }
        return Math.abs(brightness - 1.0f) > 0.001f || Math.abs(contrast - 1.0f) > 0.001f;
    }

    private boolean isAccentPixel(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        if (alpha < 16) {
            return false;
        }

        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        float[] hsb = java.awt.Color.RGBtoHSB(red, green, blue, null);
        float saturation = hsb[1];
        float brightness = hsb[2];
        return brightness >= 0.18f
            && brightness <= 0.98f
            && saturation >= 0.18f;
    }

    private int processTexturePixel(int argb, boolean accentTintEnabled, int accentColor, float brightness, float contrast) {
        int alpha = (argb >>> 24) & 0xFF;
        if (alpha <= 0) {
            return 0;
        }

        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;

        if (accentTintEnabled && isAccentPixel(argb)) {
            float luminance = Math.max(red, Math.max(green, blue)) / 255.0f;
            red = Math.round(((accentColor >> 16) & 0xFF) * luminance);
            green = Math.round(((accentColor >> 8) & 0xFF) * luminance);
            blue = Math.round((accentColor & 0xFF) * luminance);
        }

        red = applyBrightnessContrastToChannel(red, brightness, contrast);
        green = applyBrightnessContrastToChannel(green, brightness, contrast);
        blue = applyBrightnessContrastToChannel(blue, brightness, contrast);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private int applyBrightnessContrastToRgb(int rgb, float brightness, float contrast) {
        int red = applyBrightnessContrastToChannel((rgb >> 16) & 0xFF, brightness, contrast);
        int green = applyBrightnessContrastToChannel((rgb >> 8) & 0xFF, brightness, contrast);
        int blue = applyBrightnessContrastToChannel(rgb & 0xFF, brightness, contrast);
        return (red << 16) | (green << 8) | blue;
    }

    private int applyBrightnessContrastToChannel(int channel, float brightness, float contrast) {
        float normalized = channel / 255.0f;
        float contrasted = ((normalized - 0.5f) * contrast) + 0.5f;
        float adjusted = contrasted * brightness;
        return Mth.clamp(Math.round(adjusted * 255.0f), 0, 255);
    }

    private static float easeOutCubic(float value) {
        float clamped = Mth.clamp(value, 0.0f, 1.0f);
        float inverted = 1.0f - clamped;
        return 1.0f - inverted * inverted * inverted;
    }

    private static float easeInQuart(float value) {
        float clamped = Mth.clamp(value, 0.0f, 1.0f);
        return clamped * clamped * clamped * clamped;
    }

    private static float easeOutQuad(float value) {
        float clamped = Mth.clamp(value, 0.0f, 1.0f);
        float inverted = 1.0f - clamped;
        return 1.0f - inverted * inverted;
    }

    private static float easeOutSoft(float value) {
        float quad = easeOutQuad(value);
        float cubic = easeOutCubic(value);
        return Mth.lerp(0.2f, quad, cubic);
    }

    private float resolveFrameBladeRotation(String styleId, long elapsed) {
        if (!ValorantStyleCatalog.usesBlade(styleId)) {
            return 0.0f;
        }
        long barDuration = comboCount >= 5 ? BAR_SPIN_DURATION_360_MS : BAR_SPIN_DURATION_180_MS;
        long rotationDuration = BAR_SPIN_START_MS + barDuration;
        float progress = (float) elapsed / (float) rotationDuration;
        progress = Mth.clamp(progress, 0.0f, 1.0f);
        return -360.0f * easeOutCubic(progress) * spinDirection;
    }

    private float resolveEffectiveBrightness(boolean gaiaSkin) {
        return gaiaSkin ? configBrightness * GAIA_DEFAULT_BRIGHTNESS : configBrightness;
    }

    private float resolveEffectiveContrast(boolean gaiaSkin) {
        return gaiaSkin ? configContrast * GAIA_DEFAULT_CONTRAST : configContrast;
    }

    private String resolveStyleId() {
        return ValorantStyleCatalog.resolveStyleId(ConfigManager.getCurrentPresetId(), currentConfig);
    }

    private boolean isGaiaSkin(String styleId, String iconTexturePath, String barTexturePath) {
        if (styleId != null && styleId.contains("gaia")) {
            return true;
        }
        return (iconTexturePath != null && iconTexturePath.contains("gaia"))
            || (barTexturePath != null && barTexturePath.contains("gaia"));
    }

    private boolean isEntryMotionFrameLocked(String styleId) {
        return ValorantStyleCatalog.useEmblemOnlyEntryMotion(styleId);
    }

    private record TextureMetrics(float width, float height, float maxDimension, boolean square) {
    }

    private record FlashState(int color, float alpha) {
        private static FlashState none() {
            return new FlashState(0xFFFFFF, 0.0f);
        }
    }

    private record HeadshotAnimState(float scale, int color) {
        private static HeadshotAnimState idle(int restColor) {
            return new HeadshotAnimState(1.0f, restColor);
        }
    }

    private HeadshotAnimState resolveHeadshotAnimState(long elapsed, int restColor) {
        long durationMs = Math.round(configHeadshotAnimDuration * 1000.0f);
        if (durationMs <= 0L || configHeadshotAnimInitialScale <= 1.0f) {
            return HeadshotAnimState.idle(restColor);
        }
        if (elapsed >= durationMs) {
            return HeadshotAnimState.idle(restColor);
        }

        float t = Mth.clamp((float) elapsed / (float) durationMs, 0.0f, 1.0f);
        float easedT = Mth.lerp(configHeadshotAnimScaleCurve, t, easeOutCubic(t));
        float scale = Mth.lerp(easedT, configHeadshotAnimInitialScale, 1.0f);

        float flickerFreq = configHeadshotAnimFlickerSpeed;
        int color;
        if (flickerFreq <= 0.0f) {
            color = restColor;
        } else {
            float phase = (float) Math.sin(elapsed * flickerFreq * Math.PI * 2.0 / 1000.0);
            boolean flashWhite = phase > 0.0f;
            float envelope = (1.0f - t) * (1.0f - t);
            color = flashWhite ? lerpColor(restColor, configHeadshotAnimFlickerColor, envelope) : restColor;
        }

        return new HeadshotAnimState(scale, color);
    }

    private static int lerpColor(int colorA, int colorB, float t) {
        int rA = (colorA >> 16) & 0xFF;
        int gA = (colorA >> 8) & 0xFF;
        int bA = colorA & 0xFF;
        int rB = (colorB >> 16) & 0xFF;
        int gB = (colorB >> 8) & 0xFF;
        int bB = colorB & 0xFF;
        int r = Math.round(Mth.lerp(t, rA, rB));
        int g = Math.round(Mth.lerp(t, gA, gB));
        int b = Math.round(Mth.lerp(t, bA, bB));
        return (r << 16) | (g << 8) | b;
    }
}
