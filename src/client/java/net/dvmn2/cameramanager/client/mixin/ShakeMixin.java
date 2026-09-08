package net.dvmn2.cameramanager.client.mixin;

import net.dvmn2.cameramanager.client.shake.CameraShakeHandler;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин к {@link Camera}, применяющий эффект тряски ПОСЛЕ того, как
 * ванильная логика уже полностью обновила положение и поворот камеры
 * (инъекция в TAIL метода update()).
 * <p>
 * Мы не пересчитываем камеру заново, а лишь добавляем небольшое случайное
 * смещение поверх уже вычисленных ванильных значений — это самый безопасный
 * способ не сломать взаимодействие с другими модами/ванильной логикой камеры
 * (третье лицо, инвертированный вид и т.д.).
 */
@Mixin(Camera.class)
public abstract class ShakeMixin {

    @Shadow
    public abstract float getYaw();

    @Shadow
    public abstract float getPitch();

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    public abstract Vec3d getCameraPos();

    @Shadow
    protected abstract void setPos(double x, double y, double z);

    /**
     * Единичный вектор "вправо" относительно текущей ориентации камеры.
     */
    @Shadow
    public abstract Vector3fc getHorizontalPlane();

    /**
     * Единичный вектор "вверх" относительно текущей ориентации камеры.
     */
    @Shadow
    public abstract Vector3fc getVerticalPlane();

    @Inject(method = "update", at = @At("TAIL"))
    private void applyShake(World area, Entity focusedEntity, boolean thirdPerson,
                            boolean inverseView, float tickDelta, CallbackInfo ci) {
        // Если тряска сейчас не активна — ничего не делаем и не трогаем камеру,
        // чтобы не тратить случайные числа и не создавать дрожание "по ошибке".
        if (!CameraShakeHandler.isShaking()) return;

        // --- Тряска угла обзора ---
        float yawOffset = CameraShakeHandler.getYawOffset();
        float pitchOffset = CameraShakeHandler.getPitchOffset();
        this.setRotation(this.getYaw() + yawOffset, this.getPitch() + pitchOffset);

        // --- Тряска позиции ---
        // Смещаем камеру вдоль её ЛОКАЛЬНЫХ осей (right/up), а не мировых
        // (x/y/z), чтобы эффект выглядел как дрожание "в руках" игрока,
        // а не как хаотичный сдвиг по карте вне зависимости от того, куда
        // игрок смотрит.
        Vector3fc right = this.getHorizontalPlane();
        Vector3fc up = this.getVerticalPlane();
        float rightOffset = CameraShakeHandler.getRightOffset();
        float upOffset = CameraShakeHandler.getUpOffset();

        Vec3d pos = this.getCameraPos();
        double dx = right.x() * rightOffset + up.x() * upOffset;
        double dy = right.y() * rightOffset + up.y() * upOffset;
        double dz = right.z() * rightOffset + up.z() * upOffset;

        this.setPos(pos.x + dx, pos.y + dy, pos.z + dz);
    }
}