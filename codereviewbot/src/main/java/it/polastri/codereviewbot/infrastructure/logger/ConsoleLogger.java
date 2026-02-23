package it.polastri.codereviewbot.infrastructure.logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Implementazione concreta dell'interfaccia Logger che scrive i messaggi di log sulla console standard.
 *
 * Ogni messaggio viene arricchito con timestamp di emissione e livello di severità.
 */

//Uso intenzionale di System.out: è il logger di base
@SuppressWarnings("java:S106") 
public class ConsoleLogger implements Logger {

    // Formatter utilizzato per la rappresentazione del timestamp.
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    //  Registra il messaggio di log sulla console standard, inclusi timestamp e livello di severità.
    @Override
    public void log(LogLevel level, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        System.out.println("[" + timestamp + "] [" + level + "] " + message);
    }
}