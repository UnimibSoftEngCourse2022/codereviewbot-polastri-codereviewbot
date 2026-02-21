package it.polastri.codereviewbot.domain.rules;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import it.polastri.codereviewbot.domain.Categoria;
import it.polastri.codereviewbot.domain.FileAnalizzato;
import it.polastri.codereviewbot.domain.Issue;
import it.polastri.codereviewbot.domain.NodoAST;
import it.polastri.codereviewbot.domain.RegolaAnalisi;
import it.polastri.codereviewbot.domain.Severita;

/**
 * Regola di analisi che segnala metodi troppo lunghi.
 * Applica solo a nodi di tipo "METHOD" e usa come metrica il numero di figli (statement).
 */

public class RegolaMetodoTroppoLungo extends RegolaAnalisi {

    private final int sogliaStatement;

    public RegolaMetodoTroppoLungo(int sogliaStatement) {
        super(
            "R_LONG_METHOD",
            "Metodo troppo lungo: ridurre complessità suddividendo in funzioni più piccole.",
            Severita.ERROR,
            Categoria.COMPLESSITA);
        
        if (sogliaStatement <= 0) throw new IllegalArgumentException("La soglia deve essere > 0");
        this.sogliaStatement = sogliaStatement;
    }

    @Override
    public List<Issue> applica(NodoAST nodo, FileAnalizzato fileAnalizzato) {
        Objects.requireNonNull(nodo, "NodoAST non può essere null");
        Objects.requireNonNull(fileAnalizzato, "FileAnalizzato non può essere null");

        // Applica solo ai nodi che rappresentano un metodo 
        if (!"METHOD".equalsIgnoreCase(nodo.getTipoNodo())) {
            return Collections.emptyList();
        }

        int numStatement = nodo.getFigli().size();
        if (numStatement > sogliaStatement) {
            Issue issue = new Issue(fileAnalizzato, nodo.getLinea(), this,
                "Metodo troppo lungo: " + numStatement + " statement (soglia " + sogliaStatement + ").");
            return List.of(issue);
        }

        return Collections.emptyList();
    }
}