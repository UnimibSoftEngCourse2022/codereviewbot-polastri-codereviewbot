package it.polastri.codereviewbot.domain;

public class FileSorgente {
	
	private final String nome; 
	private final String path; 
	private final Linguaggio linguaggio; 
	private final String contenuto;
	
	public FileSorgente(String nome, String path, Linguaggio linguaggio, String contenuto) {
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
}
