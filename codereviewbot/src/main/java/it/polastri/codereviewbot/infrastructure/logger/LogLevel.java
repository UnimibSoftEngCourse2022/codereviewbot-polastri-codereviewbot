package it.polastri.codereviewbot.infrastructure.logger;

/**
 * Classe che rappresenta i livelli di severità dei messaggi di log.
 */

public enum LogLevel {

    /**
     * Messaggi informativi relativi al normale flusso di esecuzione
     * dell'applicazione (es. avvio, completamento di un'operazione).
     */
    INFO,

    // Messaggi che segnalano situazioni anomale ma non bloccanti.
    WARNING,

    //  Messaggi che indicano errori gravi verificati durante l'esecuzione dell'applicazione.
    ERROR
}