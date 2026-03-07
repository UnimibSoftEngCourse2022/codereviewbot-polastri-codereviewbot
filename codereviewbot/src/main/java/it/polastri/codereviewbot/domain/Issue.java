package it.polastri.codereviewbot.domain;

import java.util.Objects;

/**
 * Rappresenta una violazione di una regola di analisi rilevata
 * durante l'analisi di un file sorgente.
 */

public class Issue {
	
	private final FileAnalizzato fileAnalizzato; 
	private final int riga; 
	private final RegolaAnalisi regola; 
	private final String messaggio; 
	
	// Crea una nuova issue associata a un file analizzato.
	public Issue(FileAnalizzato fileAnalizzato,int riga, RegolaAnalisi regola, String messaggio) {
        this.fileAnalizzato = Objects.requireNonNull(fileAnalizzato, "FileAnalizzato non può essere null");
        this.regola = Objects.requireNonNull(regola, "RegolaAnalisi non può essere null");
        this.messaggio = Objects.requireNonNull(messaggio, "Messaggio non può essere null");
        if (messaggio.isBlank()) throw new IllegalArgumentException("Messaggio non può essere vuoto");
        if (riga <= 0) throw new IllegalArgumentException("Numero di riga non valido");
		
		this.riga = riga;
	}
	
	public FileAnalizzato getFileAnalizzato() {
		return fileAnalizzato;
	}

	public int getRiga() {
		return riga;
	}

	public RegolaAnalisi getRegola() {
		return regola;
	}

	public String getMessaggio() {
		return messaggio;
	}
	
	@Override
	public String toString() {
	    return "Issue{" +
	            "file=" + fileAnalizzato +
	            ", riga=" + riga +
	            ", regola=" + regola +
	            ", messaggio='" + messaggio + '\'' +
	            '}';
	}
}