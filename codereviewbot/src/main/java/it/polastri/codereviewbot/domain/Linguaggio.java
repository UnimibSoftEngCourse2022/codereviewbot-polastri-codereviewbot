package it.polastri.codereviewbot.domain;

/**
 * Rappresenta un linguaggio di programmazione supportato dal sistema,
 * definito dal nome e dalle estensioni dei file associate.
 */

import java.util.List; 

public class Linguaggio {
	
	private final String nome; 
	private final List<String> estensioni; 
	
    public Linguaggio(String nome, List<String> estensioni) {
        if (nome == null) throw new IllegalArgumentException("Nome linguaggio non può essere null");
        if (estensioni == null) throw new IllegalArgumentException("Lista estensioni non può essere null");
    	
        this.nome = nome;
        this.estensioni = List.copyOf(estensioni);
    }
	
	public String getNome() {
		return nome;
	}
	
	public List<String> getEstensioni() {
		return estensioni;
	} 
	
	// Verifica se l'estensione indicata è supportata dal linguaggio.
    public boolean supportaEstensione(String estensione) {
    	if (estensione == null) return false;
    	
    	return estensioni.contains(estensione.toLowerCase());
    }
    
    @Override
    public String toString() {
        return nome;
    }
}