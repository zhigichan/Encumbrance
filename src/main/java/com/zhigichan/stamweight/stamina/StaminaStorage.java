package com.zhigichan.stamweight.stamina;

import com.zhigichan.stamweight.StamWeight;
import com.zhigichan.stamweight.StamWeightData;
import com.zhigichan.stamweight.init.ModAttributes;
import com.zhigichan.stamweight.link.WeightStaminaLink;
import com.zhigichan.stamweight.network.StaminaSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public class StaminaStorage {

    public static double getMaxStamina(Player player) {
        double attrVal = player.getAttributeValue(ModAttributes.MAX_STAMINA);
        return Math.max(WeightStaminaLink.MIN_MAX_STAMINA, attrVal);
    }

    public static double getStamina(Player player) {
        return player.getData(StamWeight.PLAYER_DATA).getEndurance();
    }

    public static void setStamina(Player player, double value) {
        player.getData(StamWeight.PLAYER_DATA).setEndurance(value);
    }

    public static int getCooldown(Player player) {
        return player.getData(StamWeight.PLAYER_DATA).getEnduranceCooldown();
    }

    public static void setCooldown(Player player, int ticks) {
        player.getData(StamWeight.PLAYER_DATA).setEnduranceCooldown(ticks);
    }

    public static boolean isWalking(Player player) {
        return player.getData(StamWeight.PLAYER_DATA).isWalking();
    }

    public static void setWalking(Player player, boolean walking) {
        player.getData(StamWeight.PLAYER_DATA).setWalking(walking);
    }

    public static boolean wasSprinting(Player player) {
        return player.getData(StamWeight.PLAYER_DATA).wasSprinting();
    }

    public static void setWasSprinting(Player player, boolean sprinting) {
        player.getData(StamWeight.PLAYER_DATA).setWasSprinting(sprinting);
    }

    public static boolean isFatigued(Player player) {
        return player.getData(StamWeight.PLAYER_DATA).isFatigued();
    }

    public static void setFatigued(Player player, boolean fatigued) {
        player.getData(StamWeight.PLAYER_DATA).setFatigued(fatigued);
    }

    public static boolean isEmaciated(Player player) {
        return player.getData(StamWeight.PLAYER_DATA).isEmaciated();
    }

    public static void setEmaciated(Player player, boolean emaciated) {
        player.getData(StamWeight.PLAYER_DATA).setEmaciated(emaciated);
    }

    public static void syncIfChanged(ServerPlayer player, double currentStamina, double maxStamina) {
        int curScaled = (int) Math.round(currentStamina * 10.0);
        int maxScaled = (int) Math.round(maxStamina * 10.0);

        StamWeightData data = player.getData(StamWeight.PLAYER_DATA);
        if (curScaled != data.getLastSyncedCur() || maxScaled != data.getLastSyncedMax()) {
            data.setLastSyncedCur(curScaled);
            data.setLastSyncedMax(maxScaled);
            PacketDistributor.sendToPlayer(player, StaminaSyncPacket.fromValues(currentStamina, maxStamina));
        }
    }
}
