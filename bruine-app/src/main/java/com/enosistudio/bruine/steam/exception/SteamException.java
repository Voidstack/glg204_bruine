package com.enosistudio.bruine.steam.exception;

public class SteamException extends Exception {
    public SteamException() {
        super();
    }

    public SteamException(String message) {
        super(message);
    }

    public SteamException(String message, Throwable cause) {
        super(message, cause);
    }
}
