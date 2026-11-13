package com.zhigichan.stamweight.network;

import com.zhigichan.stamweight.stamina.StaminaStorage;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WalkingStatePacket(boolean isWalking) implements CustomPacketPayload {

    public static final Type<WalkingStatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("stamweight", "walking_state")
    );

    public static final StreamCodec<ByteBuf, WalkingStatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            WalkingStatePacket::isWalking,
            WalkingStatePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(final WalkingStatePacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                StaminaStorage.setWalking(serverPlayer, packet.isWalking());
            }
        });
    }
}
