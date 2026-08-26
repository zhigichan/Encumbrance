package com.zhigichan_31.thermodynamics.systems.stamina;

import com.zhigichan_31.thermodynamics.Thermodynamics;
import com.zhigichan_31.thermodynamics.data.ModDataAttachments;
import com.zhigichan_31.thermodynamics.data.PlayerData;
import com.zhigichan_31.thermodynamics.network.NetworkHandler;
import com.zhigichan_31.thermodynamics.systems.ModConfig;
import com.zhigichan_31.thermodynamics.systems.WeightManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Thermodynamics.MODID)
public class StaminaEventHandler {
    private static final ResourceLocation TWT_THIRST_ID = ResourceLocation.fromNamespaceAndPath("thirst", "player_thirst");
    private static final ResourceLocation EXHAUSTED_MINING_UUID = ResourceLocation.fromNamespaceAndPath(Thermodynamics.MODID, "exhausted_mining_penalty");
    private static final ResourceLocation EXHAUSTED_ATTACK_SPEED_UUID = ResourceLocation.fromNamespaceAndPath(Thermodynamics.MODID, "exhausted_attack_speed_penalty");

    @SubscribeEvent
    public static void onServerPlayerTick(PlayerTickEvent.Post event) { // Твой родной и стабильный Post-тик
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return;

        PlayerData data = player.getData(ModDataAttachments.PLAYER_DATA.get());
        java.util.UUID uuid = player.getUUID();
        boolean isChanged = false;

        // ОЧИСТКА ДЛЯ КРЕАТИВА И НАБЛЮДАТЕЛЯ
        if (serverPlayer.isCreative() || serverPlayer.isSpectator()) {
            AttributeInstance mAt = serverPlayer.getAttribute(Attributes.BLOCK_BREAK_SPEED);
            if (mAt != null && mAt.hasModifier(EXHAUSTED_MINING_UUID)) mAt.removeModifier(EXHAUSTED_MINING_UUID);

            AttributeInstance aAt = serverPlayer.getAttribute(Attributes.ATTACK_SPEED);
            if (aAt != null && aAt.hasModifier(EXHAUSTED_ATTACK_SPEED_UUID)) aAt.removeModifier(EXHAUSTED_ATTACK_SPEED_UUID);

            StaminaEngine.clearPlayerData(uuid);
            if (data.isExhausted() || data.getStamina() < StaminaEngine.MAX_STAMINA) {
                data.setExhausted(false);
                data.setStamina(StaminaEngine.MAX_STAMINA);
                NetworkHandler.sendToPlayer(serverPlayer);
            }
            return;
        }

        int cooldown = StaminaEngine.regenCooldowns.getOrDefault(uuid, 0);
        if (cooldown > 0) {
            cooldown--;
            StaminaEngine.regenCooldowns.put(uuid, cooldown);
        }

        // ЖЕЛЕЗНОЕ УСЛОВИЕ БЕГА (Работает идеально, без багов)
        boolean isSprintingNow = player.isSprinting() && data.getStamina() > 0.0f;

        // ПРИОРИТЕТ: РЕГЕНЕРАЦИЯ СВЕРХУ (Работает всегда, когда игрок НЕ бежит)
        if (!isSprintingNow && data.getStamina() < StaminaEngine.MAX_STAMINA && cooldown <= 0) {
            float statusModifier = 1.0f, penalty = ModConfig.HUNGER_THIRST_PENALTY.get().floatValue();
            try {
                var thirstType = net.neoforged.neoforge.registries.NeoForgeRegistries.ATTACHMENT_TYPES.get(TWT_THIRST_ID);
                if (thirstType != null && serverPlayer.hasData(thirstType)) {
                    Object thirstCap = serverPlayer.getData(thirstType);
                    if (((Number) thirstCap.getClass().getMethod("getThirst").invoke(thirstCap)).floatValue() < 6.0f) statusModifier -= penalty;
                }
            } catch (Exception ignored) {}
            if (serverPlayer.getFoodData().getFoodLevel() < 6) statusModifier -= penalty;
            statusModifier = Math.max(0.0f, statusModifier);

            float rawWeightModifier = WeightManager.getStaminaModifier(player);
            float finalWeightModifier = 1.0f + ((rawWeightModifier - 1.0f) * ModConfig.WEIGHT_REGEN_PENALTY_IMPACT.get().floatValue());

            // Регенерация идёт в полную силу (movementFactor убран, конвейеры теперь безопасны)
            float regenAmount = (ModConfig.REGEN_BASE.get().floatValue() * statusModifier) / finalWeightModifier;

            if (StaminaEngine.modifyStamina(serverPlayer, data, regenAmount, true)) isChanged = true;
        }

        // ПОРОГИ ИСТОЩЕНИЯ
        float recoveryAt = ModConfig.STAMINA_RECOVERY_THRESHOLD.get().floatValue();
        if (data.getStamina() <= 0.0f && !data.isExhausted()) {
            data.setExhausted(true);
            isChanged = true;
            if (player.isSprinting()) player.setSprinting(false);
        }
        if (data.isExhausted() && data.getStamina() >= recoveryAt) {
            data.setExhausted(false);
            isChanged = true;
        }

        // ДЕБАФФ СКОРОСТИ КОПАНИЯ ПРИ ИСТОЩЕНИИ
        AttributeInstance miningSpeedAttr = serverPlayer.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        if (miningSpeedAttr != null) {
            if (data.isExhausted()) {
                if (!miningSpeedAttr.hasModifier(EXHAUSTED_MINING_UUID)) {
                    miningSpeedAttr.addPermanentModifier(new AttributeModifier(EXHAUSTED_MINING_UUID, -ModConfig.EXHAUSTED_MINING_PENALTY.get(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
            } else if (miningSpeedAttr.hasModifier(EXHAUSTED_MINING_UUID)) {
                miningSpeedAttr.removeModifier(EXHAUSTED_MINING_UUID);
            }
        }

        // ДЕБАФФ СКОРОСТИ АТАКИ ПРИ ИСТОЩЕНИИ (ФИКС СКОРОСТИ АТАКИ)
        AttributeInstance attackSpeedAttr = serverPlayer.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeedAttr != null) {
            if (data.isExhausted()) {
                if (!attackSpeedAttr.hasModifier(EXHAUSTED_ATTACK_SPEED_UUID)) {
                    // Берёт 0.5 из твоего конфига и делает отрицательным (-0.5), замедляя атаку на 50%
                    float penaltyValue = -Math.abs(ModConfig.EXHAUSTED_ATTACK_PENALTY.get().floatValue());
                    attackSpeedAttr.addPermanentModifier(new AttributeModifier(EXHAUSTED_ATTACK_SPEED_UUID, penaltyValue, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
            } else if (attackSpeedAttr.hasModifier(EXHAUSTED_ATTACK_SPEED_UUID)) {
                attackSpeedAttr.removeModifier(EXHAUSTED_ATTACK_SPEED_UUID);
            }
        }

        // ТРАТА СТАМИНЫ СНИЗУ
        if (isSprintingNow) {
            if (StaminaMovementLogic.handleMovementDrain(serverPlayer, data)) isChanged = true;
        }
        if (StaminaActionsLogic.handleMiningDrain(serverPlayer, data)) isChanged = true;

        if (isChanged) NetworkHandler.sendToPlayer(serverPlayer);
    }

    @SubscribeEvent
    public static void onEntityAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && !serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
            PlayerData data = serverPlayer.getData(ModDataAttachments.PLAYER_DATA.get());
            StaminaActionsLogic.processServerSwing(serverPlayer, data);
            NetworkHandler.sendToPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer serverPlayer && !serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
            StaminaEngine.playersThrowingItem.add(serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public static void onMiningTickUpdate(PlayerEvent.BreakSpeed event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && event.getPosition().isPresent() && !serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
            StaminaEngine.activeMiningPositions.put(serverPlayer.getUUID(), event.getPosition().get());
        }
    }
}
