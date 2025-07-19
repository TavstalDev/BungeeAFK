package net.fameless.spigot;

import com.google.inject.AbstractModule;
import net.fameless.core.BungeeAFKPlatform;
import net.fameless.core.handling.AFKHandler;

public class SpigotModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(BungeeAFKPlatform.class).toInstance(SpigotPlatform.get());
        bind(AFKHandler.class).toInstance(new SpigotAFKHandler());
    }

}
