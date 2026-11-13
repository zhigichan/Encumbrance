package com.zhigichan.stamweight.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zhigichan.stamweight.stamina.StaminaActions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

public class WeaponStaminaJsonLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public WeaponStaminaJsonLoader() {
        super(GSON, "weapon_stamina");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        StaminaActions.ITEM_ID_COSTS.clear();
        StaminaActions.ITEM_TAG_COSTS.clear();

        for (JsonElement element : objects.values()) {
            if (!element.isJsonObject()) continue;
            JsonObject root = element.getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                if (!entry.getValue().isJsonPrimitive()) continue;

                String key = entry.getKey().trim();
                double value = entry.getValue().getAsDouble();

                if (key.startsWith("#")) {
                    ResourceLocation tagId = ResourceLocation.tryParse(key.substring(1));
                    if (tagId != null) {
                        StaminaActions.ITEM_TAG_COSTS.put(TagKey.create(Registries.ITEM, tagId), value);
                    }
                } else {
                    ResourceLocation itemId = ResourceLocation.tryParse(key);
                    if (itemId != null) {
                        StaminaActions.ITEM_ID_COSTS.put(itemId, value);
                    }
                }
            }
        }
    }
}
