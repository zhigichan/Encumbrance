package com.zhigichan.stamweight.link;

import com.zhigichan.stamweight.StamWeight;
import com.zhigichan.stamweight.StamWeightConfig;
import com.zhigichan.stamweight.StamWeightData;
import com.zhigichan.stamweight.init.ModAttributes;
import com.zhigichan.stamweight.stamina.StaminaStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.lang.reflect.Method;

public class WeightStaminaLink {

    public static boolean USE_DYNAMIC_SCALING = true;
    public static boolean EMACIATION_EXTRA_DEBUFF = false;

    public static int SURVIVAL_CHECK_INTERVAL = 20;

    public static final float OVERLOAD_START_PERCENT = 100.0f;
    public static final float STEP_PERCENT = 25.0f;

    public static final double FATIGUE_MINING_PENALTY = -0.50;
    public static final double FATIGUE_ATTACK_RECHARGE_PENALTY = -0.50;

    public static final double EMACIATION_MINING_PENALTY = -0.75;
    public static final double EMACIATION_ATTACK_RECHARGE_PENALTY = -0.75;

    public static final int SURVIVAL_THRESHOLD_LEVEL = 6;

    public static final double MIN_MAX_STAMINA = 2.0;
    public static final double EMACIATION_MAX_THRESHOLD = 6.0;
    public static final double MAX_SPEED_REDUCTION = -0.85;

    private static final ResourceLocation OVERLOAD_MAX_STAMINA_ID =
            ResourceLocation.fromNamespaceAndPath(StamWeight.MODID, "overload_max_stamina");
    private static final ResourceLocation SURVIVAL_MAX_STAMINA_ID =
            ResourceLocation.fromNamespaceAndPath(StamWeight.MODID, "survival_max_stamina");

    private static final ResourceLocation SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(StamWeight.MODID, "overload_speed");
    private static final ResourceLocation GRAVITY_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(StamWeight.MODID, "overload_gravity");
    private static final ResourceLocation STAMINA_RATE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(StamWeight.MODID, "overload_stamina_rate");

    private static final ResourceLocation FATIGUE_MINING_ID =
            ResourceLocation.fromNamespaceAndPath(StamWeight.MODID, "fatigue_mining_speed");
    private static final ResourceLocation FATIGUE_ATTACK_ID =
            ResourceLocation.fromNamespaceAndPath(StamWeight.MODID, "fatigue_attack_recharge");

    private static final ResourceLocation SURVIVAL_RATE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(StamWeight.MODID, "survival_stamina_rate");

    private static final ResourceLocation TWT_THIRST_ID =
            ResourceLocation.fromNamespaceAndPath("thirst", "player_thirst");
    private static Method cachedGetThirstMethod = null;
    private static boolean thirstLookupFailed = false;

    public static void onWeightUpdated(ServerPlayer player, float currentWeight) {
        float maxWeight = (float) Math.max(1.0, player.getAttributeValue(ModAttributes.MAX_WEIGHT));
        float weightPercent = (currentWeight / maxWeight) * 100.0f;
        double overloadRatio = calculateOverloadRatio(weightPercent);

        updateOverloadMaxStaminaModifier(player, overloadRatio);
        syncBarrelAndCheckEmaciation(player);
        updateOverloadAttributes(player, overloadRatio);
    }

    public static void tickSurvivalCheck(ServerPlayer player) {
        if (SURVIVAL_CHECK_INTERVAL > 1) {
            if ((player.getId() % SURVIVAL_CHECK_INTERVAL) != (player.tickCount % SURVIVAL_CHECK_INTERVAL)) {
                return;
            }
        }

        int currentFood = player.getFoodData().getFoodLevel();
        int currentThirst = getPlayerThirst(player);

        StamWeightData data = player.getData(StamWeight.PLAYER_DATA);
        if (currentFood == data.getLastFoodLevel() && currentThirst == data.getLastThirstLevel()) {
            return;
        }

        data.setLastFoodLevel(currentFood);
        data.setLastThirstLevel(currentThirst);

        applySurvivalPenalties(player, currentFood, currentThirst);
    }

    public static void updateFatigueDebuffs(ServerPlayer player) {
        boolean isEmaciated = StaminaStorage.isEmaciated(player);
        boolean isFatigued = StaminaStorage.isFatigued(player);

        AttributeInstance miningAttr = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        AttributeInstance attackRechargeAttr = player.getAttribute(ModAttributes.ATTACK_RECHARGE_SPEED);

        if (!isEmaciated && !isFatigued) {
            if (miningAttr != null) miningAttr.removeModifier(FATIGUE_MINING_ID);
            if (attackRechargeAttr != null) attackRechargeAttr.removeModifier(FATIGUE_ATTACK_ID);
            return;
        }

        boolean useStrongPenalty = isEmaciated && EMACIATION_EXTRA_DEBUFF;
        double miningPenalty = useStrongPenalty ? EMACIATION_MINING_PENALTY : FATIGUE_MINING_PENALTY;
        double attackPenalty = useStrongPenalty ? EMACIATION_ATTACK_RECHARGE_PENALTY : FATIGUE_ATTACK_RECHARGE_PENALTY;

        applyPercentModifierIfChanged(miningAttr, FATIGUE_MINING_ID, miningPenalty);
        applyPercentModifierIfChanged(attackRechargeAttr, FATIGUE_ATTACK_ID, attackPenalty);
    }

    private static void applySurvivalPenalties(ServerPlayer player, int food, int thirst) {
        double rateFlatDelta = 0.0;
        double maxBarrelPenalty = 0.0;

        double cfgRegenDelta = StamWeightConfig.SURVIVAL_REGEN_DELTA.get(); // -0.15
        double cfgStaminaPerPoint = StamWeightConfig.SURVIVAL_STAMINA_PER_POINT.get(); // -0.2

        if (food <= SURVIVAL_THRESHOLD_LEVEL) {
            rateFlatDelta += cfgRegenDelta;
            if (food < SURVIVAL_THRESHOLD_LEVEL) {
                maxBarrelPenalty += (SURVIVAL_THRESHOLD_LEVEL - Math.max(0, food)) * cfgStaminaPerPoint;
            }
        }

        if (thirst >= 0 && thirst <= SURVIVAL_THRESHOLD_LEVEL) {
            rateFlatDelta += cfgRegenDelta;
            if (thirst < SURVIVAL_THRESHOLD_LEVEL) {
                maxBarrelPenalty += (SURVIVAL_THRESHOLD_LEVEL - Math.max(0, thirst)) * cfgStaminaPerPoint;
            }
        }

        AttributeInstance rateAttr = player.getAttribute(ModAttributes.STAMINA_RATE);
        if (rateAttr != null) {
            if (rateFlatDelta == 0.0) {
                rateAttr.removeModifier(SURVIVAL_RATE_MODIFIER_ID);
            } else {
                applyFlatModifierIfChanged(rateAttr, SURVIVAL_RATE_MODIFIER_ID, rateFlatDelta);
            }
        }

        AttributeInstance maxStaminaAttr = player.getAttribute(ModAttributes.MAX_STAMINA);
        if (maxStaminaAttr != null) {
            if (maxBarrelPenalty >= 0.0) {
                maxStaminaAttr.removeModifier(SURVIVAL_MAX_STAMINA_ID);
            } else {
                applyFlatModifierIfChanged(maxStaminaAttr, SURVIVAL_MAX_STAMINA_ID, maxBarrelPenalty);
            }
        }

        syncBarrelAndCheckEmaciation(player);
    }

    private static void updateOverloadAttributes(ServerPlayer player, double overloadRatio) {
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance gravityAttr = player.getAttribute(Attributes.GRAVITY);
        AttributeInstance staminaRateAttr = player.getAttribute(ModAttributes.STAMINA_RATE);

        if (overloadRatio <= 0.0) {
            if (speedAttr != null) speedAttr.removeModifier(SPEED_MODIFIER_ID);
            if (gravityAttr != null) gravityAttr.removeModifier(GRAVITY_MODIFIER_ID);
            if (staminaRateAttr != null) staminaRateAttr.removeModifier(STAMINA_RATE_MODIFIER_ID);
            return;
        }

        double speedDelta = Math.max(MAX_SPEED_REDUCTION, overloadRatio * -StamWeightConfig.OVERLOAD_SPEED_PERCENT.get());
        double gravityDelta = overloadRatio * StamWeightConfig.OVERLOAD_GRAVITY_PERCENT.get();
        double staminaRateFlatDelta = overloadRatio * StamWeightConfig.OVERLOAD_REGEN_DELTA.get(); // -0.25

        applyPercentModifierIfChanged(speedAttr, SPEED_MODIFIER_ID, speedDelta);
        applyPercentModifierIfChanged(gravityAttr, GRAVITY_MODIFIER_ID, gravityDelta);
        applyFlatModifierIfChanged(staminaRateAttr, STAMINA_RATE_MODIFIER_ID, staminaRateFlatDelta);
    }

    private static void updateOverloadMaxStaminaModifier(ServerPlayer player, double overloadRatio) {
        AttributeInstance maxStaminaAttr = player.getAttribute(ModAttributes.MAX_STAMINA);
        if (maxStaminaAttr == null) return;

        if (overloadRatio <= 0.0) {
            maxStaminaAttr.removeModifier(OVERLOAD_MAX_STAMINA_ID);
            return;
        }

        double rawPenalty = overloadRatio * StamWeightConfig.OVERLOAD_STAMINA_PENALTY.get();
        double penalty = Math.round(rawPenalty * 10.0) / 10.0;
        applyFlatModifierIfChanged(maxStaminaAttr, OVERLOAD_MAX_STAMINA_ID, penalty);
    }

    private static double calculateOverloadRatio(float weightPercent) {
        if (weightPercent <= OVERLOAD_START_PERCENT) {
            return 0.0;
        }
        float excessPercent = weightPercent - OVERLOAD_START_PERCENT;
        if (USE_DYNAMIC_SCALING) {
            return excessPercent / 100.0;
        } else {
            int steps = (int) Math.floor(excessPercent / STEP_PERCENT);
            return (steps * STEP_PERCENT) / 100.0;
        }
    }

    private static void syncBarrelAndCheckEmaciation(ServerPlayer player) {
        double newMaxStamina = StaminaStorage.getMaxStamina(player);
        player.getData(StamWeight.PLAYER_DATA).setMaxEndurance(newMaxStamina);

        double currentStamina = StaminaStorage.getStamina(player);
        if (currentStamina > newMaxStamina) {
            currentStamina = newMaxStamina;
            StaminaStorage.setStamina(player, currentStamina);
        }

        boolean wasEmaciated = StaminaStorage.isEmaciated(player);
        boolean isEmaciatedNow = newMaxStamina <= EMACIATION_MAX_THRESHOLD;
        StaminaStorage.setEmaciated(player, isEmaciatedNow);

        if (isEmaciatedNow) {
            StaminaStorage.setFatigued(player, false);
        }

        if (isEmaciatedNow != wasEmaciated) {
            updateFatigueDebuffs(player);
        }

        StaminaStorage.syncIfChanged(player, currentStamina, newMaxStamina);
    }

    private static void applyPercentModifierIfChanged(AttributeInstance attr, ResourceLocation id, double newDelta) {
        if (attr == null) return;
        double roundedDelta = Math.round(newDelta * 10000.0) / 10000.0;
        AttributeModifier existing = attr.getModifier(id);
        if (existing != null && existing.amount() == roundedDelta) {
            return;
        }
        attr.addOrReplacePermanentModifier(new AttributeModifier(
                id,
                roundedDelta,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
    }
    private static void applyFlatModifierIfChanged(AttributeInstance attr, ResourceLocation id, double newDelta) {
        if (attr == null) return;
        double roundedDelta = Math.round(newDelta * 100.0) / 100.0;
        AttributeModifier existing = attr.getModifier(id);
        if (existing != null && existing.amount() == roundedDelta) {
            return;
        }
        attr.addOrReplacePermanentModifier(new AttributeModifier(
                id,
                roundedDelta,
                AttributeModifier.Operation.ADD_VALUE
        ));
    }
    private static int getPlayerThirst(ServerPlayer player) {
        if (thirstLookupFailed) {
            return -1;
        }
        try {
            AttachmentType<?> thirstType = NeoForgeRegistries.ATTACHMENT_TYPES.get(TWT_THIRST_ID);
            if (thirstType == null || !player.hasData(thirstType)) {
                return -1;
            }
            Object thirstCap = player.getData(thirstType);
            if (cachedGetThirstMethod == null) {
                cachedGetThirstMethod = thirstCap.getClass().getMethod("getThirst");
            }
            Object result = cachedGetThirstMethod.invoke(thirstCap);
            if (result instanceof Number num) {
                return num.intValue();
            }
        } catch (Exception e) {
            thirstLookupFailed = true;
        }
        return -1;
    }
}