package net.dvmn2.cameramanager.client.shake;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Пакет "начать тряску камеры" (S2C). Идентификатор "cameramanager:shake"
 * ДОЛЖЕН совпадать с каналом ShakeCommand.CHANNEL_ADD на сервере, а порядок
 * writeInt/readInt — с порядком записи в ShakeCommand#add (angle, position, duration).
 * <p>
 * "Start camera shake" packet (S2C). The "cameramanager:shake" identifier
 * MUST match ShakeCommand.CHANNEL_ADD on the server, and the writeInt/readInt
 * order must match ShakeCommand#add's write order (angle, position, duration).
 */
public record CameraShakePayload(int angle_delta, int position_delta, int duration) implements CustomPayload {

    public static final Id<CameraShakePayload> ID =
            new Id<>(Identifier.of("cameramanager", "shake"));

    public static final PacketCodec<PacketByteBuf, CameraShakePayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeInt(payload.angle_delta());
                buf.writeInt(payload.position_delta());
                buf.writeInt(payload.duration());
            },
            buf -> new CameraShakePayload(buf.readInt(), buf.readInt(), buf.readInt())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}