package net.fameless.bungee;

import com.google.inject.AbstractModule;
import net.fameless.core.BungeeAFKPlatform;
import net.fameless.core.handling.AFKHandler;

public class BungeeModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(BungeeAFKPlatform.class).toInstance(BungeePlatform.get());
        bind(AFKHandler.class).to(BungeeAFKHandler.class);
    }
}
