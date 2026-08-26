package com.zhigichan_31.thermodynamics.systems;

import com.zhigichan_31.thermodynamics.Thermodynamics;
import com.zhigichan_31.thermodynamics.api.IWeightProvider;
import com.zhigichan_31.thermodynamics.data.ModDataAttachments;
import com.zhigichan_31.thermodynamics.data.PlayerData;
import com.zhigichan_31.thermodynamics.network.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.*;

@EventBusSubscriber(modid = Thermodynamics.MODID)
@SuppressWarnings("resource")
public class WeightManager {
    private static final List<IWeightProvider> addons = new ArrayList<>();

    public static void registerAddon(IWeightProvider addon) {
        if (addon != null && !addons.contains(addon)) addons.add(addon);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) recalculateWeight(sp);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) recalculateWeight(sp);
    }

    @SubscribeEvent
    public static void onServerPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || !(p instanceof ServerPlayer sp)) return;

        // Если игрок в креативе/спектрале, мгновенно сбрасываем вес
        if (!sp.gameMode.isSurvival()) {
            PlayerData data = sp.getData(ModDataAttachments.PLAYER_DATA.get());
            if (data.getWeight() > 0) {
                data.setWeight(0.0f);
                NetworkHandler.sendToPlayer(sp);
            }
            return;
        }

        // Оптимизированный пересчет раз в 5 тиков
        if (sp.tickCount % 5 == 0) {
            recalculateWeight(sp);
        }
    }

    public static void recalculateWeight(ServerPlayer player) {
        float totalWeight = 0.0f;

        // Считаем вес предметов с учетом нового базового веса из конфига
        for (ItemStack stack : player.getInventory().items) if (!stack.isEmpty()) totalWeight += calculateItemWeight(stack) * stack.getCount();
        for (ItemStack armorStack : player.getInventory().armor) if (!armorStack.isEmpty()) totalWeight += calculateItemWeight(armorStack);
        for (ItemStack offhandStack : player.getInventory().offhand) if (!offhandStack.isEmpty()) totalWeight += calculateItemWeight(offhandStack) * offhandStack.getCount();

        for (IWeightProvider addon : addons) totalWeight += addon.getTotalWeight(player);

        PlayerData data = player.getData(ModDataAttachments.PLAYER_DATA.get());
        float oldWeight = data.getWeight();

        // Обновляем данные на клиенте только при реальном изменении веса
        if (Math.abs(oldWeight - totalWeight) > 0.001f) {
            data.setWeight(totalWeight);
            NetworkHandler.sendToPlayer(player);
        }
    }

    public static float calculateItemWeight(ItemStack stack) {
        // Заменили хардкод 1.0f на динамическое значение базового веса из конфигурации
        float defaultWeight = ModConfig.BASE_ITEM_WEIGHT.get().floatValue();
        return stack.isEmpty() ? 0.0f : ConfigParser.getItemWeight(stack, defaultWeight);
    }

    /**
     * Множитель расхода стамины от ТЯЖЕСТИ (используется при беге/копании)
     */
    public static float getStaminaModifier(Player player) {
        PlayerData data = player.getData(ModDataAttachments.PLAYER_DATA.get());
        float currentWeight = data.getWeight();
        float baseModifier = 1.0f + (currentWeight / ModConfig.MAX_CARRY_WEIGHT.get().floatValue());

        for (IWeightProvider addon : addons) baseModifier *= addon.getStaminaDrainModifier(player);
        return baseModifier;
    }

    /**
     * Множитель расхода стамины на атаку при ИСТОЩЕНИИ.
     * Вызовите этот метод там, где у вас прописано снятие стамины за удар мечом/инструментом.
     */
    public static float getAttackStaminaCostModifier(Player player) {
        PlayerData data = player.getData(ModDataAttachments.PLAYER_DATA.get());

        // Если игрок истощен, атака требует больше стамины (например, +50%, настраивается в конфиге)
        if (data.isExhausted()) {
            return 1.0f + ModConfig.EXHAUSTED_ATTACK_PENALTY.get().floatValue();
        }
        return 1.0f;
    }
}
