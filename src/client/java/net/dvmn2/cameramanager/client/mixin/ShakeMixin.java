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
 * Миксин в Camera#update: после стандартного пересчёта позиции/поворота камеры
 * (в конце метода — @At("TAIL")) добавляет случайное смещение угла и позиции,
 * если в CameraShakeHandler есть активные тряски.
 * <p>
 * ВАЖНО: имена @Shadow-методов ниже (getYaw/getPitch/setRotation/getCameraPos/
 * setPos/getHorizontalPlane/getVerticalPlane) и сигнатура "update" зависят от
 * используемых маппингов (Yarn/Mojmap) и версии Minecraft. При смене версии
 * стоит перепроверить их через genSources/сборку — иначе сборка просто не
 * скомпилируется, миксин не является "тихим" источником багов.
 * <p>
 * Mixin into Camera#update: after the vanilla position/rotation recompute
 * (at the end of the method — @At("TAIL")), adds a random rotation/position
 * offset whenever CameraShakeHandler has active shakes.
 * <p>
 * IMPORTANT: the @Shadow method names below (getYaw/getPitch/setRotation/
 * getCameraPos/setPos/getHorizontalPlane/getVerticalPlane) and the "update"
 * signature depend on the mappings (Yarn/Mojmap) and Minecraft version in use.
 * Re-verify them via genSources/a build whenever you bump the MC version —
 * otherwise the build simply won't compile, this mixin isn't a silent source
 * of bugs.
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

    @Shadow
    public abstract Vector3fc getHorizontalPlane();

    @Shadow
    public abstract Vector3fc getVerticalPlane();

    @Inject(method = "update", at = @At("TAIL"))
    private void applyShake(World area, Entity focusedEntity, boolean thirdPerson,
                            boolean inverseView, float tickDelta, CallbackInfo ci) {
        // Если тряски нет — ничего не делаем, камера остаётся в исходном положении.
        // If there's no active shake — do nothing, keep the camera as-is.
        if (!CameraShakeHandler.isShaking()) return;

        // Смещение поворота камеры (в градусах).
        // Camera rotation offset (in degrees).
        float yawOffset = CameraShakeHandler.getYawOffset();
        float pitchOffset = CameraShakeHandler.getPitchOffset();
        this.setRotation(this.getYaw() + yawOffset, this.getPitch() + pitchOffset);

        // Смещение позиции камеры вдоль локальных осей "вправо" и "вверх"
        // относительно текущей ориентации камеры (уже с учётом нового поворота выше не пересчитывается,
        // используются плоскости, посчитанные до применения нового rotation в этом тике —
        // приемлемо, т.к. угол смещения обычно небольшой и эффект незаметен на глаз).
        //
        // Camera position offset along the local "right" and "up" axes relative
        // to the camera's current orientation (these planes are taken before this
        // tick's new rotation is applied above — acceptable since the shake angle
        // is small and the effect isn't visually noticeable).
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