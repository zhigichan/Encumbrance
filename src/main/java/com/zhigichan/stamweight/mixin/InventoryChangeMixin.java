package com.zhigichan.stamweight.mixin;

import com.zhigichan.stamweight.weight.WeightCore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryChangeMixin {

    @Shadow @Final public Player player;

    @Inject(method = "setItem", at = @At("TAIL"))
    private void stamweight$onSlotSet(int slot, ItemStack stack, CallbackInfo ci) {
        if (this.player instanceof ServerPlayer serverPlayer) {
            WeightCore.planLazyRecalc(serverPlayer);
        }
    }

    @Inject(method = "removeItem(II)Lnet/minecraft/world/item/ItemStack;", at = @At("TAIL"))
    private void stamweight$onSlotRemoveCount(int slot, int amount, CallbackInfoReturnable<ItemStack> cir) {
        if (this.player instanceof ServerPlayer serverPlayer && !cir.getReturnValue().isEmpty()) {
            WeightCore.planLazyRecalc(serverPlayer);
        }
    }

    @Inject(method = "removeItemNoUpdate", at = @At("TAIL"))
    private void stamweight$onSlotRemoveNoUpdate(int slot, CallbackInfoReturnable<ItemStack> cir) {
        if (this.player instanceof ServerPlayer serverPlayer && !cir.getReturnValue().isEmpty()) {
            WeightCore.planLazyRecalc(serverPlayer);
        }
    }
}
