package com.zhigichan.encumbrance;
import com.zhigichan.encumbrance.data.ModDataAttachments;
import com.zhigichan.encumbrance.systems.ModConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;

@Mod(Encumbrance.MODID)
public class Encumbrance {
    public static final String MODID = "encumbrance";
    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MODID);

    public Encumbrance(IEventBus modEventBus) {
        net.neoforged.fml.ModLoadingContext.get().getActiveContainer().registerConfig(Type.SERVER, ModConfig.SPEC);
        ModDataAttachments.register(modEventBus);
    }
}
