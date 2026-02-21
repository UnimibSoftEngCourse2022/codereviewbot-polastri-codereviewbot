package it.polastri.codereviewbot.domain;

import java.util.List; 
import java.util.ArrayList; 
import java.util.Collections;
import java.util.Objects; 

/**
 * Rappresenta un progetto software composto da uno o più file sorgente.
 */

public class Progetto {
	
	private final String projectPath; 
	private final List<FileSorgente> fileSorgenti = new ArrayList<>();
	
	public Progetto(String projectPath) {
        this.projectPath = Objects.requireNonNull(projectPath, "Project path non può essere null");
	}
	
	public String getProjectPath() {
		return projectPath;
	}
	
	public List<FileSorgente> getFileSorgenti() {
		return Collections.unmodifiableList(fileSorgenti);
	} 
	
	// Aggiunge un file sorgente al progetto.
    public void aggiungiFileSorgente(FileSorgente file) {
        Objects.requireNonNull(file, "FileSorgente non può essere null");
        
        boolean duplicato = fileSorgenti.stream().anyMatch(f -> f.getPath().equals(file.getPath()));
        if (duplicato) {
            throw new IllegalArgumentException("File già presente nel progetto: " + file.getPath());
        }
        fileSorgenti.add(file);
    }
}