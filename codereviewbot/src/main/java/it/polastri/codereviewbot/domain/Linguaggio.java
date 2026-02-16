package it.polastri.codereviewbot.domain;

import java.util.List; 

public class Linguaggio {
	private final String nome; 
	private final List<String> estensioni; 
	
    public Linguaggio(String nome, List<String> estensioni) {
        this.nome = nome;
        this.estensioni = List.copyOf(estensioni);
    }
	
	public String getNome() {
		return nome;
	}
	
	public List<String> getEstensioni() {
		return estensioni;
	} 
	
    public boolean supportaEstensione(String estensione) {
        return estensioni.contains(estensione);
    }
    
    @Override
    public String toString() {
        return nome;
    }
}
