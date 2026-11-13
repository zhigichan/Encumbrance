package com.zhigichan.stamweight;

import net.neoforged.neoforge.common.ModConfigSpec;

public class StamWeightConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // --- [stamina] ---
    public static final ModConfigSpec.DoubleValue SPRINT_DELTA;
    public static final ModConfigSpec.DoubleValue WALK_DELTA;
    public static final ModConfigSpec.BooleanValue WALK_FLOOR_ZERO;
    public static final ModConfigSpec.DoubleValue IDLE_REGEN;
    public static final ModConfigSpec.DoubleValue SWING_COST;
    public static final ModConfigSpec.DoubleValue MINING_COST;
    public static final ModConfigSpec.IntValue REGEN_COOLDOWN;

    // --- [weight] ---
    public static final ModConfigSpec.DoubleValue DEFAULT_ITEM_WEIGHT;

    // --- [survival] ---
    public static final ModConfigSpec.DoubleValue SURVIVAL_REGEN_DELTA;
    public static final ModConfigSpec.DoubleValue SURVIVAL_STAMINA_PER_POINT;

    // --- [overload_logic] ---
    public static final ModConfigSpec.DoubleValue OVERLOAD_STAMINA_PENALTY;
    public static final ModConfigSpec.DoubleValue OVERLOAD_REGEN_DELTA;
    public static final ModConfigSpec.DoubleValue OVERLOAD_GRAVITY_PERCENT;
    public static final ModConfigSpec.DoubleValue OVERLOAD_SPEED_PERCENT;

    static {
        // [стамина]
        BUILDER.push("stamina");
        SPRINT_DELTA = BUILDER
                .comment("Максимальная трата/изменение при беге (дефолт -0.05 очков за тик)")
                .defineInRange("sprintDelta", -0.05, -100.0, 100.0);
        WALK_DELTA = BUILDER
                .comment("Максимальная трата/реген при ходьбе (дефолт 0.02 очков за тик)")
                .defineInRange("walkDelta", 0.02, -100.0, 100.0);
        WALK_FLOOR_ZERO = BUILDER
                .comment("Определяет, работает ли ограничитель траты при ходьбе не меньше 0.0 (дефолт false)")
                .define("walkFloorZero", false);
        IDLE_REGEN = BUILDER
                .comment("Восстановление стамины в покое (дефолт 1.0 очков за тик)")
                .defineInRange("idleRegen", 0.1, 0.0, 100.0);
        SWING_COST = BUILDER
                .comment("Дефолтный параметр удара в 0.5 очков")
                .defineInRange("swingCost", 0.5, 0.0, 100.0);
        MINING_COST = BUILDER
                .comment("Дефолтный параметр вскапывания за тик (дефолт 0.05 очков)")
                .defineInRange("miningCost", 0.05, 0.0, 100.0);
        REGEN_COOLDOWN = BUILDER
                .comment("Кулдаун восстановления в тиках (20 тиков = 1 секунда)")
                .defineInRange("regenCooldown", 20, 0, 1200);
        BUILDER.pop();

        // [весс]
        BUILDER.push("weight");
        DEFAULT_ITEM_WEIGHT = BUILDER
                .comment("Если отсутствует параметр веса к предмету в JSON, используется этот параметр в виде очков (дефолт 1.0)")
                .defineInRange("defaultItemWeight", 1.0, 0.0, 10000.0);
        BUILDER.pop();

        // [потребность]
        BUILDER.push("survival");
        SURVIVAL_REGEN_DELTA = BUILDER
                .comment("Если <= 6 очков голода и жажды, применяется снижение скорости регенерации в очках (дефолт -0.15, стакаются)")
                .defineInRange("regenDelta", -0.15, -100.0, 100.0);
        SURVIVAL_STAMINA_PER_POINT = BUILDER
                .comment("После 6 очков голода и жажды за каждое отнятое очко снижается максимальная выносливость (дефолт -0.2, стакаются)")
                .defineInRange("staminaPerPoint", -0.2, -100.0, 0.0);
        BUILDER.pop();

        // [логика]
        BUILDER.push("overload_logic");
        BUILDER.comment("Вес от 0% до 100% считается бездебаффным. Дебаффы начинаются после 100% (указаны значения за промежуток в 100% перегруза):");
        OVERLOAD_STAMINA_PENALTY = BUILDER
                .comment("Настройка выносливости на промежуток 100% в виде числа (дефолт -4.0)")
                .defineInRange("staminaPenalty", -4.0, -1000.0, 0.0);
        OVERLOAD_REGEN_DELTA = BUILDER
                .comment("Настройка регенерации на промежуток 100% в виде плоского числа (дефолт -0.25)")
                .defineInRange("regenDelta", -0.25, -100.0, 100.0);
        OVERLOAD_GRAVITY_PERCENT = BUILDER
                .comment("Настройка гравитации на промежуток 100% в виде процента (дефолт 0.05 = +5%)")
                .defineInRange("gravityPercent", 0.05, 0.0, 10.0);
        OVERLOAD_SPEED_PERCENT = BUILDER
                .comment("Настройка снижения скорости передвижения на промежуток 100% в виде процента (дефолт 0.05 = -5%)")
                .defineInRange("speedPercent", 0.05, 0.0, 0.99);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
