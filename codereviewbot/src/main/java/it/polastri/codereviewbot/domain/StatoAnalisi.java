package it.polastri.codereviewbot.domain;

/**
 * Rappresenta lo stato del ciclo di vita di un'analisi.
 */

public enum StatoAnalisi {
	CREATA,
	IN_ESECUZIONE,
	COMPLETATA,
	FALLITA; 
}
