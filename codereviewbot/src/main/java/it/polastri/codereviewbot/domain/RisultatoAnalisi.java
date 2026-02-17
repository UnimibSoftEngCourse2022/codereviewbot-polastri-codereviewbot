package it.polastri.codereviewbot.domain;

import java.util.Map; 
import java.util.HashMap; 
import java.time.LocalDateTime;
import java.util.List; 

public class RisultatoAnalisi {
	
	private final int numeroErrori;
	private final int numeroWarning; 
	private final Map<String, Integer> metrichePreliminari; 
	private final LocalDateTime generatoIl; 
	private final int numeroIssueTotali;
	
	public RisultatoAnalisi(int numeroErrori, int numeroWarning, Map<String, Integer> metrichePreliminari, List<Issue> issues) {
		this.numeroErrori = (int) issues.stream().filter(i -> i.getRegola().getSeverita() == Severita.ERROR).count();
		this.numeroWarning = (int) issues.stream().filter(i -> i.getRegola().getSeverita() == Severita.WARNING).count();
		this.numeroIssueTotali = issues.size();
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
	
	public static RisultatoAnalisi creaDa(List<Issue> issues, List<FileAnalizzato> fileAnalizzati) {
		Map<String, Integer> metriche = new HashMap<>();

	    // Metriche preliminari sensate
	    metriche.put("fileAnalizzati: ", fileAnalizzati.size());

	    int fileConParsingFallito = (int) fileAnalizzati.stream().filter(f -> !f.parsingRiuscito()).count();

	    metriche.put("fileConParsingFallito", fileConParsingFallito);

	    // numeroErrori e numeroWarning non servono: li ricalcola il costruttore
	    return new RisultatoAnalisi(0,0, metriche, issues);
	}
}
