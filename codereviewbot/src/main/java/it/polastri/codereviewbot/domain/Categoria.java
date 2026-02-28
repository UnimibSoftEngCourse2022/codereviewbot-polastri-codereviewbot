package it.polastri.codereviewbot.domain;

/**
 * Rappresenta la categoria di una regola o di una issue,
 * utilizzata per classificare i problemi rilevati durante l'analisi.
 */

public enum Categoria {
	NAMING,
	COMPLESSITA,
	SICUREZZA,
	STILE, 
	BEST_PRACTICE,
	BUG;
}