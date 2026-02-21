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
 * Regola di analisi che segnala l'uso di System.out.println nel codice.
 * Scopo: dimostrare una best practice (evitare output diretto in produzione).
 */

public class RegolaNoSystemOutPrintln extends RegolaAnalisi {

    public RegolaNoSystemOutPrintln() {
        super(
        	"R_NO_SYSOUT", 
        	"Evita l'uso di System.out.println: preferire un logger.",
            Severita.WARNING, 
            Categoria.BEST_PRACTICE);
    }

    @Override
    public List<Issue> applica(NodoAST nodo, FileAnalizzato fileAnalizzato) {
        Objects.requireNonNull(nodo, "NodoAST non può essere null");
        Objects.requireNonNull(fileAnalizzato, "FileAnalizzato non può essere null");

        String valore = nodo.getValore();
        if (valore == null || valore.isBlank()) {
            return Collections.emptyList();
        }

        if (valore.contains("System.out.println")) {
            Issue issue = new Issue(fileAnalizzato, nodo.getLinea(), this,
                "Uso di System.out.println rilevato: usare un logger.");
            return List.of(issue);
        }

        return Collections.emptyList();
    }
}