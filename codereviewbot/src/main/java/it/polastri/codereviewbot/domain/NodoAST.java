package it.polastri.codereviewbot.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class NodoAST {
	
	private final String tipoNodo; 
	private final String valore; 
	private final int linea; 
	private final List<NodoAST> figli = new ArrayList<>(); 
	
	public NodoAST(String tipoNodo, String valore, int linea) {
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
	
    public void aggiungiFiglio(NodoAST figlio) {
        if (figlio == null) {
            throw new IllegalArgumentException("Il figlio non può essere null");
        }
        figli.add(figlio);
    }
    
    public void accettaRegola(RegolaAnalisi regola) {
        if (regola == null) {
            throw new IllegalArgumentException("La regola non può essere null");
        }
        regola.applica(this);
    }
}
