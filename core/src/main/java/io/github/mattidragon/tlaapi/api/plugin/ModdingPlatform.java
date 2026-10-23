package io.github.mattidragon.tlaapi.api.plugin;

import java.util.List;

import io.github.mattidragon.tlaapi.impl.TlaApi;

public interface ModdingPlatform {
    static ModdingPlatform getInstance() {
        return TlaApi.PLATFORM;
    }

    String getPlatformName();

    <T> List<T> getEntrypoints(String name, Class<T> type);

    long getBucketAmount();

    void registerClientTickHandler(Runnable handler);
}
