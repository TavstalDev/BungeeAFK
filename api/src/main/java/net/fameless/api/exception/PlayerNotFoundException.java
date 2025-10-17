package net.fameless.api.exception;

import java.io.Serial;

public class PlayerNotFoundException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public PlayerNotFoundException(String message) {
        super(message);
    }

    public PlayerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public PlayerNotFoundException(Throwable cause) {
        super(cause);
    }
}
