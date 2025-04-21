package net.fameless.velocity;

import com.google.inject.AbstractModule;
import net.fameless.core.BungeeAFKPlatform;
import net.fameless.core.handling.AFKHandler;
import net.fameless.core.inject.PlatformConfig;

public class VelocityModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(BungeeAFKPlatform.class).toInstance(VelocityPlatform.get());
        bind(PlatformConfig.class).toInstance(new VelocityConfig(VelocityPlatform.getDataDirectory()));
        bind(AFKHandler.class).toInstance(new VelocityAFKHandler());
    }
}
