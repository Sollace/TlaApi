package io.github.mattidragon.tlaapi.impl.client.fabric;

import io.github.mattidragon.tlaapi.impl.TlaApi;
import io.github.mattidragon.tlaapi.impl.fabric.FabricModdingPlatform;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class TlaApiClientFabric implements ClientModInitializer {
    public TlaApiClientFabric() {
        // We don't actually register anything here, just set internal state
        TlaApi.WORLD = () -> MinecraftClient.getInstance().world;
        FabricModdingPlatform.CLIENT_TICK_HANDLERS = handler -> {
            ClientTickEvents.START_CLIENT_TICK.register(client -> handler.run());
        };
    }

    @Override
    public void onInitializeClient() { }
}
