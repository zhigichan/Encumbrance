package com.zhigichan.stamweight.weight;

import com.zhigichan.stamweight.StamWeight;
import com.zhigichan.stamweight.StamWeightData;
import com.zhigichan.stamweight.link.WeightStaminaLink;
import com.zhigichan.stamweight.network.WeightPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class WeightCore {

    private static final int DELAY_TICKS = 10; // Ровно 2 секунды (40 тиков)

    public static void planLazyRecalc(ServerPlayer player) {
        if (player == null) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        StamWeightData data = player.getData(StamWeight.PLAYER_DATA);
        long currentTick = server.getTickCount();

        if (data.getWeightTaskTargetTick() > currentTick) {
            return;
        }

        long targetTick = currentTick + DELAY_TICKS;
        data.setWeightTaskTargetTick(targetTick);

        System.out.println("[StamWeight] >>> Активатор взведен! Дверь закрыта до тика: " + targetTick);
    }

    public static void executeScheduledTasks(long currentTick, MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            StamWeightData data = player.getData(StamWeight.PLAYER_DATA);
            long target = data.getWeightTaskTargetTick();

            if (target > 0 && currentTick >= target) {

                System.out.println("[StamWeight] <<< ЦЕЛЬ ДОСТИГНУТА! Прошло ровно 20 тиков. Считаем вес.");

                // 1. Считаем вес
                float finalWeight = WeightCalc.getInventoryWeight(player);
                data.setCurrentWeight(finalWeight);

                // 2. Передаем новый вес в связку для обновления размера бочонка стамины
                WeightStaminaLink.onWeightUpdated(player, finalWeight);

                // 3. Сжимаем и шлем пакет веса
                int compressed = Math.round(finalWeight * 10.0f);
                PacketDistributor.sendToPlayer(player, new WeightPacket(compressed));

                // 4. Сбрасываем активатор
                data.setWeightTaskTargetTick(0);
            }
        }
    }
}
