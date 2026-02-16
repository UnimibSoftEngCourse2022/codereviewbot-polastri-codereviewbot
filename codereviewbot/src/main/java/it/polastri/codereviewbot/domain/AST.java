package it.polastri.codereviewbot.domain;

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
}
