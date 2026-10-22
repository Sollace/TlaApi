package io.github.mattidragon.tlaapi.impl.client.fabric;

import io.github.mattidragon.tlaapi.impl.TlaApi;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

public class TlaApiClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TlaApi.WORLD = () -> MinecraftClient.getInstance().world;
    }
}
