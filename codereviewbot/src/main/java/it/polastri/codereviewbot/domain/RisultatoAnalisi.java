package it.polastri.codereviewbot.domain;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.HashMap; 
import java.time.LocalDateTime;
import java.util.List; 

/**
 * Rappresenta il risultato generato dopo l'avvio, esecuzione e completamento
 * di un'Analisi. 
 */

public class RisultatoAnalisi {
	
	private final int numeroErrori;
	private final int numeroWarning; 
	private final Map<String, Integer> metrichePreliminari; 
	private final LocalDateTime generatoIl; 
	private final int numeroIssueTotali;
	
	public RisultatoAnalisi(int numeroErrori, int numeroWarning, Map<String, Integer> metrichePreliminari, int numeroIssueTotali) {
        Objects.requireNonNull(metrichePreliminari, "Metriche non possono essere null");
        if (numeroErrori < 0 || numeroWarning < 0 || numeroIssueTotali < 0) throw new IllegalArgumentException("Conteggi non validi");
		
		this.numeroErrori = numeroErrori; 
		this.numeroWarning = numeroWarning; 
		this.numeroIssueTotali = numeroIssueTotali; 
		this.metrichePreliminari = Map.copyOf(metrichePreliminari);
		this.generatoIl = LocalDateTime.now();
	}
	
	public int getNumeroErrori() {
		return numeroErrori;
	}
	
	public int getNumeroWarning() {
		return numeroWarning;
	}
	
	public Map<String, Integer> getMetrichePreliminari() {
		return metrichePreliminari;
	}
	
	public LocalDateTime getGeneratoIl() {
		return generatoIl;
	}
	
	public int getNumeroIssueTotali() {
		return numeroIssueTotali;
	} 
	
	// Factory method: crea il risultato a partire da issue e file analizzati. Calcola conteggi e metriche preliminari.
    public static RisultatoAnalisi creaDa(List<Issue> issues, List<FileAnalizzato> fileAnalizzati) {
        Objects.requireNonNull(issues, "Issues non possono essere null");
        Objects.requireNonNull(fileAnalizzati, "FileAnalizzati non possono essere null");

        // Numero di errori, warning e issues totali (errori + warning) 
        int errori = (int) issues.stream().filter(i -> i.getRegola().getSeverita() == Severita.ERROR).count();
        int warning = (int) issues.stream().filter(i -> i.getRegola().getSeverita() == Severita.WARNING).count();
        int totIssues = issues.size();

        // Metriche preliminari semplici
        Map<String, Integer> metriche = new HashMap<>();
        metriche.put("files_analizzati", fileAnalizzati.size());

        // Numero di file in cui il parsing è andato a buon fine
        int parsingOk = (int) fileAnalizzati.stream().filter(f -> f.getEsitoParsing() == EsitoParsing.OK).count();

        // Numero di file in cui il parsing è fallito
        int parsingError = (int) fileAnalizzati.stream().filter(f -> f.getEsitoParsing() == EsitoParsing.ERROR).count();

        metriche.put("files_parsing_ok", parsingOk);
        metriche.put("files_parsing_error", parsingError);

        metriche.put("issues_totali", totIssues);
        metriche.put("issues_error", errori);
        metriche.put("issues_warning", warning);

        // Numero massimo di issue su un singolo file
        Map<FileAnalizzato, Long> countPerFile = issues.stream()
        		.collect(Collectors.groupingBy(Issue::getFileAnalizzato, Collectors.counting()));

        int maxPerFile = countPerFile.values().stream().mapToInt(Long::intValue).max().orElse(0);
        metriche.put("issues_per_file_max", maxPerFile);

        return new RisultatoAnalisi(errori, warning, metriche, totIssues);
    }
}