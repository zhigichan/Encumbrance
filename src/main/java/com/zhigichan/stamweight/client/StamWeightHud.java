package com.zhigichan.stamweight.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zhigichan.stamweight.StamWeight;
import com.zhigichan.stamweight.init.ModAttributes;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

@EventBusSubscriber(modid = StamWeight.MODID, value = Dist.CLIENT)
public class StamWeightHud {

    private static final ResourceLocation HUD_ATLAS = ResourceLocation.fromNamespaceAndPath("stamweight", "textures/gui/hud_elements.png");
    private static final int ATLAS_WIDTH = 256;
    private static final int ATLAS_HEIGHT = 256;

    // Смещение холста (2, 112)
    private static final int OFFSET_U = 2;
    private static final int OFFSET_V = 112;

    // Координаты спрайтов
    private static final int BAG_U = OFFSET_U,      BAG_V = OFFSET_V,      BAG_W = 22, BAG_H = 14;
    private static final int STAM_BG_U = 27,        STAM_BG_V = OFFSET_V;
    private static final int STAM_FULL_U = 36,      STAM_FULL_V = OFFSET_V;
    private static final int STAM_HALF_U = 45,      STAM_HALF_V = OFFSET_V;

    private static final int SAT_FULL_U = 27,       SAT_FULL_V = OFFSET_V + 9;
    private static final int SAT_HALF_U = 36,       SAT_HALF_V = OFFSET_V + 9;

    private static final int HYDR_FULL_U = 27,      HYDR_FULL_V = OFFSET_V + 18;
    private static final int HYDR_HALF_U = 36,      HYDR_HALF_V = OFFSET_V + 18;

    private static final int ARMOR_FULL_U = 27,     ARMOR_FULL_V = OFFSET_V + 27;
    private static final int ARMOR_HALF_U = 36,     ARMOR_HALF_V = OFFSET_V + 27;

    private static final int ICON_SIZE = 9;

    private static float currentWeight = 0.0f;

    // Кэш рефлексии для Thirst Was Reclaimed
    private static Method cachedThirstMethod = null;
    private static Field cachedThirstField = null;
    private static boolean thirstLookupDone = false;

    public static void updateWeightFromServer(int compressedWeight) {
        currentWeight = compressedWeight / 10.0f;
    }

    public static float getCurrentWeight() {
        return currentWeight;
    }

    @SubscribeEvent
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.ARMOR_LEVEL)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderGuiLayerPost(RenderGuiLayerEvent.Post event) {
        if (event.getName().equals(VanillaGuiLayers.AIR_LEVEL)) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null || mc.options.hideGui || player.isCreative() || player.isSpectator()) {
                return;
            }

            GuiGraphics guiGraphics = event.getGuiGraphics();
            int rightX = guiGraphics.guiWidth() / 2 + 91;
            int baseY = guiGraphics.guiHeight() - 39;

            float quenched = getThirstQuenched(player);
            if (quenched > 0.0f) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                int hydrPoints = Mth.ceil(quenched);
                renderIconRowRight(guiGraphics, rightX, baseY - 10, hydrPoints, HYDR_FULL_U, HYDR_FULL_V, HYDR_HALF_U, HYDR_HALF_V);

                RenderSystem.disableBlend();
            }
        }
    }

    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || mc.options.hideGui || player.isCreative() || player.isSpectator()) {
            return;
        }

        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        int leftX = screenWidth / 2 - 91;
        int rightX = screenWidth / 2 + 91;
        int baseY = screenHeight - 39;

        float maxHealth = Math.max((float) player.getAttributeValue(Attributes.MAX_HEALTH), player.getHealth());
        int absorption = Mth.ceil(player.getAbsorptionAmount());
        int healthRows = Mth.ceil((maxHealth + absorption) / 2.0F / 10.0F);
        int rowHeight = Math.max(10 - (healthRows - 2), 3);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // =====================================================================
        // 1. БРОНЯ (поверх сердечек)
        // =====================================================================
        int armorValue = player.getArmorValue();
        if (armorValue > 0) {
            renderArmorRowLeft(guiGraphics, leftX, baseY, armorValue, rowHeight);
        }

        // =====================================================================
        // 2. ВЫНОСЛИВОСТЬ (над сердцами)
        // =====================================================================
        var playerData = player.getData(StamWeight.PLAYER_DATA);
        double currentStamina = playerData.getEndurance();
        double maxStamina = player.getAttributeValue(ModAttributes.MAX_STAMINA);
        boolean isFatigued = playerData.isFatigued();
        boolean isEmaciated = playerData.isEmaciated();

        int staminaY = baseY - (healthRows - 1) * rowHeight - 10;

        if (isEmaciated) {
            guiGraphics.setColor(0.6f, 0.4f, 0.4f, 1.0f);
        } else if (isFatigued) {
            guiGraphics.setColor(0.8f, 0.7f, 0.5f, 1.0f);
        }

        int curStamPoints = Mth.ceil(currentStamina);
        int maxStamPoints = Math.max(2, Mth.ceil(maxStamina));
        renderIconRowLeft(guiGraphics, leftX, staminaY, curStamPoints, maxStamPoints,
                STAM_FULL_U, STAM_FULL_V, STAM_HALF_U, STAM_HALF_V,
                true, STAM_BG_U, STAM_BG_V, 10);

        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        // =====================================================================
        // 3. СЫТОСТЬ (поверх окорочков голода)
        // =====================================================================
        float saturation = player.getFoodData().getSaturationLevel();
        if (saturation > 0.0f) {
            int satPoints = Mth.ceil(saturation);
            renderIconRowRight(guiGraphics, rightX, baseY, satPoints, SAT_FULL_U, SAT_FULL_V, SAT_HALF_U, SAT_HALF_V);
        }

        // =====================================================================
        // 4. СУМКА / ВЕС
        // =====================================================================
        float weight = getCurrentWeight();
        double maxWeight = player.getAttributeValue(ModAttributes.MAX_WEIGHT);
        float rawRatio = maxWeight > 0 ? (float) (weight / maxWeight) : 0.0f;
        float clampedRatio = Mth.clamp(rawRatio, 0.0f, 1.0f);

        int bagX = screenWidth / 2 + 95;
        int bagY = screenHeight - 21 - BAG_H;

        guiGraphics.setColor(0.35f, 0.35f, 0.35f, 0.85f);
        guiGraphics.blit(HUD_ATLAS, bagX, bagY, BAG_U, BAG_V, BAG_W, BAG_H, ATLAS_WIDTH, ATLAS_HEIGHT);
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        if (clampedRatio > 0.0f) {
            int filledHeight = Mth.ceil(BAG_H * clampedRatio);
            int yOffset = BAG_H - filledHeight;
            guiGraphics.blit(
                    HUD_ATLAS,
                    bagX, bagY + yOffset,
                    BAG_U, BAG_V + yOffset,
                    BAG_W, filledHeight,
                    ATLAS_WIDTH, ATLAS_HEIGHT
            );
        }

        RenderSystem.disableBlend();

        // =====================================================================
        // 5. ТЕКСТ ВЕСА (чётко по центру сумки + цвета по порогам заполненности)
        // =====================================================================
        String weightText = String.format(Locale.US, "%.1f", weight);

        // До 75% — белый, от 75% до 100% — жёлтый, после 100% — фиолетовый
        int textColor;
        if (rawRatio > 1.0f) {
            textColor = 0xFF55FF; // Фиолетовый (перегруз > 100%)
        } else if (rawRatio >= 0.75f) {
            textColor = 0xFFFF55; // Жёлтый (75% - 100%)
        } else {
            textColor = 0xFFFFFF; // Белый (< 75%)
        }

        float textScale = 0.70f;
        int textWidth = mc.font.width(weightText);
        float scaledTextHeight = mc.font.lineHeight * textScale;

        guiGraphics.pose().pushPose();
        // Точное центрирование по X и Y внутри 21x16 сумки
        float centerX = bagX + (BAG_W / 2.0f);
        float centerY = bagY + (BAG_H - scaledTextHeight) / 2.0f + 0.5f;
        guiGraphics.pose().translate(centerX, centerY, 100.0f);
        guiGraphics.pose().scale(textScale, textScale, 1.0f);
        guiGraphics.drawString(mc.font, weightText, -textWidth / 2, 0, textColor, true);
        guiGraphics.pose().popPose();
    }

    private static void renderArmorRowLeft(GuiGraphics guiGraphics, int startX, int startY, int currentPoints, int verticalStep) {
        int totalIcons = Mth.ceil(currentPoints / 2.0f);

        for (int i = 0; i < totalIcons; i++) {
            int row = i / 10;
            int col = i % 10;
            int x = startX + col * 8;
            int y = startY - row * verticalStep;

            int pointThreshold = i * 2 + 1;
            if (currentPoints > pointThreshold) {
                guiGraphics.blit(HUD_ATLAS, x, y, ARMOR_FULL_U, ARMOR_FULL_V, ICON_SIZE, ICON_SIZE, ATLAS_WIDTH, ATLAS_HEIGHT);
            } else if (currentPoints == pointThreshold) {
                guiGraphics.blit(HUD_ATLAS, x, y, ARMOR_HALF_U, ARMOR_HALF_V, 5, ICON_SIZE, ATLAS_WIDTH, ATLAS_HEIGHT);
            }
        }
    }
    private static void renderIconRowLeft(GuiGraphics guiGraphics, int startX, int startY,
                                          int currentPoints, int maxPoints,
                                          int fullU, int fullV, int halfU, int halfV,
                                          boolean drawBg, int bgU, int bgV, int verticalStep) {
        int totalIcons = Mth.ceil(maxPoints / 2.0f);
        for (int i = 0; i < totalIcons; i++) {
            int row = i / 10;
            int col = i % 10;
            int x = startX + col * 8;
            int y = startY - row * verticalStep;
            if (drawBg) {
                guiGraphics.blit(HUD_ATLAS, x, y, bgU, bgV, ICON_SIZE, ICON_SIZE, ATLAS_WIDTH, ATLAS_HEIGHT);
            }
            int pointThreshold = i * 2 + 1;
            if (currentPoints > pointThreshold) {
                guiGraphics.blit(HUD_ATLAS, x, y, fullU, fullV, ICON_SIZE, ICON_SIZE, ATLAS_WIDTH, ATLAS_HEIGHT);
            } else if (currentPoints == pointThreshold) {
                guiGraphics.blit(HUD_ATLAS, x, y, halfU, halfV, ICON_SIZE, ICON_SIZE, ATLAS_WIDTH, ATLAS_HEIGHT);
            }
        }
    }
    private static void renderIconRowRight(GuiGraphics guiGraphics, int rightEdgeX, int y,
                                           int currentPoints, int fullU, int fullV, int halfU, int halfV) {
        for (int i = 0; i < 10; i++) {
            int x = rightEdgeX - i * 8 - ICON_SIZE;
            int pointThreshold = i * 2 + 1;
            if (currentPoints > pointThreshold) {
                guiGraphics.blit(HUD_ATLAS, x, y, fullU, fullV, ICON_SIZE, ICON_SIZE, ATLAS_WIDTH, ATLAS_HEIGHT);
            } else if (currentPoints == pointThreshold) {
                guiGraphics.blit(HUD_ATLAS, x, y, halfU, halfV, ICON_SIZE, ICON_SIZE, ATLAS_WIDTH, ATLAS_HEIGHT);
            }
        }
    }
    private static float getThirstQuenched(Player player) {
        try {
            AttachmentType<?> thirstType = NeoForgeRegistries.ATTACHMENT_TYPES.get(
                    ResourceLocation.fromNamespaceAndPath("thirst", "player_thirst")
            );
            if (thirstType == null) {
                return 0.0f;
            }
            Object thirstData = player.getData(thirstType);
            if (thirstData == null) {
                return 0.0f;
            }
            if (!thirstLookupDone) {
                thirstLookupDone = true;
                Class<?> clazz = thirstData.getClass();
                for (String methodName : new String[]{"getQuenched", "getHydration", "quenched"}) {
                    try {
                        Method m = clazz.getMethod(methodName);
                        m.setAccessible(true);
                        cachedThirstMethod = m;
                        break;
                    } catch (NoSuchMethodException ignored) {}
                }
                if (cachedThirstMethod == null) {
                    for (String fieldName : new String[]{"quenched", "hydration"}) {
                        try {
                            Field f = clazz.getDeclaredField(fieldName);
                            f.setAccessible(true);
                            cachedThirstField = f;
                            break;
                        } catch (NoSuchFieldException ignored) {}
                    }
                }
            }
            Object val = null;
            if (cachedThirstMethod != null) {
                val = cachedThirstMethod.invoke(thirstData);
            } else if (cachedThirstField != null) {
                val = cachedThirstField.get(thirstData);
            }
            if (val instanceof Number n) {
                return n.floatValue();
            }
        } catch (Exception ignored) {
        }
        return 0.0f;
    }
}