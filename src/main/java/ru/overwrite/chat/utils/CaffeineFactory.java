// Original - https://github.com/LuckPerms/LuckPerms/blob/master/common/src/main/java/me/lucko/luckperms/common/util/CaffeineFactory.java

package ru.overwrite.chat.utils;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public final class CaffeineFactory {

    private CaffeineFactory() {
    }

    @Getter
    private static ForkJoinPool loaderPool;

    public static void initialize() {
        if (loaderPool == null) {
            loaderPool = new ForkJoinPool();
        }
    }

    public static void shutdown() {
        if (loaderPool != null) {
            loaderPool.shutdown();
            loaderPool = null;
        }
    }

    public static Caffeine<Object, Object> newBuilder() {
        if (loaderPool == null) {
            initialize();
        }
        return Caffeine.newBuilder().executor(loaderPool);
    }

    public static Executor executor() {
        if (loaderPool == null) {
            initialize();
        }
        return loaderPool;
    }
}
