package net.fameless.velocity;

import com.google.inject.AbstractModule;
import net.fameless.core.BungeeAFKPlatform;
import net.fameless.core.handling.AFKHandler;

public class VelocityModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(BungeeAFKPlatform.class).toInstance(VelocityPlatform.get());
        bind(AFKHandler.class).to(VelocityAFKHandler.class);
    }
}
