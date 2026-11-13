package com.zhigichan.stamweight.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, "stamweight");

    // Атрибут 1: сколько очков заряда падает в бочонок за один тик игры (база 1.0)
    public static final DeferredHolder<Attribute, Attribute> ATTACK_RECHARGE_SPEED = ATTRIBUTES.register(
            "attack_recharge_speed",
            () -> new RangedAttribute("attribute.name.attack_recharge_speed", 1.0D, 0.0D, 20.0D).setSyncable(true)
    );

    // Атрибут 2: множитель импульса при прыжке на бегу (1.0 = стандартный ванильный рывок)
    public static final DeferredHolder<Attribute, Attribute> SPRINT_JUMP_IMPULSE = ATTRIBUTES.register(
            "sprint_jump_impulse",
            () -> new RangedAttribute("attribute.name.sprint_jump_impulse", 1.0D, 0.0D, 10.0D).setSyncable(true)
    );

    // Атрибут 3: общий множитель регенерации и расхода стамины (база 1.0)
    public static final DeferredHolder<Attribute, Attribute> STAMINA_RATE = ATTRIBUTES.register(
            "stamina_rate",
            () -> new RangedAttribute("attribute.name.stamina_rate", 1.0D, 0.0D, 20.0D).setSyncable(true)
    );

    public static final DeferredHolder<Attribute, Attribute> MAX_STAMINA = ATTRIBUTES.register(
            "max_stamina",
            () -> new RangedAttribute(
                    "attribute.name.max_stamina",
                    20.0D, // Базовое значение (20.0 очков)
                    2.0D,  // Минимум (защита от нулевого/отрицательного максимума)
                    1000.0D // Максимум (для зелий, колец и прокачки)
            ).setSyncable(true) // Обязательно true, чтобы клиент видел изменения!
    );

    public static final DeferredHolder<Attribute, Attribute> MAX_WEIGHT = ATTRIBUTES.register(
            "max_weight",
            () -> new RangedAttribute(
                    "attribute.name.max_weight",
                    100.0D,   // Базовый лимит веса по умолчанию
                    1.0D,     // Минимум (защита от деления на ноль при расчёте процентов)
                    100000.0D // Максимум (для прокачки в аддоне и экипировки)
            ).setSyncable(true) // Обязательно true, чтобы клиент видел лимит для HUD
    );

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
    }
}
