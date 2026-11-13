package com.zhigichan.stamweight.mixin;

import com.zhigichan.stamweight.weight.WeightCore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class ContainerClickMixin {

    /**
     * Срабатывает ТОЛЬКО в момент реального клика мышкой/клавишей внутри открытого рюкзака Quark.
     * Пока ты просто смотришь в открытый рюкзак — код не вызывается ни разу!
     */
    @Inject(method = "doClick", at = @At("TAIL"))
    private void stamweight$onBackpackClick(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        String menuName = this.getClass().getName().toLowerCase();
        if (menuName.contains("quark") && menuName.contains("backpack")) {
            WeightCore.planLazyRecalc(serverPlayer);
        }
    }
}
