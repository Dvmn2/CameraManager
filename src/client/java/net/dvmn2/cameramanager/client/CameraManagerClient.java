package net.dvmn2.cameramanager.client;

import net.dvmn2.cameramanager.client.shake.CameraShakeHandler;
import net.dvmn2.cameramanager.client.shake.CameraShakePayload;
import net.dvmn2.cameramanager.client.shake.CameraShakeStopPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Точка входа клиентской части мода. Регистрирует сетевые пакеты (payload'ы),
 * обработчики входящих пакетов и тиковую логику тряски камеры.
 * <p>
 * Client-side entrypoint. Registers network payloads, incoming packet
 * handlers, and the camera-shake tick logic.
 */
public class CameraManagerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Регистрируем типы пакетов S2C (сервер -> клиент) с их кодеками.
        // Register S2C (server -> client) payload types with their codecs.
        PayloadTypeRegistry.playS2C().register(CameraShakePayload.ID, CameraShakePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CameraShakeStopPayload.ID, CameraShakeStopPayload.CODEC);

        // При получении пакета "начать тряску" — выполняем на клиентском потоке
        // и запускаем новый ShakeInstance в обработчике.
        //
        // On receiving the "start shake" packet — run on the client thread and
        // start a new ShakeInstance in the handler.
        ClientPlayNetworking.registerGlobalReceiver(CameraShakePayload.ID, (payload, context) ->
                context.client().execute(() ->
                        CameraShakeHandler.start(payload.angle_delta(), payload.position_delta(), payload.duration())));

        // Пакет "остановить тряску" — очищает все активные тряски сразу.
        // "Stop shake" packet — clears all active shakes immediately.
        ClientPlayNetworking.registerGlobalReceiver(CameraShakeStopPayload.ID, (payload, context) ->
                context.client().execute(CameraShakeHandler::stopAll));

        // Тикаем состояние тряски вместе с игровым тиком клиента (20 раз в секунду),
        // чтобы длительность тряски не зависела от FPS.
        //
        // Tick the shake state alongside the client's game tick (20/s) so shake
        // duration doesn't depend on the render framerate.
        ClientTickEvents.END_CLIENT_TICK.register(CameraShakeHandler::tick);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CameraShakeHandler.stopAll());
    }
}