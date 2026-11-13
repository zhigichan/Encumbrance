package com.zhigichan.stamweight.weight;

import com.zhigichan.stamweight.StamWeight;
import com.zhigichan.stamweight.datapack.ContainerModifierJsonLoader;
import com.zhigichan.stamweight.datapack.ItemWeightJsonLoader;
import com.zhigichan.stamweight.datapack.WeaponStaminaJsonLoader;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = StamWeight.MODID)
public class WeightEvents {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long currentTick = event.getServer().getTickCount();
        WeightCore.executeScheduledTasks(currentTick, event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            WeightCore.planLazyRecalc(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onItemPickedUp(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
            WeightCore.planLazyRecalc(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            WeightCore.planLazyRecalc(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onItemUsedFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            WeightCore.planLazyRecalc(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new WeaponStaminaJsonLoader());
        event.addListener(new ItemWeightJsonLoader());
        event.addListener(new ContainerModifierJsonLoader());
    }
}
