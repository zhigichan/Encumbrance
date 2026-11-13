package com.zhigichan.stamweight.client;

import com.zhigichan.stamweight.StamWeight;
import com.zhigichan.stamweight.StamWeightData;
import com.zhigichan.stamweight.link.WeightStaminaLink;
import com.zhigichan.stamweight.network.WalkingStatePacket;
import com.zhigichan.stamweight.stamina.StaminaCalculator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = StamWeight.MODID, value = Dist.CLIENT)
public class StaminaClientHandler {

    private static boolean wasWalking = false;

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }

        StamWeightData data = player.getData(StamWeight.PLAYER_DATA);

        if (player.isCreative() || player.isSpectator()) {
            if (wasWalking) {
                wasWalking = false;
                PacketDistributor.sendToServer(new WalkingStatePacket(false));
            }
            data.setFatigued(false);
            data.setEmaciated(false);
            return;
        }

        handleSprintRestrictions(player, data);
        handleWalkingInput(player);
    }

    private static void handleSprintRestrictions(LocalPlayer player, StamWeightData data) {
        double currentEndurance = data.getEndurance();
        double maxEndurance = data.getMaxEndurance();

        Minecraft mc = Minecraft.getInstance();
        boolean isSprintKeyPressed = mc.options.keySprint.isDown();
        boolean hasForwardImpulse = player.input.hasForwardImpulse();

        // 1. Обновляем флаг Измождения (макс. стамина <= 6.0)
        boolean isEmaciatedNow = maxEndurance <= WeightStaminaLink.EMACIATION_MAX_THRESHOLD;
        data.setEmaciated(isEmaciatedNow);

        // 2. Обновляем флаг Усталости (для UI/индикаторов, бег он НЕ блокирует выше 0.0)
        if (isEmaciatedNow) {
            data.setFatigued(false);
        } else if (currentEndurance <= 0.0) {
            data.setFatigued(true);
        } else if (data.isFatigued() && currentEndurance >= StaminaCalculator.EXHAUSTION_RECOVERY_THRESHOLD && (!isSprintKeyPressed || !hasForwardImpulse)) {
            data.setFatigued(false);
        }

        // 3. ЖЁСТКАЯ БЛОКИРОВКА БЕГА: ТОЛЬКО при Измождении ИЛИ при 0.0 очках стамины!
        if (isEmaciatedNow || currentEndurance <= 0.0) {
            forceStopSprint(mc, player, isSprintKeyPressed);
        }
    }

    private static void forceStopSprint(Minecraft mc, LocalPlayer player, boolean isSprintKeyPressed) {
        if (player.isSprinting()) {
            player.setSprinting(false);
        }
        if (isSprintKeyPressed) {
            mc.options.keySprint.setDown(false);
        }
    }

    private static void handleWalkingInput(LocalPlayer player) {
        boolean hasMoveInput = player.input.forwardImpulse != 0.0f || player.input.leftImpulse != 0.0f;

        boolean isWalkingNow = hasMoveInput
                && !player.isSprinting()
                && !player.isPassenger()
                && !player.getAbilities().flying
                && !player.isFallFlying();

        if (isWalkingNow != wasWalking) {
            wasWalking = isWalkingNow;
            PacketDistributor.sendToServer(new WalkingStatePacket(isWalkingNow));
        }
    }
}
