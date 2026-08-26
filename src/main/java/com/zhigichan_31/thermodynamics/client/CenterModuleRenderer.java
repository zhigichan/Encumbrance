package com.zhigichan_31.thermodynamics.client;

import com.zhigichan_31.thermodynamics.data.PlayerData;
import com.zhigichan_31.thermodynamics.systems.ModConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class CenterModuleRenderer {

    // КЭШ СТРОК: Создаем массив от 0% до 200%
    private static final String[] WEIGHT_CACHE = new String[201]; static { for (int i = 0; i <= 200; i++) { WEIGHT_CACHE[i] = i + "%"; } }

    public static void render(GuiGraphics graphics, Font font, int centerX, int height, PlayerData data, ResourceLocation texture) {
        // ТОЧНЫЕ КООРДИНАТЫ РАЗМЕЩЕНИЯ НА ЭКРАНЕ
        // ИСПРАВЛЕНО: Сдвинули на 2 пикселя влево (было 96, стало 94) для идеального выравнивания над рамкой
        int baseModuleX = centerX + 94;
        int baseModuleY = height - 36;

        // Расчет процентов веса
        float maxWeight = ModConfig.MAX_CARRY_WEIGHT.get().floatValue();
        float weightPercentRaw = maxWeight > 0 ? (data.getWeight() / maxWeight) : 0f;
        int weightPercent = (int) (weightPercentRaw * 100f);

        // БЕЗОПАСНОЕ ПОЛУЧЕНИЕ СТРОКИ ИЗ КЭША
        String weightDisplayStr;
        if (weightPercent >= 0 && weightPercent <= 200) {
            weightDisplayStr = WEIGHT_CACHE[weightPercent];
        } else {
            weightDisplayStr = weightPercent + "%";
        }

        // НАЧАЛО ОТРИСОВКИ СИСТЕМЫ ВЕСА
        graphics.pose().pushPose();
        graphics.pose().translate(baseModuleX, baseModuleY, 0);

        // ОТРИСОВКА ВАШЕЙ ФИКСИРОВАННОЙ ТЕКСТУРЫ (U=0, V=121, Ширина=23, Высота=15)
        graphics.blit(texture, 0, 0, 0.0F, 121.0F, 23, 15, 256, 256);

        // ИСПРАВЛЕНО: ТОЧНАЯ ЦЕНТРОВКА ЦИФР ПРОЦЕНТОВ ПО ОСЯМ X И Y
        graphics.pose().pushPose();

        // Масштаб шрифта 0.55f уменьшает стандартную высоту букв (9 пикселей) примерно до 5 пикселей.
        // Чтобы отцентрировать их внутри плашки высотой 15 пикселей:
        // По X: Ровно середина плашки (23 / 2 = 11.5f).
        // По Y: Опускаем на 5.2f пикселей, чтобы текст лежал идеально по центру синего поля.
        graphics.pose().translate(11.5f, 7.2f, 0.0f);
        graphics.pose().scale(0.55f, 0.55f, 1.0f);

        // Вычисляем половину ширины текста с учётом масштаба для центровки по оси X
        int textOffsetX = -(font.width(weightDisplayStr) / 2);

        // Рендерим текст с тенью ровно по центру плашки
        graphics.drawString(font, weightDisplayStr, textOffsetX, 0, 0xFFFFFF, true);
        graphics.pose().popPose();

        graphics.pose().popPose(); // КОНЕЦ ОТРИСОВКИ
    }
}
