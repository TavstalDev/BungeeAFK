package net.fameless.bungee;

import com.google.inject.AbstractModule;
import net.fameless.core.BungeeAFKPlatform;
import net.fameless.core.handling.AFKHandler;
import net.fameless.core.inject.PlatformConfig;

public class BungeeModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(BungeeAFKPlatform.class).toInstance(BungeePlatform.get());
        bind(PlatformConfig.class).toInstance(new BungeeConfig());
        bind(AFKHandler.class).toInstance(new BungeeAFKHandler());
    }
}
