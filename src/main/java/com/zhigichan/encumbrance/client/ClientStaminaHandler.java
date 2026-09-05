package com.zhigichan.encumbrance.client;

import com.zhigichan.encumbrance.Encumbrance;
import com.zhigichan.encumbrance.network.ServerboundAirSwingPayload;
import com.zhigichan.encumbrance.data.ModDataAttachments;
import com.zhigichan.encumbrance.data.PlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Encumbrance.MODID, value = Dist.CLIENT)
public class ClientStaminaHandler {

    // Флаг блокировки спринта
    private static boolean isSprintBlocked = false;

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        Player basePlayer = event.getEntity();
        if (basePlayer instanceof LocalPlayer player) {
            PlayerData data = player.getData(ModDataAttachments.PLAYER_DATA.get());

            Minecraft mc = Minecraft.getInstance();
            boolean isSprintKeyPressed = mc.options.keySprint.isDown();

            // Проверяем, жмет ли игрок движение вперед (через импульс ввода)
            boolean hasForwardImpulse = player.input.hasForwardImpulse();

            // 1. Возвращено: Жестко блокируем спринт строго при достижении нуля
            if (data.getStamina() <= 0.0f) {
                isSprintBlocked = true;
            }

            // 2. Если спринт заблокирован, принудительно гасим его
            if (isSprintBlocked) {
                if (player.isSprinting()) {
                    player.setSprinting(false);
                }
                // Отключаем ванильный триггер удержания кнопки бега
                if (isSprintKeyPressed) {
                    mc.options.keySprint.setDown(false);
                }
            }

            // 3. Возвращено: Сбрасываем блокировку, если отжата кнопка спринта ИЛИ если игрок перестал бежать вперед (отпустил W)
            if (!isSprintKeyPressed || !hasForwardImpulse) {
                isSprintBlocked = false;
            }
        }
    }

    @SubscribeEvent
    public static void onClientAirClick(PlayerInteractEvent.LeftClickEmpty event) {
        if (event.getEntity() instanceof net.minecraft.client.player.LocalPlayer player) {
            if (!player.isCreative() && !player.isSpectator()) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ServerboundAirSwingPayload());
            }
        }
    }
}
