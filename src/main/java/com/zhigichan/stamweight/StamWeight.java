package com.zhigichan.stamweight;

import com.zhigichan.stamweight.client.StamWeightHud;
import com.zhigichan.stamweight.init.ModAttributes;
import com.zhigichan.stamweight.network.StaminaSyncPacket;
import com.zhigichan.stamweight.network.WalkingStatePacket;
import com.zhigichan.stamweight.network.WeightPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

@Mod(StamWeight.MODID)
public class StamWeight {
    public static final String MODID = "stamweight";

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    public static final Supplier<AttachmentType<StamWeightData>> PLAYER_DATA = ATTACHMENT_TYPES.register(
            "player_data",
            () -> AttachmentType.builder(() -> new StamWeightData())
                    .serialize(StamWeightData.CODEC)
                    .copyOnDeath()
                    .build()
    );

    public StamWeight(IEventBus modEventBus, ModContainer modContainer) {
        ATTACHMENT_TYPES.register(modEventBus);
        ModAttributes.register(modEventBus);
        modEventBus.addListener(this::modifyAttributes);
        modContainer.registerConfig(ModConfig.Type.SERVER, StamWeightConfig.SPEC);

        modEventBus.addListener(this::registerPackets);
        modEventBus.addListener(this::registerLayers);
    }

    private void modifyAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ModAttributes.ATTACK_RECHARGE_SPEED);
        event.add(EntityType.PLAYER, ModAttributes.SPRINT_JUMP_IMPULSE);
        event.add(EntityType.PLAYER, ModAttributes.STAMINA_RATE);
        event.add(EntityType.PLAYER, ModAttributes.MAX_STAMINA);
        event.add(EntityType.PLAYER, ModAttributes.MAX_WEIGHT);
    }

    private void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MODID).versioned("1.0.0");

        registrar.playToClient(
                WeightPacket.TYPE,
                WeightPacket.CODEC,
                (payload, context) -> context.enqueueWork(() -> StamWeightHud.updateWeightFromServer(payload.compressedWeight()))
        );

        registrar.playToServer(
                WalkingStatePacket.TYPE,
                WalkingStatePacket.STREAM_CODEC,
                WalkingStatePacket::handleServer
        );

        registrar.playToClient(
                StaminaSyncPacket.TYPE,
                StaminaSyncPacket.STREAM_CODEC,
                StaminaSyncPacket::handleClient
        );
    }

    private void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.FOOD_LEVEL,
                ResourceLocation.fromNamespaceAndPath(MODID, "stamweight_hud"),
                StamWeightHud::render
        );
    }
}
