package it.polastri.codereviewbot.domain;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.EnumMap; 
import java.util.List; 

/**
 * Rappresenta un report generato a partire dai risultati di un'analisi.
 * Contiene una classificazione delle issue per categoria e severità
 * e un punteggio complessivo di qualità.
 */

public class Report {
	
	private final String id; 
	private final Analisi analisi; 
	private final LocalDateTime generatoIl; 
	private final ReportFormat formato;
	private final int scoreQualita; 
	private final Map<Categoria, Map<Severita, Integer>> classificazione;
	
	public Report(String id, Analisi analisi, ReportFormat formato, int scoreQualita,
			Map<Categoria, Map<Severita, Integer>> classificazione) {
	   
        this.id = Objects.requireNonNull(id, "Id report non può essere null");
        this.analisi = Objects.requireNonNull(analisi, "Analisi non può essere null");
        this.formato = Objects.requireNonNull(formato, "Formato report non può essere null");
        Objects.requireNonNull(classificazione, "Classificazione non può essere null");
        if (id.isBlank()) throw new IllegalArgumentException("Id report non può essere vuoto");
        
		this.generatoIl = LocalDateTime.now();
		this.scoreQualita = scoreQualita;
		this.classificazione = classificazione.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, 
				e -> Map.copyOf(e.getValue())));
	}
	
	public String getId() {
		return id;
	}
	
	public Analisi getAnalisi() {
		return analisi;
	}
	
	public LocalDateTime getGeneratoIl() {
		return generatoIl;
	}
	
	public ReportFormat getFormato() {
		return formato;
	}
	
	public int getScoreQualita() {
		return scoreQualita;
	} 
	
	// Restituisce la classificazione delle issue per categoria e severità. La mappa restituita è non modificabile.
	public Map<Categoria, Map<Severita, Integer>> getClassificazione() {
		return classificazione;
	}
	
	// Crea un report a partire da un'analisi completata. Aggrega le issue per categoria e severità.
	public static Report creaDa(Analisi analisi, ReportFormat formato, int scoreQualita) {
        Objects.requireNonNull(analisi, "Analisi non può essere null");
        Objects.requireNonNull(formato, "Formato non può essere null");
	    if (analisi.getStatoAnalisi() != StatoAnalisi.COMPLETATA) throw new IllegalStateException("Il report può essere creato solo da un'analisi completata");
	    
	    // assumo che Analisi esponga le issue
	    List<Issue> issues = analisi.getIssues();

	    // categoria -> (severita -> count)
	    Map<Categoria, Map<Severita, Integer>> classificazione = new EnumMap<>(Categoria.class);

	    for (Issue issue : issues) {
	        Categoria cat = issue.getRegola().getCategoria();
	        Severita sev = issue.getRegola().getSeverita();

	        classificazione.computeIfAbsent(cat, c -> new EnumMap<>(Severita.class))
	            .merge(sev, 1, Integer::sum);
	    }

	    String id = "REP-" + analisi.getId();
	    return new Report(id, analisi, formato, scoreQualita, classificazione);
	}
}