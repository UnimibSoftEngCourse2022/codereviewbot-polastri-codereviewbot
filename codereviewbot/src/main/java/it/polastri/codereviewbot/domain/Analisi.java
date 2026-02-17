package it.polastri.codereviewbot.domain;

/**
 * Rappresenta un'analisi di qualità del codice eseguita su un progetto.
 * Gestisce il ciclo di vita dell'analisi, i file analizzati, le issue rilevate
 * e il risultato finale dell'analisi.
 */

import java.time.LocalDateTime; 
import java.util.List; 
import java.util.ArrayList; 
import java.util.Collections; 

public class Analisi {
	
	private final String id; 
	private final LocalDateTime dataOra; 
	private final Progetto progetto; 
	private StatoAnalisi statoAnalisi; 
	private final List<FileAnalizzato> fileAnalizzati = new ArrayList<>(); 
	private final List<Issue> issues = new ArrayList<>(); 
	private RisultatoAnalisi risultato;
	
	public Analisi(String id, Progetto progetto) {
	    if (id == null) throw new IllegalArgumentException("Id analisi non può essere null");
	    if (progetto == null) throw new IllegalArgumentException("Progetto non può essere null");
	    
		this.id = id;
		this.dataOra = LocalDateTime.now();
		this.progetto = progetto;
		this.statoAnalisi = StatoAnalisi.CREATA;
		this.risultato = null; 
	}

	public String getId() {
		return id;
	}
	
	public LocalDateTime getDataOra() {
		return dataOra;
	}
	
	public Progetto getProgetto() {
		return progetto;
	}
	
	public StatoAnalisi getStatoAnalisi() {
		return statoAnalisi;
	}
	
	public List<FileAnalizzato> getFileAnalizzati() {
		return Collections.unmodifiableList(fileAnalizzati);
	}
	
	public List<Issue> getIssues() {
		return Collections.unmodifiableList(issues);
	}
	
	public RisultatoAnalisi getRisultato() {
		return risultato;
	}
	
	public void avvia() {
		if (statoAnalisi != StatoAnalisi.CREATA) throw new IllegalStateException("Stato non valido per avviare l'analisi");
		
		this.statoAnalisi = StatoAnalisi.IN_ESECUZIONE; 
	}
	
	// Avvia l'analisi portandola dallo stato CREATA a IN_ESECUZIONE.
    public void aggiungiFileAnalizzato(FileAnalizzato file) {
    	if (file == null) throw new IllegalArgumentException("FileAnalizzato non può essere null");
        
    	fileAnalizzati.add(file);
    }

    public void registraIssue(Issue issue) {
    	if (issue == null) throw new IllegalArgumentException("Issue non può essere null");
        
    	issues.add(issue);
    }
    
    // Conclude l'analisi impostando il risultato finale e lo stato COMPLETATA.
    public void concludi(RisultatoAnalisi risultato) {
    	if (statoAnalisi != StatoAnalisi.IN_ESECUZIONE) throw new IllegalStateException("Stato non valido per avviare l'analisi");
    	if (risultato == null) throw new IllegalArgumentException("RisultatoAnalisi non può essere null");
    	
        this.risultato = risultato;
        this.statoAnalisi = StatoAnalisi.COMPLETATA;
    }
    
    // Porta l'analisi nello stato FALLITA in caso di errore durante l'esecuzione.
    public void fallisci() {
        if (statoAnalisi != StatoAnalisi.IN_ESECUZIONE) throw new IllegalStateException("Stato non valido per avviare l'analisi");
        
        this.statoAnalisi = StatoAnalisi.FALLITA;
    }
}
