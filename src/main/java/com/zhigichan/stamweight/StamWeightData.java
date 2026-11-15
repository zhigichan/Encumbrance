package com.zhigichan.stamweight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class StamWeightData {
    private float currentWeight;
    private float currentStamina;
    private final transient java.util.concurrent.atomic.AtomicBoolean isWeightTaskScheduled = new java.util.concurrent.atomic.AtomicBoolean(false);
    private transient long weightTaskTargetTick = 0;

    // Константа максимального объёма шкалы атаки (20.0 очков)
    public static final float MAX_STAMINA = 20.0f;


    // --- Выносливость движения в оперативной памяти (RAM, не пишется в NBT) ---
    public static final double DEFAULT_ENDURANCE = 20.0;
    private double endurance = DEFAULT_ENDURANCE;
    private transient double maxEndurance = DEFAULT_ENDURANCE;
    private transient double weightMaxPenalty = 0.0;
    private transient int enduranceCooldown = 0;

    private transient int lastFoodLevel = -1;
    private transient int lastThirstLevel = -1;

    // Флаги состояния в оперативной памяти
    private transient boolean isWalking = false;
    private transient boolean wasSprinting = false;
    private transient boolean isFatigued = false;   // Усталость 1 (текущая стамина 0.0 -> 6.0)
    private transient boolean isEmaciated = false;  // Измождение 2 (макс. стамина <= 6.0 от веса)

    // Кэш последней синхронизации по сети
    private transient int lastSyncedCur = -1;
    private transient int lastSyncedMax = -1;

    private transient double survivalActionMultiplier = 1.0;

    public double getSurvivalActionMultiplier() { return this.survivalActionMultiplier; }
    public void setSurvivalActionMultiplier(double multiplier) { this.survivalActionMultiplier = multiplier; }

    public int getLastFoodLevel() { return this.lastFoodLevel; }
    public void setLastFoodLevel(int food) { this.lastFoodLevel = food; }

    public int getLastThirstLevel() { return this.lastThirstLevel; }
    public void setLastThirstLevel(int thirst) { this.lastThirstLevel = thirst; }

    public StamWeightData() {
        this.currentWeight = 0.0f;
        this.currentStamina = MAX_STAMINA;
    }

    public StamWeightData(float currentWeight, float currentStamina, double endurance) {
        this.currentWeight = currentWeight;
        this.currentStamina = currentStamina;
        this.endurance = endurance;
    }

    // 👑 МЕТОДЫ ШКАЛЫ АТАКИ
    public void gainStamina(float points) {
        this.currentStamina = Math.min(MAX_STAMINA, this.currentStamina + points);
    }

    public void resetStamina() {
        this.currentStamina = 0.0f;
    }

    public float getCurrentWeight() { return this.currentWeight; }
    public void setCurrentWeight(float weight) { this.currentWeight = weight; }

    public float getCurrentStamina() { return this.currentStamina; }
    public void setCurrentStamina(float stamina) { this.currentStamina = stamina; }

    public java.util.concurrent.atomic.AtomicBoolean getWeightTaskScheduled() { return this.isWeightTaskScheduled; }
    public long getWeightTaskTargetTick() { return this.weightTaskTargetTick; }
    public void setWeightTaskTargetTick(long tick) { this.weightTaskTargetTick = tick; }

    // --- Геттеры и сеттеры выносливости и флагов (RAM) ---
    public double getEndurance() { return this.endurance; }
    public void setEndurance(double endurance) { this.endurance = endurance; }

    public double getMaxEndurance() { return this.maxEndurance; }
    public void setMaxEndurance(double maxEndurance) { this.maxEndurance = maxEndurance; }

    public double getWeightMaxPenalty() { return this.weightMaxPenalty; }
    public void setWeightMaxPenalty(double penalty) { this.weightMaxPenalty = penalty; }

    public int getEnduranceCooldown() { return this.enduranceCooldown; }
    public void setEnduranceCooldown(int cooldown) { this.enduranceCooldown = cooldown; }

    public boolean isWalking() { return this.isWalking; }
    public void setWalking(boolean walking) { this.isWalking = walking; }

    public boolean wasSprinting() { return this.wasSprinting; }
    public void setWasSprinting(boolean wasSprinting) { this.wasSprinting = wasSprinting; }

    public boolean isFatigued() { return this.isFatigued; }
    public void setFatigued(boolean fatigued) { this.isFatigued = fatigued; }

    public boolean isEmaciated() { return this.isEmaciated; }
    public void setEmaciated(boolean emaciated) { this.isEmaciated = emaciated; }

    public int getLastSyncedCur() { return this.lastSyncedCur; }
    public void setLastSyncedCur(int cur) { this.lastSyncedCur = cur; }

    public int getLastSyncedMax() { return this.lastSyncedMax; }
    public void setLastSyncedMax(int max) { this.lastSyncedMax = max; }

    public static final Codec<StamWeightData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.FLOAT.fieldOf("weight").forGetter(StamWeightData::getCurrentWeight),
                    Codec.FLOAT.fieldOf("stamina").forGetter(StamWeightData::getCurrentStamina),
                    Codec.DOUBLE.optionalFieldOf("endurance", DEFAULT_ENDURANCE).forGetter(StamWeightData::getEndurance)
            ).apply(instance, StamWeightData::new)
    );
}
