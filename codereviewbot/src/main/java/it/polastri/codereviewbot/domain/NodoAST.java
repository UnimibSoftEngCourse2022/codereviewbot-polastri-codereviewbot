package it.polastri.codereviewbot.domain;

/**
 * Rappresenta un nodo dell'Abstract Syntax Tree (AST).
 * Ogni nodo può avere figli e può essere visitato da una regola di analisi.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NodoAST {
	
	private final String tipoNodo; 
	private final String valore; 
	private final int linea; 
	private final List<NodoAST> figli = new ArrayList<>(); 
	
	public NodoAST(String tipoNodo, String valore, int linea) {
	    if (tipoNodo == null) throw new IllegalArgumentException("Tipo nodo non può essere null");
	    if (valore == null) throw new IllegalArgumentException("Valore nodo non può essere null");
	    if (linea <= 0) throw new IllegalArgumentException("Numero di linea non valido");
	    
		this.tipoNodo = tipoNodo; 
		this.valore = valore;
		this.linea = linea; 
	}
	
    public String getTipoNodo() {
        return tipoNodo;
    }

    public String getValore() {
        return valore;
    }

    public int getLinea() {
        return linea;
    }
	
	public List<NodoAST> getFigli() {
		return Collections.unmodifiableList(figli);
	}
	
	// Aggiunge un nodo figlio a questo nodo dell'AST.
    public void aggiungiFiglio(NodoAST figlio) {
        if (figlio == null) throw new IllegalArgumentException("Il figlio non può essere null");

        figli.add(figlio);
    }
    
    // Accetta una regola di analisi applicandola a questo nodo. La raccolta delle issue prodotte è responsabilità dell'analisi.
    public List<Issue> accettaRegola(RegolaAnalisi regola, FileAnalizzato fileAnalizzato) {
        if (regola == null) throw new IllegalArgumentException("La regola non può essere null");
        if (fileAnalizzato == null) throw new IllegalArgumentException("Il file analizzato non può essere null");
 
        return regola.applica(this, fileAnalizzato);
    }
    
    public boolean haFigli() {
        return !figli.isEmpty();
    }
}