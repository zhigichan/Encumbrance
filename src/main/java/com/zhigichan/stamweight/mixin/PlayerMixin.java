package com.zhigichan.stamweight.mixin;

import com.zhigichan.stamweight.init.ModAttributes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    // Кастомный независимый бочонок. Не зависит от refMap, не крашит загрузку игры!
    @Unique
    private float stamweight$customAttackTicker = 0.0F;

    /**
     * НАКАТ ПО ВАШЕЙ ФОРМУЛЕ (Каждый тик игры)
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void stamweight$fillFixedCooldownBarrel(CallbackInfo ci) {
        Player player = (Player) (Object) this;

        // Если бочонок уже полон (достиг фиксированного объема 20) — код засыпает! Нагрузка 0%
        if (this.stamweight$customAttackTicker >= 20.0F) {
            this.stamweight$customAttackTicker = 20.0F;
            return;
        }

        // 1. Находим нашу базу (атрибут веса брони/выносливости из папки init)
        double baseAttribute = player.getAttributeValue(ModAttributes.ATTACK_RECHARGE_SPEED);

        if (baseAttribute <= 0.0D) {
            return;
        }

        // 2. Находим множитель оружия (динамический ATTACK_SPEED руки или предмета)
        double weaponMultiplier = player.getAttributeValue(Attributes.ATTACK_SPEED);

        // 3. ВАША ФОРМУЛА: шаг = база * множитель
        float step = (float) (baseAttribute * weaponMultiplier);

        // Наваливаем полученные очки в бочонок за этот тик
        this.stamweight$customAttackTicker = Math.min(20.0F, this.stamweight$customAttackTicker + step);
    }

    /**
     * СБРОС БОЧОНКА (При ударе или смене слота)
     */
    @Inject(method = "resetAttackStrengthTicker", at = @At("HEAD"))
    private void stamweight$clearCooldownBarrel(CallbackInfo ci) {
        // Опустошаем бочонок в ноль, запуская перезарядку заново
        this.stamweight$customAttackTicker = 0.0F;
    }

    /**
     * ИДЕАЛЬНЫЙ ФИКСИРОВАННЫЙ РЕНДЕР ПОЛОСОЧКИ НА 20 ОЧКОВ
     * Майнкрафт просит процент заполнения полоски. Мы делим наши очки строго на объем 20!
     */
    @Inject(method = "getAttackStrengthScale", at = @At("HEAD"), cancellable = true)
    private void stamweight$renderFixedScale(float partialTicks, CallbackInfoReturnable<Float> cir) {
        // Плавное время для рендера с учетом partialTicks
        float currentTicks = this.stamweight$customAttackTicker + partialTicks;

        // Процент готовности полоски относительно ФИКСИРОВАННОГО объема 20
        float scale = currentTicks / 20.0F;
        float clampedScale = Math.max(0.0F, Math.min(scale, 1.0F));

        // Отдаем результат рендеру. Полоска ВСЕГДА будет заполняться ровно до конца (100%)
        cir.setReturnValue(clampedScale);
    }
}