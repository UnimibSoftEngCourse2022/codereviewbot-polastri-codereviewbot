package it.polastri.codereviewbot.domain;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors; 

/**
 * Rappresenta un linguaggio di programmazione supportato dal sistema,
 * definito dal nome e dalle estensioni dei file associate.
 */

public class Linguaggio {
	
	private final String nome; 
	private final List<String> estensioni; 
	
    public Linguaggio(String nome, List<String> estensioni) {
        this.nome = Objects.requireNonNull(nome, "Nome linguaggio non può essere null");
        Objects.requireNonNull(estensioni, "Lista estensioni non può essere null");
    	
        // Normalizzazione: lower-case e con '.'
        this.estensioni = estensioni.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.startsWith(".") ? s.toLowerCase() : ("." + s.toLowerCase()))
                .distinct()
                .collect(Collectors.toUnmodifiableList());
    }
	
	public String getNome() {
		return nome;
	}
	
	public List<String> getEstensioni() {
		return estensioni;
	} 
	
	// Verifica se l'estensione indicata è supportata dal linguaggio.
    public boolean supportaEstensione(String estensione) {
        if (estensione == null || estensione.isBlank()) return false;
        String norm = estensione.trim().toLowerCase();
        
        if (!norm.startsWith(".")) norm = "." + norm;
        return estensioni.contains(norm);
    }
    
    @Override
    public String toString() {
        return nome;
    }
}