package it.polastri.codereviewbot.domain;

/**
 * Rappresenta un file sorgente appartenente a un progetto,
 * con il relativo contenuto, percorso e linguaggio di programmazione.
 */

public class FileSorgente {
	
	private final String nome; 
	private final String path; 
	private final Linguaggio linguaggio; 
	private final String contenuto;
	
	public FileSorgente(String nome, String path, Linguaggio linguaggio, String contenuto) {
	    if (nome == null) throw new IllegalArgumentException("Nome file non può essere null");
	    if (path == null) throw new IllegalArgumentException("Path non può essere null");
	    if (linguaggio == null) throw new IllegalArgumentException("Linguaggio non può essere null");
	    if (contenuto == null) throw new IllegalArgumentException("Contenuto non può essere null");
		
		this.nome = nome;
		this.path = path;
		this.linguaggio = linguaggio;
		this.contenuto = contenuto;
	}
	
	public String getNome() {
		return nome;
	}
	
	public String getPath() {
		return path;
	}
	
	public Linguaggio getLinguaggio() {
		return linguaggio;
	}
	
	public String getContenuto() {
		return contenuto;
	} 
	
	@Override
	public String toString() {
	    return "FileSorgente{path='" + path + "'}";
	}
	
	// Restituisce l'estensione del file (incluso il punto, es. ".java").
	public String getEstensione() {
	    int dot = nome.lastIndexOf('.');
	    if (dot < 0 || dot == nome.length() - 1) return "";
	    return nome.substring(dot).toLowerCase(); 
	}
}
