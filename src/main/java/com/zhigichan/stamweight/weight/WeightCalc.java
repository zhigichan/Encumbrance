package com.zhigichan.stamweight.weight;

import com.zhigichan.stamweight.StamWeightConfig;
import com.zhigichan.stamweight.compat.CuriosCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class WeightCalc {

    public static float DEFAULT_CONTAINER_MODIFIER = 1.0f;

    public static final Map<ResourceLocation, Float> ITEM_ID_WEIGHTS = new LinkedHashMap<>();
    public static final Map<TagKey<Item>, Float> ITEM_TAG_WEIGHTS = new LinkedHashMap<>();

    public static final Map<ResourceLocation, Float> CONTAINER_ID_MODIFIERS = new LinkedHashMap<>();
    public static final Map<TagKey<Item>, Float> CONTAINER_TAG_MODIFIERS = new LinkedHashMap<>();

    private static final Map<ItemContainerContents, Float> CONTAINER_WEIGHT_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<BundleContents, Float> BUNDLE_WEIGHT_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static void clearCaches() {
        CONTAINER_WEIGHT_CACHE.clear();
        BUNDLE_WEIGHT_CACHE.clear();
    }

    public static float getInventoryWeight(Player player) {
        float totalWeight = 0.0f;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                totalWeight += calculateStackWeight(stack);
            }
        }

        totalWeight += CuriosCompat.getCuriosWeight(player);

        return totalWeight;
    }

    public static float calculateStackWeight(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0f;
        }

        float unitWeight = getSingleItemBaseWeight(stack);
        float selfWeight = unitWeight * stack.getCount();

        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container != null) {
            float rawInsideWeight = getCachedContainerRawWeight(container);
            float containerMod = getContainerMultiplier(stack);
            return selfWeight + (rawInsideWeight * containerMod);
        }

        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) {
            float rawInsideWeight = getCachedBundleRawWeight(bundle);
            float containerMod = getContainerMultiplier(stack);
            return selfWeight + (rawInsideWeight * containerMod);
        }

        return selfWeight;
    }

    public static float getSingleItemBaseWeight(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Float idWeight = ITEM_ID_WEIGHTS.get(itemId);
        if (idWeight != null) {
            return idWeight;
        }

        for (Map.Entry<TagKey<Item>, Float> entry : ITEM_TAG_WEIGHTS.entrySet()) {
            if (stack.is(entry.getKey())) {
                return entry.getValue();
            }
        }

        return StamWeightConfig.DEFAULT_ITEM_WEIGHT.get().floatValue();
    }

    public static float getContainerMultiplier(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Float idMod = CONTAINER_ID_MODIFIERS.get(itemId);
        if (idMod != null) {
            return idMod;
        }

        for (Map.Entry<TagKey<Item>, Float> entry : CONTAINER_TAG_MODIFIERS.entrySet()) {
            if (stack.is(entry.getKey())) {
                return entry.getValue();
            }
        }

        return DEFAULT_CONTAINER_MODIFIER;
    }

    private static float getCachedContainerRawWeight(ItemContainerContents contents) {
        Float cached = CONTAINER_WEIGHT_CACHE.get(contents);
        if (cached != null) {
            return cached;
        }

        float insideWeight = 0.0f;
        for (ItemStack innerStack : contents.nonEmptyItems()) {
            if (!innerStack.isEmpty()) {
                insideWeight += calculateStackWeight(innerStack);
            }
        }

        CONTAINER_WEIGHT_CACHE.put(contents, insideWeight);
        return insideWeight;
    }

    private static float getCachedBundleRawWeight(BundleContents contents) {
        Float cached = BUNDLE_WEIGHT_CACHE.get(contents);
        if (cached != null) {
            return cached;
        }

        float insideWeight = 0.0f;
        for (ItemStack innerStack : contents.items()) {
            if (!innerStack.isEmpty()) {
                insideWeight += calculateStackWeight(innerStack);
            }
        }

        BUNDLE_WEIGHT_CACHE.put(contents, insideWeight);
        return insideWeight;
    }
}
