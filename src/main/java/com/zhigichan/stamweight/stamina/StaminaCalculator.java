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
        // База атрибута = 1.0, поэтому (rate - 1.0) — это чистая сумма плоских дебаффов веса, голода и жажды
        double flatDebuffs = rate - 1.0;

        double baseAction;
        if (isSprinting) {
            baseAction = StamWeightConfig.SPRINT_DELTA.get();
        } else if (isWalking) {
            baseAction = StamWeightConfig.WALK_DELTA.get();
        } else {
            baseAction = StamWeightConfig.IDLE_REGEN.get();
        }

        double totalDelta = baseAction + flatDebuffs;

        if (isWalking && !isSprinting && StamWeightConfig.WALK_FLOOR_ZERO.get() && totalDelta < 0.0) {
            return 0.0;
        }

        return totalDelta;
    }
}
