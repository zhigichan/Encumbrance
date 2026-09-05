package com.zhigichan.encumbrance.network;

import com.zhigichan.encumbrance.Encumbrance;
import com.zhigichan.encumbrance.data.ModDataAttachments;
import com.zhigichan.encumbrance.data.PlayerData;
import com.zhigichan.encumbrance.systems.stamina.StaminaActionsLogic; // Изменили импорт на новый путь
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = Encumbrance.MODID)
public class NetworkHandler {

    @SubscribeEvent
    public static void registerPackets(RegisterPayloadHandlersEvent event) {
        // Регистрация вашего первого пакета данных стамины
        event.registrar(Encumbrance.MODID).versioned("1.0.0").playToClient(
                ClientboundSyncDataPacket.TYPE, ClientboundSyncDataPacket.CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    LocalPlayer player = Minecraft.getInstance().player;
                    if (player != null) {
                        PlayerData data = player.getData(ModDataAttachments.PLAYER_DATA.get());
                        data.setStamina(packet.stamina());
                        data.setWeight(packet.weight());
                        data.setExhausted(packet.isExhausted());
                    }
                })
        );

        // ⚔️ 🔥 РЕГИСТРАЦИЯ ПАКЕТА ВЗМАХА В ВОЗДУХЕ (От Клиента к Серверу)
        event.registrar(Encumbrance.MODID).versioned("1.0.0").playToServer(
                ServerboundAirSwingPayload.TYPE, ServerboundAirSwingPayload.CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        PlayerData data = serverPlayer.getData(ModDataAttachments.PLAYER_DATA.get());
                        StaminaActionsLogic.processServerSwing(serverPlayer, data);

                        // ИСПРАВЛЕНИЕ ТУТ: отправляем изменения обратно на клиент сразу после взмаха в воздух
                        sendToPlayer(serverPlayer);
                    }
                })
        );
    }

    public static void sendToPlayer(ServerPlayer player) {
        PlayerData data = player.getData(ModDataAttachments.PLAYER_DATA.get());
        PacketDistributor.sendToPlayer(player, new ClientboundSyncDataPacket(data.getStamina(), data.getWeight(), data.isExhausted()));
    }

}
