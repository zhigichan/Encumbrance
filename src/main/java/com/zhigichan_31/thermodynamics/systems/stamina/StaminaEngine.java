package com.zhigichan_31.thermodynamics.systems.stamina;

import com.zhigichan_31.thermodynamics.data.ModDataAttachments;
import com.zhigichan_31.thermodynamics.data.PlayerData;
import com.zhigichan_31.thermodynamics.network.NetworkHandler;
import com.zhigichan_31.thermodynamics.systems.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public class StaminaEngine {
    public static final float MAX_STAMINA = 20.0f;

    public static final HashMap<UUID, Integer> regenCooldowns = new HashMap<>();
    public static final HashSet<UUID> playersThrowingItem = new HashSet<>();
    public static final HashMap<UUID, BlockPos> activeMiningPositions = new HashMap<>();

    /**
     * Безопасное изменение стамины (в пределах от 0.0 до 20.0)
     */
    public static boolean modifyStamina(ServerPlayer player, PlayerData data, float amount, boolean isRecovery) {
        float oldStamina = data.getStamina();
        float newStamina;

        if (isRecovery) {
            newStamina = Math.min(MAX_STAMINA, oldStamina + amount);
        } else {
            newStamina = Math.max(0.0f, oldStamina - amount);
            // При трате стамины обновляем кулдаун регенерации
            regenCooldowns.put(player.getUUID(), ModConfig.REGEN_COOLDOWN.get());
        }

        if (Math.abs(oldStamina - newStamina) > 0.0001f) {
            data.setStamina(newStamina);
            return true;
        }
        return false;
    }

    public static void clearPlayerData(UUID uuid) {
        regenCooldowns.remove(uuid);
        playersThrowingItem.remove(uuid);
        activeMiningPositions.remove(uuid);
    }
}
