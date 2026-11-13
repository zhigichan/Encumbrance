package com.zhigichan.stamweight.mixin;

import com.zhigichan.stamweight.init.ModAttributes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {

    private static final double VANILLA_SPRINT_JUMP_BASE = 0.2D;

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void stamweight$modifySprintJumpImpulse(CallbackInfo ci) {
        if ((Object) this instanceof Player player && player.isSprinting()) {
            float yawRad = player.getYRot() * ((float) Math.PI / 180.0F);
            Vec3 currentVel = player.getDeltaMovement();

            // 1. Вычитаем ванильный рывок 0.2
            double vanillaDx = -Mth.sin(yawRad) * VANILLA_SPRINT_JUMP_BASE;
            double vanillaDz = Mth.cos(yawRad) * VANILLA_SPRINT_JUMP_BASE;

            // 2. Считаем наш импульс: (Атрибут * 0.2) * (Текущая_Скорость / 0.1)
            double impulseMultiplier = player.getAttributeValue(ModAttributes.SPRINT_JUMP_IMPULSE);
            double currentSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
            double finalImpulse = (impulseMultiplier * VANILLA_SPRINT_JUMP_BASE) * (currentSpeed / 0.1D);

            // 3. Прибавляем наш честный импульс
            double customDx = -Mth.sin(yawRad) * finalImpulse;
            double customDz = Mth.cos(yawRad) * finalImpulse;

            player.setDeltaMovement(
                    currentVel.x - vanillaDx + customDx,
                    currentVel.y,
                    currentVel.z - vanillaDz + customDz
            );
        }
    }
}
