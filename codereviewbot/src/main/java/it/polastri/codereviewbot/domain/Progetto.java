package it.polastri.codereviewbot.domain;

/**
 * Rappresenta un progetto software composto da uno o più file sorgente.
 */

import java.util.List; 
import java.util.ArrayList; 
import java.util.Collections;

public class Progetto {
	
	private final String projectPath; 
	private final List<FileSorgente> fileSorgenti = new ArrayList<>();
	
	public Progetto(String projectPath) {
	    if (projectPath == null) throw new IllegalArgumentException("Project path non può essere null");
	  
		this.projectPath = projectPath;
	}
	
	public String getProjectPath() {
		return projectPath;
	}
	
	public List<FileSorgente> getFileSorgenti() {
		return Collections.unmodifiableList(fileSorgenti);
	} 
	
	// Aggiunge un file sorgente al progetto.
	public void aggiungiFileSorgente(FileSorgente file) {
		// Evita la presenza di file duplicati.
		if (fileSorgenti.contains(file)) throw new IllegalArgumentException("File già presente nel progetto");
        if (file == null) throw new IllegalArgumentException("FileSorgente non può essere null");
		
        fileSorgenti.add(file); 
	}
}