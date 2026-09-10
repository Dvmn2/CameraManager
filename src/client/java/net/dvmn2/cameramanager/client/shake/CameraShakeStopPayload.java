package net.dvmn2.cameramanager.client.shake;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Пакет "остановить тряску камеры" (S2C). Не несёт данных — на сервере
 * (ShakeCommand#stop) отправляется byte[0], здесь используется PacketCodec.unit(...),
 * который не читает и не пишет ни одного байта. Идентификатор "cameramanager:shake_stop"
 * ДОЛЖЕН совпадать с ShakeCommand.CHANNEL_STOP на сервере.
 * <p>
 * "Stop camera shake" packet (S2C). Carries no data — the server
 * (ShakeCommand#stop) sends byte[0], and PacketCodec.unit(...) here reads/writes
 * zero bytes accordingly. The "cameramanager:shake_stop" identifier MUST match
 * ShakeCommand.CHANNEL_STOP on the server.
 */
public record CameraShakeStopPayload() implements CustomPayload {

    public static final Id<CameraShakeStopPayload> ID =
            new Id<>(Identifier.of("cameramanager", "shake_stop"));

    public static final PacketCodec<PacketByteBuf, CameraShakeStopPayload> CODEC =
            PacketCodec.unit(new CameraShakeStopPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}