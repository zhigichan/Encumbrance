package com.zhigichan.stamweight.network;

import com.zhigichan.stamweight.StamWeight;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Используем int вместо float для оптимизации трафика (вес * 10)
public record WeightPacket(int compressedWeight) implements CustomPacketPayload {

    public static final Type<WeightPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(StamWeight.MODID, "weight_packet"));

    // Стрим-кодек для чтения и записи целого числа (int) в сетевой буфер
    public static final StreamCodec<FriendlyByteBuf, WeightPacket> CODEC = StreamCodec.of(
            (buf, value) -> buf.writeInt(value.compressedWeight),
            buf -> new WeightPacket(buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
