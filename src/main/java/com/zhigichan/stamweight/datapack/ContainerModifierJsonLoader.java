package com.zhigichan.stamweight.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zhigichan.stamweight.weight.WeightCalc;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

public class ContainerModifierJsonLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public ContainerModifierJsonLoader() {
        super(GSON, "container_modifiers");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        WeightCalc.CONTAINER_ID_MODIFIERS.clear();
        WeightCalc.CONTAINER_TAG_MODIFIERS.clear();
        WeightCalc.clearCaches();

        for (JsonElement element : objects.values()) {
            if (!element.isJsonObject()) continue;
            JsonObject root = element.getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                if (!entry.getValue().isJsonPrimitive()) continue;

                String key = entry.getKey().trim();
                float value = entry.getValue().getAsFloat();

                if (key.startsWith("#")) {
                    ResourceLocation tagId = ResourceLocation.tryParse(key.substring(1));
                    if (tagId != null) {
                        WeightCalc.CONTAINER_TAG_MODIFIERS.put(TagKey.create(Registries.ITEM, tagId), value);
                    }
                } else {
                    ResourceLocation itemId = ResourceLocation.tryParse(key);
                    if (itemId != null) {
                        WeightCalc.CONTAINER_ID_MODIFIERS.put(itemId, value);
                    }
                }
            }
        }
    }
}
