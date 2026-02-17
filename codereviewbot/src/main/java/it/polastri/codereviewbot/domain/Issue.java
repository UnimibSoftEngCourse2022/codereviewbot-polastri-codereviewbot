package it.polastri.codereviewbot.domain;

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
		if (fileAnalizzato == null) throw new IllegalArgumentException("FileAnalizzato non può essere null");
		if (regola == null) throw new IllegalArgumentException("RegolaAnalisi non può essere null");
		if (messaggio == null) throw new IllegalArgumentException("Messaggio non può essere null");
		if (riga <= 0) throw new IllegalArgumentException("Numero di riga non valido");
		
		this.fileAnalizzato = fileAnalizzato;
		this.riga = riga;
		this.regola = regola;
		this.messaggio = messaggio;
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
	
	// Sostituisce descrizioneCompleta() del modello di dominio
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