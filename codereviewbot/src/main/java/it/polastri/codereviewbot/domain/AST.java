package it.polastri.codereviewbot.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Rappresenta l'Abstract Syntax Tree (AST) di un file sorgente.
 * Contiene i nodi dell'albero rilevanti per l'analisi delle regole.
 */

public class AST {
	
	private final List<NodoAST> nodi = new ArrayList<>();
	
	public List<NodoAST> getNodiRilevanti() {
		return Collections.unmodifiableList(nodi);
	}
	
    public void aggiungiNodo(NodoAST nodo) {
        nodi.add(Objects.requireNonNull(nodo, "Il nodo non può essere null"));
    }
    
    public boolean isEmpty() {
        return nodi.isEmpty();
    }
}