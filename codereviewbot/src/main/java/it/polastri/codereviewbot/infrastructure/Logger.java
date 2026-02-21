package it.polastri.codereviewbot.infrastructure;

import it.polastri.codereviewbot.domain.Analisi;

/**
 * Componente infrastrutturale per la registrazione di messaggi di log.
 *
 * Il Logger è utilizzato per:
 * - tracciare eventi significativi dell'esecuzione
 * - registrare warning ed errori non bloccanti
 */

public interface Logger {
	
    void logAvvioAnalisi(Analisi analisi);
}