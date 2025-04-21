package net.fameless.core.handling;

public interface AFKHandler {

    void init();

    void setAction(Action action);

    void setActionDelayMillis(long delay);

    void setAfkDelayMillis(long delay);

    long getAfkDelayMillis();

}
