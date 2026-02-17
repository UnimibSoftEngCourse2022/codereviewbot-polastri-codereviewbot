package it.polastri.codereviewbot.domain;

/**
 * Rappresenta l'Abstract Syntax Tree (AST) di un file sorgente.
 * Contiene i nodi dell'albero rilevanti per l'analisi delle regole.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AST {
	
	private final List<NodoAST> nodi = new ArrayList<>();
	
	public List<NodoAST> getNodiRilevanti() {
		return Collections.unmodifiableList(nodi);
	}
	
    public void aggiungiNodo(NodoAST nodo) {
        if (nodo == null) {
            throw new IllegalArgumentException("Il nodo non può essere null");
        }
        nodi.add(nodo);
    }
    
    public boolean isEmpty() {
        return nodi.isEmpty();
    }
}