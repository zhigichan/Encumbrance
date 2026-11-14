package com.zhigichan.stamweight.mixin;

import cn.mlus.thirst.content.thirst.PlayerThirst;
import com.zhigichan.stamweight.link.WeightStaminaLink;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerThirst.class, remap = false)
public abstract class ThirstDataMixin {

    @Shadow
    int thirst;

    @Unique
    private int stamweight$prevThirst = -1;

    @Inject(method = "updateThirstData", at = @At("HEAD"))
    private void stamweight$onThirstUpdated(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer && this.thirst != this.stamweight$prevThirst) {
            int oldThirst = this.stamweight$prevThirst;
            this.stamweight$prevThirst = this.thirst;

            // Реагируем только в зоне дебаффа (<= 6) или при выходе из неё (oldThirst <= 6)
            if (this.thirst <= WeightStaminaLink.SURVIVAL_THRESHOLD_LEVEL
                    || oldThirst <= WeightStaminaLink.SURVIVAL_THRESHOLD_LEVEL) {
                WeightStaminaLink.onSurvivalStatChanged(serverPlayer);
            }
        }
    }
}
