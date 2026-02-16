package it.polastri.codereviewbot.domain;

public class Issue {
	private final FileAnalizzato fileAnalizzato; 
	private final int riga; 
	private final RegolaAnalisi regola; 
	private final String messaggio; 
	
	public Issue(FileAnalizzato fileAnalizzato,int riga, RegolaAnalisi regola, String messaggio) {
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
	
	// sostituisce descrizioneCompleta() del modello di dominio
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
