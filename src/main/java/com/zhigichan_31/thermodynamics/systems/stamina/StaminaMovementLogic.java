package com.zhigichan_31.thermodynamics.systems.stamina;

import com.zhigichan_31.thermodynamics.data.PlayerData;
import com.zhigichan_31.thermodynamics.systems.ModConfig;
import com.zhigichan_31.thermodynamics.systems.WeightManager;
import net.minecraft.server.level.ServerPlayer;

public class StaminaMovementLogic {

    public static boolean handleMovementDrain(ServerPlayer player, PlayerData data) {
        // Железное условие: если флаг isSprinting() равен false или стамины уже нет — ничего не делаем
        if (!player.isSprinting() || data.getStamina() <= 0.0f) {
            return false;
        }

        // Берем базовый модификатор тяжести от WeightManager из главного пакета
        float rawWeightModifier = WeightManager.getStaminaModifier(player);

        // Считаем финальный множитель траты от веса брони/инвентаря на основе вашего конфига
        float finalWeightModifier = 1.0f + ((rawWeightModifier - 1.0f) * ModConfig.WEIGHT_SPRINT_DRAIN_IMPACT.get().floatValue());

        // Рассчитываем точное количество стамины за этот тик (например, 0.07 * множитель веса)
        float drainAmount = ModConfig.SPRINT_DRAIN.get().floatValue() * finalWeightModifier;

        // Вычитаем стамину через наш безопасный StaminaEngine, который лежит в этой же папке stamina
        return StaminaEngine.modifyStamina(player, data, drainAmount, false);
    }
}
