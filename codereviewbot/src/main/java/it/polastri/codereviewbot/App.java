package it.polastri.codereviewbot;

import it.polastri.codereviewbot.infrastructure.logger.ConsoleLogger;
import it.polastri.codereviewbot.infrastructure.logger.Logger;

/**
 * Classe di avvio dell'applicazione.
 */
public class App {

    public static void main(String[] args) {
        Logger logger = new ConsoleLogger();
        logger.info("Applicazione avviata");
    }
}