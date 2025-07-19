package net.fameless.core.handling;

public interface AFKHandler {

    void init();

    void shutdown();

    void setAction(Action action);

    void setWarnDelayMillis(long delay);

    void setActionDelayMillis(long delay);

    void setAfkDelayMillis(long delay);

    long getWarnDelayMillis();

    long getAfkDelayMillis();

    long getActionDelayMillis();

    void updateConfigValues();

}
