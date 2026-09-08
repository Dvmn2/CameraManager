package net.dvmn2.cameramanager.client.shake;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Сетевой пакет направления сервер -> клиент (S2C), которым сервер сообщает
 * клиенту параметры тряски камеры.
 * <p>
 * ВАЖНО: идентификатор "cameramanager:shake" должен совпадать с каналом, который
 * использует серверный плагин (см. {@code ShakeCommand.CHANNEL} в модуле
 * плагина), так как плагин отправляет "сырые" байты через Bukkit Plugin
 * Messaging API, а не напрямую через Fabric Networking API — сервер ведь
 * не является Fabric-сервером, это Paper.
 *
 * @param angle_delta    максимальный угол отклонения камеры (градусы)
 * @param position_delta максимальное смещение камеры (сырые единицы, см. CameraShakeHandler.POSITION_SCALE)
 * @param duration       длительность эффекта в тиках
 */
public record CameraShakePayload(int angle_delta, int position_delta, int duration) implements CustomPayload {

    /**
     * Идентификатор пакета — используется и при регистрации на клиенте, и как имя канала на сервере.
     */
    public static final Id<CameraShakePayload> ID =
            new Id<>(Identifier.of("cameramanager", "shake"));

    /**
     * Кодек сериализации/десериализации пакета.
     * Порядок записи/чтения полей должен строго совпадать с порядком,
     * в котором сервер пишет данные через ByteArrayDataOutput в ShakeCommand,
     * иначе значения перепутаются местами без каких-либо явных ошибок.
     */
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