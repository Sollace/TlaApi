package io.github.mattidragon.tlaapi.impl.client.neoforge;

import io.github.mattidragon.tlaapi.impl.TlaApi;
import io.github.mattidragon.tlaapi.impl.neoforge.NeoforgeModdingPlatform;
import net.minecraft.client.MinecraftClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@Mod(value = TlaApi.DEFAULT_NAMESPACE, dist = Dist.CLIENT)
public class TlaApiClientNeoforge {
    public TlaApiClientNeoforge(IEventBus bus, ModContainer modContainer) {
        // We don't actually register anything here, just set internal state
        TlaApi.WORLD = () -> MinecraftClient.getInstance().world;
        bus.register(this);
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Pre event) {
        NeoforgeModdingPlatform.CLIENT_TICK_HANDLERS.forEach(Runnable::run);
    }
}
