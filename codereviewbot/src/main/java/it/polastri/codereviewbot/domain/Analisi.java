package it.polastri.codereviewbot.domain;

import java.time.LocalDateTime; 
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.ArrayList; 
import java.util.Collections; 

/**
 * Rappresenta un'analisi di qualità del codice eseguita su un progetto.
 * Gestisce il ciclo di vita dell'analisi, i file analizzati, le issue rilevate
 * e il risultato finale dell'analisi.
 */

public class Analisi {
	
	private final String id; 
	private final LocalDateTime dataOra; 
	private final Progetto progetto; 
	private StatoAnalisi statoAnalisi; 
	private final List<FileAnalizzato> fileAnalizzati = new ArrayList<>(); 
	private final List<Issue> issues = new ArrayList<>(); 
	private RisultatoAnalisi risultato;
	
	public Analisi(String id, Progetto progetto) {
        this.id = Objects.requireNonNull(id, "Id analisi non può essere null");
        this.progetto = Objects.requireNonNull(progetto, "Progetto non può essere null");
        
		this.dataOra = LocalDateTime.now();
		this.statoAnalisi = StatoAnalisi.CREATA;
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
		requireState(StatoAnalisi.CREATA, "avviare");
		
		this.statoAnalisi = StatoAnalisi.IN_ESECUZIONE; 
	}
	
	// Avvia l'analisi portandola dallo stato CREATA a IN_ESECUZIONE.
    public void aggiungiFileAnalizzato(FileAnalizzato file) {
        Objects.requireNonNull(file, "FileAnalizzato non può essere null");
    	requireState(StatoAnalisi.IN_ESECUZIONE, "aggiungere file analizzati");
    	
    	fileAnalizzati.add(file);
    }

    public void registraIssue(Issue issue) {
        Objects.requireNonNull(issue, "Issue non può essere null");
    	requireState(StatoAnalisi.IN_ESECUZIONE, "registrare issue");
    	
    	issues.add(issue);
    }
    
    // Conclude l'analisi impostando il risultato finale e lo stato COMPLETATA.
    public void concludi(RisultatoAnalisi risultato) {
        Objects.requireNonNull(risultato, "RisultatoAnalisi non può essere null");
    	requireState(StatoAnalisi.IN_ESECUZIONE, "concludere l'analisi");
    	
        this.risultato = risultato;
        this.statoAnalisi = StatoAnalisi.COMPLETATA;
    }
    
    // Porta l'analisi nello stato FALLITA in caso di errore durante l'esecuzione.
    public void fallisci() {
    	requireState(StatoAnalisi.IN_ESECUZIONE, "marcare l'analisi come fallita");
        
        this.statoAnalisi = StatoAnalisi.FALLITA;
    }
    
    private void requireState(StatoAnalisi expected, String azione) {
        if (statoAnalisi != expected) {
            throw new IllegalStateException("Stato non valido per " + azione + ": atteso " + expected + " ma era " + statoAnalisi);
        }
    }
    
    // Restituisce l’elenco delle issue associate a un file sorgente specifico, identificato dal suo percorso all’interno del progetto.
    public List<Issue> getIssuesPerFilePath(String filePath) {
        Objects.requireNonNull(filePath, "filePath non può essere null");

        return issues.stream().filter(i -> filePath.equals(i.getFileAnalizzato().getFileSorgente().getPath())).collect(Collectors.toList());
    }
}