package com.zhigichan.stamweight.mixin;

import com.zhigichan.stamweight.stamina.StaminaActions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @Shadow @Final protected ServerPlayer player;
    @Shadow private boolean isDestroyingBlock;
    @Shadow private boolean hasDelayedDestroy;

    /**
     * Ванильный тик копания на сервере сам пинает наш StaminaActions,
     * когда игрок реально ломает блок.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void stamweight$onServerMiningTick(CallbackInfo ci) {
        if (this.isDestroyingBlock || this.hasDelayedDestroy) {
            StaminaActions.onPlayerMiningTick(this.player);
        }
    }
}
