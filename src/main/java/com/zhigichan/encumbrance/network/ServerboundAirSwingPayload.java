package com.zhigichan.encumbrance.network;

import com.zhigichan.encumbrance.Encumbrance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ServerboundAirSwingPayload() implements CustomPacketPayload {

    public static final Type<ServerboundAirSwingPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Encumbrance.MODID, "air_swing"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundAirSwingPayload> CODEC =
            StreamCodec.unit(new ServerboundAirSwingPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
