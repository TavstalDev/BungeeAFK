package net.fameless.core;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import net.fameless.core.caption.Caption;
import net.fameless.core.caption.Language;
import net.fameless.core.command.framework.Command;
import net.fameless.core.config.PluginConfig;
import net.fameless.core.handling.AFKHandler;
import net.fameless.core.util.PluginUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BungeeAFK {

    private static final Logger LOGGER = LoggerFactory.getLogger("BungeeAFK/" + BungeeAFK.class.getSimpleName());
    private static boolean initialized = false;
    private static BungeeAFKPlatform platform;
    private static Injector injector;
    private static AFKHandler afkHandler;

    public static synchronized void initCore(AbstractModule platformModule) {
        if (initialized) {
            throw new RuntimeException("You may not initialize another instance of BungeeAFK Core.");
        }

        injector = Guice.createInjector(
                Stage.PRODUCTION,
                platformModule
        );

        PluginConfig.init();
        PluginUpdater.runTask();

        platform = injector.getInstance(BungeeAFKPlatform.class);
        afkHandler = injector.getInstance(AFKHandler.class);

        Command.init();

        Caption.loadDefaultLanguages();
        Caption.setCurrentLanguage(Language.ofIdentifier(PluginConfig.get().getString("lang", "en")));

        initialized = true;
        LOGGER.info("BungeeAFK Core initialized successfully.");
    }

    public static void handleShutdown() {
        if (!initialized) return;
        Caption.saveToFile();
        PluginConfig.handleShutdown();
        afkHandler.shutdown();
    }

    public static Injector injector() {
        return injector;
    }

    public static BungeeAFKPlatform platform() {
        return platform;
    }

}
