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
 * Regola di analisi che segnala la presenza di "TODO" nel codice/commenti.
 * Scopo: dimostrare una regola semplice che produce issue (WARNING).
 */

public class RegolaNoTODO extends RegolaAnalisi {

    public RegolaNoTODO() {
        super(
            "R_NO_TODO", 
            "Evita di lasciare TODO nel codice: devono essere risolti o tracciati altrove.",
            Severita.WARNING, 
            Categoria.STILE);
    }

    @Override
    public List<Issue> applica(NodoAST nodo, FileAnalizzato fileAnalizzato) {
        Objects.requireNonNull(nodo, "NodoAST non può essere null");
        Objects.requireNonNull(fileAnalizzato, "FileAnalizzato non può essere null");

        String valore = nodo.getValore();
        if (valore == null || valore.isBlank()) {
            return Collections.emptyList();
        }

        // Controllo semplice: se nel testo del nodo compare "TODO" (case-insensitive) segnala una issue
        if (valore.toUpperCase().contains("TODO")) {
            Issue issue = new Issue(fileAnalizzato, nodo.getLinea(), this, 
            		"Presente TODO nel codice: rimuovere o risolvere il TODO.");
            return List.of(issue);
        }

        return Collections.emptyList();
    }
}