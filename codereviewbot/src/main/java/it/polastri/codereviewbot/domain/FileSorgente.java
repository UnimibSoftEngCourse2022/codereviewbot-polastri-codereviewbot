package it.polastri.codereviewbot.domain;

import java.util.Objects;

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
        this.nome = Objects.requireNonNull(nome, "Nome file non può essere null");
        this.path = Objects.requireNonNull(path, "Path non può essere null");
        this.linguaggio = Objects.requireNonNull(linguaggio, "Linguaggio non può essere null");
        this.contenuto = Objects.requireNonNull(contenuto, "Contenuto non può essere null");
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
