package it.polastri.codereviewbot.infrastructure.loader;

import it.polastri.codereviewbot.domain.Progetto;

/**
 * Componente infrastrutturale responsabile del caricamento di un Progetto.
 *
 * Il ProjectLoader:
 * - interpreta una sorgente esterna (es. filesystem, repository)
 * - individua i file sorgente del progetto
 * - costruisce l'oggetto di dominio Progetto con i relativi FileSorgente
 */

public interface ProjectLoader {
	
    Progetto caricaProgetto(String path);
}