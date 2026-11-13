package com.zhigichan.stamweight.mixin;

import com.zhigichan.stamweight.network.SwingPacketExtension;
import com.zhigichan.stamweight.stamina.StaminaActions;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {

    @Shadow public ServerPlayer player;

    @Unique
    private long stamweight$lastSwingGameTick = -1L;

    @Inject(method = "handleAnimate", at = @At("HEAD"))
    private void stamweight$onPlayerSwingPacket(ServerboundSwingPacket packet, CallbackInfo ci) {
        if (packet.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (packet instanceof SwingPacketExtension ext && ext.stamweight$isAttackSwing()) {
            // Защита от двойного клиентского пакета swing() в одном тике при ударе
            long currentTick = this.player.level().getGameTime();
            if (this.stamweight$lastSwingGameTick == currentTick) {
                return;
            }
            this.stamweight$lastSwingGameTick = currentTick;

            float chargeRatio = this.player.getAttackStrengthScale(0.0F);
            StaminaActions.onPlayerAttackSwing(this.player, chargeRatio);

            // Сбрасываем бочонок перезарядки на сервере
            this.player.resetAttackStrengthTicker();
        }
    }
}
