package it.polastri.codereviewbot.cli;

import it.polastri.codereviewbot.infrastructure.logger.ConsoleLogger;
import it.polastri.codereviewbot.infrastructure.logger.Logger;

/**
 * Punto di ingresso dell'applicazione da linea di comando (CLI).
 */
public class Main {

    public static void main(String[] args) {

        Logger logger = new ConsoleLogger();
        logger.info("CodeReviewBot avviato");

        // Qui in futuro potrai aggiungere il parsing degli argomenti CLI
        // e l'invocazione dei casi d'uso UC1 e UC2.
    }
}