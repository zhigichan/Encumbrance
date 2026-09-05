package com.zhigichan.encumbrance.mixin;

import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class ModifierPacketHungry {

    @Shadow private float saturationLevel;

    // Скрытая переменная внутри FoodData для запоминания ПРОШЛОГО целого значения сытости
    @Unique
    private int thermo$lastSaturationInt = -1;

    @Inject(method = "tick", at = @At("TAIL"))
    private void syncSaturationOnCellularChange(Player player, CallbackInfo ci) {
        // Проверяем, что код выполняется на СЕРВЕРЕ для реального сетевого игрока
        if (player instanceof ServerPlayer serverPlayer) {

            // Защита: если игрок в процессе отключения или мир только загружается
            if (serverPlayer.connection == null) return;

            // 1. Округляем текущее насыщение до целого числа (например, 4.6 -> 4, а 3.9 -> 3)
            int currentSatInt = (int) Math.floor(this.saturationLevel);

            // 2. Если это самый первый тик, инициализируем переменную и выходим
            if (thermo$lastSaturationInt == -1) {
                thermo$lastSaturationInt = currentSatInt;
                return;
            }

            // 3. Проверяем изменения:
            // Если целая часть сытости изменилась с момента последней отправки
            if (currentSatInt != thermo$lastSaturationInt) {

                // Шлём ванильный пакет напрямую в сеть игрока. Это 100% скомпилируется в 1.21.1!
                serverPlayer.connection.send(new ClientboundSetHealthPacket(
                        serverPlayer.getHealth(),
                        serverPlayer.getFoodData().getFoodLevel(),
                        this.saturationLevel // Сюда передаем точный float, чтобы у клиента были идеальные данные
                ));

                // Запоминаем новое целое значение, чтобы не спамить сеть на следующем тике
                thermo$lastSaturationInt = currentSatInt;
            }
        }
    }
}
