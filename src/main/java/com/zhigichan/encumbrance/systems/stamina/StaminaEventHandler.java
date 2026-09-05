package com.zhigichan.encumbrance.systems.stamina;

import com.zhigichan.encumbrance.Encumbrance;
import com.zhigichan.encumbrance.data.ModDataAttachments;
import com.zhigichan.encumbrance.data.PlayerData;
import com.zhigichan.encumbrance.network.NetworkHandler;
import com.zhigichan.encumbrance.systems.ModConfig;
import com.zhigichan.encumbrance.systems.WeightManager;
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

@EventBusSubscriber(modid = Encumbrance.MODID)
public class StaminaEventHandler {
    private static final ResourceLocation TWT_THIRST_ID = ResourceLocation.fromNamespaceAndPath("thirst", "player_thirst");
    private static final ResourceLocation EXHAUSTED_MINING_ID = ResourceLocation.fromNamespaceAndPath(Encumbrance.MODID, "exhausted_mining_penalty");
    private static final ResourceLocation EXHAUSTED_ATTACK_SPEED_ID = ResourceLocation.fromNamespaceAndPath(Encumbrance.MODID, "exhausted_attack_speed_penalty");

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
            if (mAt != null && mAt.hasModifier(EXHAUSTED_MINING_ID)) mAt.removeModifier(EXHAUSTED_MINING_ID);

            AttributeInstance aAt = serverPlayer.getAttribute(Attributes.ATTACK_SPEED);
            if (aAt != null && aAt.hasModifier(EXHAUSTED_ATTACK_SPEED_ID)) aAt.removeModifier(EXHAUSTED_ATTACK_SPEED_ID);

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
                if (!miningSpeedAttr.hasModifier(EXHAUSTED_MINING_ID)) {
                    // Используем addTransientModifier (или addOrReplacePermanentModifier, если NBT критичен)
                    miningSpeedAttr.addTransientModifier(new AttributeModifier(
                            EXHAUSTED_MINING_ID,
                            -ModConfig.EXHAUSTED_MINING_PENALTY.get(),
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    ));
                }
            } else if (miningSpeedAttr.hasModifier(EXHAUSTED_MINING_ID)) {
                miningSpeedAttr.removeModifier(EXHAUSTED_MINING_ID);
            }
        }

// ДЕБАФФ СКОРОСТИ АТАКИ ПРИ ИСТОЩЕНИИ
        AttributeInstance attackSpeedAttr = serverPlayer.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeedAttr != null) {
            if (data.isExhausted()) {
                if (!attackSpeedAttr.hasModifier(EXHAUSTED_ATTACK_SPEED_ID)) {
                    double penaltyValue = -Math.abs(ModConfig.EXHAUSTED_ATTACK_PENALTY.get());

                    attackSpeedAttr.addTransientModifier(new AttributeModifier(
                            EXHAUSTED_ATTACK_SPEED_ID,
                            penaltyValue,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    ));
                }
            } else if (attackSpeedAttr.hasModifier(EXHAUSTED_ATTACK_SPEED_ID)) {
                attackSpeedAttr.removeModifier(EXHAUSTED_ATTACK_SPEED_ID);
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
