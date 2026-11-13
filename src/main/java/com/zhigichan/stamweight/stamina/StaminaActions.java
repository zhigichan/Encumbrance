package com.zhigichan.stamweight.stamina;

import com.zhigichan.stamweight.StamWeight;
import com.zhigichan.stamweight.StamWeightConfig;
import com.zhigichan.stamweight.StamWeightData;
import com.zhigichan.stamweight.init.ModAttributes;
import com.zhigichan.stamweight.link.WeightStaminaLink;
import com.zhigichan.stamweight.network.StaminaSyncPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.Map;

public class StaminaActions {

    public static final double MIN_SWING_MULTIPLIER = 0.5;

    public static final Map<ResourceLocation, Double> ITEM_ID_COSTS = new LinkedHashMap<>();
    public static final Map<TagKey<Item>, Double> ITEM_TAG_COSTS = new LinkedHashMap<>();

    public static void onPlayerAttackSwing(ServerPlayer player, float chargeRatio) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        double baseCost = getWeaponStaminaCost(player.getMainHandItem());
        double swingMultiplier = Math.max(MIN_SWING_MULTIPLIER, chargeRatio);
        double finalCost = baseCost * swingMultiplier;

        consumeStamina(player, finalCost);
    }

    public static void onPlayerMiningTick(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        consumeStamina(player, StamWeightConfig.MINING_COST.get());
    }

    public static double getWeaponStaminaCost(ItemStack stack) {
        if (stack.isEmpty()) {
            return StamWeightConfig.SWING_COST.get();
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Double idCost = ITEM_ID_COSTS.get(itemId);
        if (idCost != null) {
            return idCost;
        }

        for (Map.Entry<TagKey<Item>, Double> entry : ITEM_TAG_COSTS.entrySet()) {
            if (stack.is(entry.getKey())) {
                return entry.getValue();
            }
        }

        return StamWeightConfig.SWING_COST.get();
    }

    public static void consumeStamina(ServerPlayer player, double amount) {
        if (amount <= 0.0) return;

        StamWeightData data = player.getData(StamWeight.PLAYER_DATA);

        double maxStamina = Math.max(WeightStaminaLink.MIN_MAX_STAMINA, player.getAttributeValue(ModAttributes.MAX_STAMINA));
        double currentStamina = Math.max(0.0, data.getEndurance() - amount);

        data.setEndurance(currentStamina);
        data.setEnduranceCooldown(StaminaCalculator.getRegenCooldownTicks());

        if (currentStamina <= 0.0 && !data.isEmaciated() && !data.isFatigued()) {
            data.setFatigued(true);
            WeightStaminaLink.updateFatigueDebuffs(player);
        }

        int curScaled = (int) Math.round(currentStamina * 10.0);
        int maxScaled = (int) Math.round(maxStamina * 10.0);
        if (curScaled != data.getLastSyncedCur() || maxScaled != data.getLastSyncedMax()) {
            data.setLastSyncedCur(curScaled);
            data.setLastSyncedMax(maxScaled);
            PacketDistributor.sendToPlayer(player, StaminaSyncPacket.fromValues(currentStamina, maxStamina));
        }
    }
}
