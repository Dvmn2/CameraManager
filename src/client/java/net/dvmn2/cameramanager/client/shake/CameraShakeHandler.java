package net.dvmn2.cameramanager.client.shake;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Хранит и обсчитывает все активные "тряски камеры" на клиенте.
 * Каждый ShakeInstance затухает линейно-квадратично (fadeFactor) от 1 до 0
 * за время своей длительности в тиках.
 * <p>
 * Все методы статические и вызываются только с клиентского (main) потока —
 * из ClientTickEvents и из миксина в Camera#update, поэтому синхронизация
 * коллекции {@link #shakes} не требуется.
 * <p>
 * Holds and computes all active client-side "camera shakes". Each
 * ShakeInstance fades out (fadeFactor) from 1 to 0 over its duration in ticks.
 * <p>
 * All methods are static and only ever called from the client main thread
 * (ClientTickEvents and the Camera#update mixin), so the {@link #shakes}
 * collection does not need synchronization.
 */
public class CameraShakeHandler {

    // Множитель, переводящий "position_delta" (целое число из команды/пакета)
    // в реальное смещение камеры в блоках.
    //
    // Multiplier converting "position_delta" (an integer from the command/packet)
    // into an actual camera offset in blocks.
    private static final float POSITION_SCALE = 0.01f;

    private static final Random RANDOM = Random.create();

    private static final List<ShakeInstance> shakes = new ArrayList<>();

    /**
     * Одна активная тряска со своим углом, смещением и оставшимся временем жизни.
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
         * Коэффициент затухания: 1.0 в начале, 0.0 в конце (по квадратичной кривой).
         */
        float fadeFactor() {
            if (totalDuration <= 0) return 0f;
            float t = (float) ticksLeft / (float) totalDuration;
            return t * t;
        }
    }

    /**
     * Запускает новую тряску. Тряски с duration <= 0 игнорируются.
     */
    public static void start(int angle_delta, int position_delta, int duration) {
        if (duration <= 0) return;
        shakes.add(new ShakeInstance(angle_delta, position_delta, duration));
    }

    /**
     * Немедленно останавливает все активные тряски (используется и по сетевой команде, и при дисконнекте).
     */
    public static void stopAll() {
        shakes.clear();
    }

    /**
     * Вызывается каждый игровой тик клиента — уменьшает оставшееся время жизни тряски и удаляет завершённые.
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

    // Ниже — четыре независимых источника случайного смещения (yaw/pitch/right/up).
    // Below — four independent random offset sources (yaw/pitch/right/up).

    public static float getYawOffset() {
        float sum = 0f;
        for (ShakeInstance s : shakes) {
            sum += (RANDOM.nextFloat() - 0.5f) * s.rotation * s.fadeFactor();
        }
        return sum;
    }

    public static float getPitchOffset() {
        float sum = 0f;
        for (ShakeInstance s : shakes) {
            sum += (RANDOM.nextFloat() - 0.5f) * s.rotation * s.fadeFactor();
        }
        return sum;
    }

    public static float getRightOffset() {
        float sum = 0f;
        for (ShakeInstance s : shakes) {
            sum += (RANDOM.nextFloat() - 0.5f) * s.position * s.fadeFactor();
        }
        return sum;
    }

    public static float getUpOffset() {
        float sum = 0f;
        for (ShakeInstance s : shakes) {
            sum += (RANDOM.nextFloat() - 0.5f) * s.position * s.fadeFactor();
        }
        return sum;
    }

    public static boolean isShaking() {
        return !shakes.isEmpty();
    }
}