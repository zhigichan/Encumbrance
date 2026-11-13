package com.zhigichan.stamweight.mixin;

import com.zhigichan.stamweight.network.SwingPacketExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerboundSwingPacket.class)
public abstract class ServerboundSwingPacketMixin implements SwingPacketExtension {

    @Unique
    private boolean stamweight$isAttackSwing = false;

    @Override
    public boolean stamweight$isAttackSwing() {
        return this.stamweight$isAttackSwing;
    }

    /**
     * Клиент сам решает в момент отправки пакета:
     * 1 (true) — только если нажата кнопка атаки и игрок не ломает блок.
     * 0 (false) — выброс на Q, открытие сундуков, копание, использование предметов.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/InteractionHand;)V", at = @At("TAIL"))
    private void stamweight$onClientCreate(InteractionHand hand, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        boolean isAttackKeyDown = mc.options != null && mc.options.keyAttack.isDown();
        boolean isMining = mc.gameMode != null && mc.gameMode.isDestroying();
        this.stamweight$isAttackSwing = isAttackKeyDown && !isMining;
    }

    @Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("TAIL"))
    private void stamweight$onServerDecode(FriendlyByteBuf buffer, CallbackInfo ci) {
        this.stamweight$isAttackSwing = buffer.readBoolean();
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void stamweight$onClientEncode(FriendlyByteBuf buffer, CallbackInfo ci) {
        buffer.writeBoolean(this.stamweight$isAttackSwing);
    }
}
