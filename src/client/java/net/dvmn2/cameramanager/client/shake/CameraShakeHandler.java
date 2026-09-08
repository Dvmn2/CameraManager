package net.dvmn2.cameramanager.client.shake;

import net.dvmn2.cameramanager.client.CameraManagerClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Класс отвечает за состояние тряски камеры на стороне клиента.
 * <p>
 * Поддерживает НЕСКОЛЬКО одновременных независимых тряcок: каждая живёт
 * своим таймером/затуханием, а итоговое смещение камеры — сумма вкладов
 * всех активных на данный момент тряcок. Благодаря этому, если поверх
 * длинной тряски накладывается короткая, в момент наложения амплитуды
 * складываются, а после окончания короткой — длинная продолжает трясти
 * камеру как ни в чём не бывало.
 * <p>
 * Само применение смещения к камере происходит не здесь, а в
 * {@link net.dvmn2.cameramanager.client.mixin.ShakeMixin} — этот класс только
 * хранит состояние всех активных тряcок и отдаёт суммарные значения
 * смещений по запросу.
 */
public class CameraShakeHandler {

    /**
     * Множитель для перевода "сырых" единиц позиции (из пакета/команды) в блоки мира.
     */
    private static final float POSITION_SCALE = 0.01f;

    /**
     * Общий генератор случайных чисел для расчёта дрожания.
     */
    private static final Random RANDOM = Random.create();

    /**
     * Список всех активных тряcок. Работаем с ним только на клиентском
     * main-потоке (tick() вызывается из END_CLIENT_TICK, а входящий пакет
     * оборачивается в context.client().execute(...) в ShakeModClient),
     * поэтому обычный ArrayList безопасен — синхронизация не нужна.
     */
    private static final List<ShakeInstance> shakes = new ArrayList<>();

    /**
     * Одна независимая тряска: свои амплитуды, свой прогресс и своё затухание.
     */
    private static final class ShakeInstance {
        final float rotation;
        final float position;
        final int totalDuration;
        int ticksLeft;

        ShakeInstance(int angle_delta, int position_delta, int duration) {
            this.rotation = angle_delta;
            this.position = position_delta * POSITION_SCALE;
            this.totalDuration = duration;
            this.ticksLeft = duration;
        }

        /**
         * Коэффициент затухания ЭТОЙ КОНКРЕТНОЙ тряски: 1 в начале, 0 в конце.
         * Квадратичная кривая — тряска "успокаивается" быстрее к концу.
         */
        float fadeFactor() {
            if (totalDuration <= 0) return 0f;
            float t = (float) ticksLeft / (float) totalDuration; // 1 -> 0
            return t * t;
        }
    }

    /**
     * Запускает новый, независимый эффект тряски камеры, ДОБАВЛЯЯ его
     * к уже идущим (если они есть), а не заменяя их.
     *
     * @param angle_delta    максимальный угол отклонения камеры (градусы)
     * @param position_delta максимальное смещение камеры (в "сырых" единицах, см. POSITION_SCALE)
     * @param duration       длительность эффекта в тиках
     */
    public static void start(int angle_delta, int position_delta, int duration) {
        if (duration < 0) return; // тряска отрицательной длительности не имеет смысла
        if (duration == 0) {      // считаем тряску нулевой длительности за сброс инстансов
            shakes.clear();
            return;
        }
        shakes.add(new ShakeInstance(angle_delta, position_delta, duration));
    }

    /**
     * Вызывается каждый клиентский тик (см. {@link CameraManagerClient}).
     * Уменьшает оставшееся время у КАЖДОЙ активной тряски и удаляет те,
     * что закончились. Само дрожание камеры считается "лениво", по
     * запросу, через getXxxOffset()-методы ниже.
     */
    public static void tick(MinecraftClient client) {
        if (shakes.isEmpty()) return;

        Iterator<ShakeInstance> it = shakes.iterator();
        while (it.hasNext()) {
            ShakeInstance s = it.next();
            if (s.ticksLeft > 0) {
                s.ticksLeft--;
            }
            if (s.ticksLeft <= 0) {
                it.remove();
            }
        }
    }

    /**
     * Суммарное случайное смещение по рысканию (yaw) камеры — сумма вкладов
     * всех активных тряcок, каждая со своей амплитудой и своим затуханием.
     */
    public static float getYawOffset() {
        float sum = 0f;
        for (ShakeInstance s : shakes) {
            sum += (RANDOM.nextFloat() - 0.5f) * s.rotation * s.fadeFactor();
        }
        return sum;
    }

    /**
     * Суммарное случайное смещение по тангажу (pitch) камеры.
     */
    public static float getPitchOffset() {
        float sum = 0f;
        for (ShakeInstance s : shakes) {
            sum += (RANDOM.nextFloat() - 0.5f) * s.rotation * s.fadeFactor();
        }
        return sum;
    }

    /**
     * Суммарное случайное смещение камеры вдоль локальной горизонтальной оси.
     */
    public static float getRightOffset() {
        float sum = 0f;
        for (ShakeInstance s : shakes) {
            sum += (RANDOM.nextFloat() - 0.5f) * s.position * s.fadeFactor();
        }
        return sum;
    }

    /**
     * Суммарное случайное смещение камеры вдоль локальной вертикальной оси.
     */
    public static float getUpOffset() {
        float sum = 0f;
        for (ShakeInstance s : shakes) {
            sum += (RANDOM.nextFloat() - 0.5f) * s.position * s.fadeFactor();
        }
        return sum;
    }

    /**
     * true, если есть хотя бы одна активная тряска.
     */
    public static boolean isShaking() {
        return !shakes.isEmpty();
    }
}