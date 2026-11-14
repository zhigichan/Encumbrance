package com.zhigichan.stamweight.stamina;

import com.zhigichan.stamweight.StamWeight;
import com.zhigichan.stamweight.link.WeightStaminaLink;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = StamWeight.MODID)
public class StaminaCore {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        boolean isSprinting = player.isSprinting();
        boolean isWalking = StaminaStorage.isWalking(player);

        double maxStamina = StaminaStorage.getMaxStamina(player);
        double currentStamina = StaminaStorage.getStamina(player);
        int cooldown = StaminaStorage.getCooldown(player);

        double deltaPerTick = StaminaCalculator.calculateDeltaPerTick(player, isSprinting, isWalking);

        // Обновляем состояние Усталости для дебаффов копания и перезарядки (если нет Измождения)
        if (!StaminaStorage.isEmaciated(player)) {
            updateFatigueState(player, currentStamina);
        }

        if (currentStamina >= maxStamina && cooldown <= 0 && deltaPerTick >= 0) {
            return;
        }

        if (deltaPerTick < 0) {
            currentStamina = Math.max(0.0, currentStamina + deltaPerTick);
            cooldown = StaminaCalculator.getRegenCooldownTicks();
        } else if (deltaPerTick > 0) {
            if (cooldown > 0) {
                cooldown--;
            } else if (currentStamina < maxStamina) {
                currentStamina = Math.min(maxStamina, currentStamina + deltaPerTick);
            }
        }

        StaminaStorage.setStamina(player, currentStamina);
        StaminaStorage.setCooldown(player, cooldown);
        StaminaStorage.syncIfChanged(player, currentStamina, maxStamina);
    }

    private static void updateFatigueState(ServerPlayer player, double currentStamina) {
        boolean isFatigued = StaminaStorage.isFatigued(player);

        if (!isFatigued && currentStamina <= 0.0) {
            StaminaStorage.setFatigued(player, true);
            WeightStaminaLink.updateFatigueDebuffs(player);
        } else if (isFatigued && currentStamina >= StaminaCalculator.EXHAUSTION_RECOVERY_THRESHOLD) {
            StaminaStorage.setFatigued(player, false);
            WeightStaminaLink.updateFatigueDebuffs(player);
        }
    }
}
