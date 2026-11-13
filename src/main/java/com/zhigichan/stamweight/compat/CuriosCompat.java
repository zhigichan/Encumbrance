package com.zhigichan.stamweight.compat;

import com.zhigichan.stamweight.StamWeight;
import com.zhigichan.stamweight.weight.WeightCalc;
import com.zhigichan.stamweight.weight.WeightCore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public class CuriosCompat {

    private static final boolean CURIOS_LOADED = ModList.get().isLoaded("curios");

    /**
     * Считает общий вес всех предметов во всех слотах Curios игрока
     * (включая содержимое рюкзака Quark, если он надет в слот спины).
     */
    public static float getCuriosWeight(Player player) {
        if (!CURIOS_LOADED) {
            return 0.0f;
        }

        return CuriosApi.getCuriosInventory(player)
                .map(CuriosCompat::calculateHandlerWeight)
                .orElse(0.0f);
    }

    private static float calculateHandlerWeight(ICuriosItemHandler handler) {
        float totalWeight = 0.0f;

        // Проходим по всем зарегистрированным типам слотов Curios без привязки к их именам
        for (ICurioStacksHandler stacksHandler : handler.getCurios().values()) {
            IDynamicStackHandler stacks = stacksHandler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    totalWeight += WeightCalc.calculateStackWeight(stack);
                }
            }
        }

        return totalWeight;
    }

    /**
     * Внутренний слушатель событий Curios: пинает ленивый пересчёт веса,
     * когда игрок надевает, снимает или меняет предмет в слотах Curios.
     */
    @EventBusSubscriber(modid = StamWeight.MODID)
    public static class CuriosEvents {

        @SubscribeEvent
        public static void onCurioChange(CurioChangeEvent event) {
            if (!CURIOS_LOADED) return;

            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                WeightCore.planLazyRecalc(serverPlayer);
            }
        }
    }
}
