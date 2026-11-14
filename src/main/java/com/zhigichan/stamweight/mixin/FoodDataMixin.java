package com.zhigichan.stamweight.mixin;

import com.zhigichan.stamweight.link.WeightStaminaLink;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    @Shadow
    private int foodLevel;

    @Unique
    private int stamweight$prevFood = -1;

    @Inject(method = "tick", at = @At("TAIL"))
    private void stamweight$onFoodTick(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer && this.foodLevel != this.stamweight$prevFood) {
            int oldFood = this.stamweight$prevFood;
            this.stamweight$prevFood = this.foodLevel;

            // Реагируем только в зоне дебаффа (<= 6) или при выходе из неё (oldFood <= 6)
            if (this.foodLevel <= WeightStaminaLink.SURVIVAL_THRESHOLD_LEVEL
                    || oldFood <= WeightStaminaLink.SURVIVAL_THRESHOLD_LEVEL) {
                WeightStaminaLink.onSurvivalStatChanged(serverPlayer);
            }
        }
    }
}
