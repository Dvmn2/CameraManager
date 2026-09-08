package net.dvmn2.cameramanager.client;

import net.dvmn2.cameramanager.client.shake.CameraShakeHandler;
import net.dvmn2.cameramanager.client.shake.CameraShakePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class CameraManagerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Регистрируем тип пакета для направления сервер -> клиент (S2C).
        // Без этого шага клиент не сможет корректно декодировать входящие байты.
        PayloadTypeRegistry.playS2C().register(CameraShakePayload.ID, CameraShakePayload.CODEC);

        // Обработчик входящего пакета: при получении запускаем тряску камеры.
        // context.client().execute(...) выполняет код в основном потоке клиента,
        // так как сетевые колбэки могут прилетать из другого потока, а изменять
        // состояние игры безопасно только из main-потока.
        ClientPlayNetworking.registerGlobalReceiver(CameraShakePayload.ID, (payload, context) ->
                context.client().execute(() ->
                        CameraShakeHandler.start(payload.angle_delta(), payload.position_delta(), payload.duration())));

        // На каждый клиентский тик уменьшаем оставшееся время тряски.
        ClientTickEvents.END_CLIENT_TICK.register(CameraShakeHandler::tick);
    }
}
