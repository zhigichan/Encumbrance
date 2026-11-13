package com.zhigichan.stamweight.stamina;

import com.zhigichan.stamweight.StamWeightConfig;
import com.zhigichan.stamweight.init.ModAttributes;
import net.minecraft.server.level.ServerPlayer;

public class StaminaCalculator {

    public static final double EXHAUSTION_RECOVERY_THRESHOLD = 6.0;

    public static int getRegenCooldownTicks() {
        return StamWeightConfig.REGEN_COOLDOWN.get();
    }

    public static double calculateDeltaPerTick(ServerPlayer player, boolean isSprinting, boolean isWalking) {
        double rate = player.getAttributeValue(ModAttributes.STAMINA_RATE);

        if (isSprinting) {
            return StamWeightConfig.SPRINT_DELTA.get();
        }

        if (isWalking) {
            double walkBase = StamWeightConfig.WALK_DELTA.get();
            double walkDelta = walkBase > 0.0 ? walkBase * rate : walkBase;
            if (StamWeightConfig.WALK_FLOOR_ZERO.get() && walkDelta < 0.0) {
                return 0.0;
            }
            return walkDelta;
        }

        return StamWeightConfig.IDLE_REGEN.get() * rate;
    }
}
