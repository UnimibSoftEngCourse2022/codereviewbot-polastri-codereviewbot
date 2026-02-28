package it.polastri.codereviewbot.infrastructure.logger;

/**
 * Interfaccia che definisce il contratto per il logging dell'applicazione.
 *
 * Incapsula la gestione dell'output dei messaggi di log, evitando l'uso diretto di System.out/System.err.
 */

public interface Logger {

    // Registra un messaggio di log associato a un determinato livello di severità.
    void log(LogLevel level, String message);

    // Registra un messaggio informativo.
    default void info(String message) {
        log(LogLevel.INFO, message);
    }

    // Registra un messaggio di warning.
    default void warning(String message) {
        log(LogLevel.WARNING, message);
    }

    // Registra un messaggio di errore.
    default void error(String message) {
        log(LogLevel.ERROR, message);
    }

    // Registra un messaggio di errore associato a un'eccezione.
    default void error(String message, Throwable t) {
        log(LogLevel.ERROR, message + " - " + t.getMessage());
    }
}