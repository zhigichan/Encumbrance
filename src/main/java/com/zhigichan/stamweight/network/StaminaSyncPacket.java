package com.zhigichan.stamweight.network;

import com.zhigichan.stamweight.StamWeight;
import com.zhigichan.stamweight.StamWeightData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StaminaSyncPacket(int scaledStamina, int scaledMaxStamina) implements CustomPacketPayload {

    public static final Type<StaminaSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("stamweight", "stamina_sync")
    );

    // Используем VAR_INT для сжатия в 1-2 байта
    public static final StreamCodec<ByteBuf, StaminaSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            StaminaSyncPacket::scaledStamina,
            ByteBufCodecs.VAR_INT,
            StaminaSyncPacket::scaledMaxStamina,
            StaminaSyncPacket::new
    );

    // Удобный конструктор, который сам умножает на 10 и округляет
    public static StaminaSyncPacket fromValues(double stamina, double maxStamina) {
        return new StaminaSyncPacket(
                (int) Math.round(stamina * 10.0),
                (int) Math.round(maxStamina * 10.0)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(final StaminaSyncPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player != null) {
                // Расшифровываем обратно с точностью до десятых и пишем прямо в StamWeightData
                double realStamina = packet.scaledStamina() / 10.0;
                double realMaxStamina = packet.scaledMaxStamina() / 10.0;

                StamWeightData data = player.getData(StamWeight.PLAYER_DATA);
                data.setEndurance(realStamina);
                data.setMaxEndurance(realMaxStamina);
            }
        });
    }
}
