package it.polastri.codereviewbot.domain.rules;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import it.polastri.codereviewbot.domain.Categoria;
import it.polastri.codereviewbot.domain.FileAnalizzato;
import it.polastri.codereviewbot.domain.Issue;
import it.polastri.codereviewbot.domain.NodoAST;
import it.polastri.codereviewbot.domain.RegolaAnalisi;
import it.polastri.codereviewbot.domain.Severita;

/**
 * Regola di analisi che segnala la presenza di TO_DO nei commenti.
 *
 * Versione raffinata:
 * - segnala TO_DO solo come parola intera (evita falsi positivi tipo RegolaNoTODO o R_NO_TODO)
 * - considera TO_DO solo se presente in un commento 
 */
 
public class RegolaNoTODO extends RegolaAnalisi {

    private static final Pattern TODO_WORD = Pattern.compile("\\bTODO\\b", Pattern.CASE_INSENSITIVE);

    public RegolaNoTODO() {
        super(
            "R_NO_TODO",
            "Evita di lasciare TODO nel codice: devono essere risolti o tracciati altrove.",
            Severita.WARNING,
            Categoria.STILE
        );
    }

    @Override
    public List<Issue> applica(NodoAST nodo, FileAnalizzato fileAnalizzato) {
        Objects.requireNonNull(nodo, "NodoAST non può essere null");
        Objects.requireNonNull(fileAnalizzato, "FileAnalizzato non può essere null");

        String valore = nodo.getValore();
        if (valore == null || valore.isBlank()) {
            return Collections.emptyList();
        }

        if (containsTodoInComment(valore)) {
            Issue issue = new Issue(
                fileAnalizzato,
                nodo.getLinea(),
                this,
                "Presente TODO nel commento: rimuovere o risolvere il TODO."
            );
            return List.of(issue);
        }

        return Collections.emptyList();
    }

    private static boolean containsTodoInComment(String line) {
        // 1) Commento di riga: // ...
        int idxLineComment = line.indexOf("//");
        if (idxLineComment >= 0) {
            String comment = line.substring(idxLineComment + 2);
            return TODO_WORD.matcher(comment).find();
        }

        // 2) Inizio commento di blocco: /* ...
        int idxBlockComment = line.indexOf("/*");
        if (idxBlockComment >= 0) {
            String comment = line.substring(idxBlockComment + 2);
            return TODO_WORD.matcher(comment).find();
        }

        // 3) Righe tipiche dentro commento di blocco (Javadoc o multiline): " * ..."
        String trimmedLeft = line.stripLeading();
        if (trimmedLeft.startsWith("*")) {
            return TODO_WORD.matcher(trimmedLeft).find();
        }

        return false;
    }
}