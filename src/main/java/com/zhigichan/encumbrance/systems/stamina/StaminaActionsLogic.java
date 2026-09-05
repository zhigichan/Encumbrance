package com.zhigichan.encumbrance.systems.stamina;

import com.zhigichan.encumbrance.data.ModDataAttachments;
import com.zhigichan.encumbrance.data.PlayerData;
import com.zhigichan.encumbrance.systems.ModConfig;
import com.zhigichan.encumbrance.systems.ConfigParser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public class StaminaActionsLogic {

    public static boolean handleMiningDrain(ServerPlayer player, PlayerData data) {
        UUID uuid = player.getUUID();
        if (StaminaEngine.activeMiningPositions.containsKey(uuid)) {
            float cost = ModConfig.BLOCK_BREAK_HARDNESS_MULTIPLIER.get().floatValue();
            boolean changed = StaminaEngine.modifyStamina(player, data, cost, false);
            StaminaEngine.activeMiningPositions.remove(uuid);
            return changed;
        }
        return false;
    }

    public static void processServerSwing(ServerPlayer player, PlayerData data) {
        UUID uuid = player.getUUID();
        if (StaminaEngine.playersThrowingItem.contains(uuid)) {
            StaminaEngine.playersThrowingItem.remove(uuid);
            return;
        }

        float baseDrain = ConfigParser.getWeaponDrain(player.getMainHandItem(), ModConfig.BASE_SWING_DRAIN.get().floatValue());

        // Умножаем базовую трату на множитель атаки от истощения
        float finalDrain = baseDrain * getAttackStaminaCostModifier(player);

        float currentScale = player.getAttackStrengthScale(0.0F);
        float scaleModifier = Math.max(ModConfig.MIN_SWING_SCALE_DRAIN.get().floatValue(), currentScale);

        StaminaEngine.modifyStamina(player, data, finalDrain * scaleModifier, false);
    }

    public static float getAttackStaminaCostModifier(Player player) {
        PlayerData data = player.getData(ModDataAttachments.PLAYER_DATA.get());

        // Если игрок истощен, атака требует больше стамины (например, +50%, настраивается в конфиге)
        if (data.isExhausted()) {
            return 1.0f + ModConfig.EXHAUSTED_ATTACK_PENALTY_SOON.get().floatValue();
        }
        return 1.0f;
    }
}
