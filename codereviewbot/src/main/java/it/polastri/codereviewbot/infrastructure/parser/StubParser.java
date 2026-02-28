package it.polastri.codereviewbot.infrastructure.parser;

import it.polastri.codereviewbot.domain.AST;
import it.polastri.codereviewbot.domain.FileSorgente;
import it.polastri.codereviewbot.domain.NodoAST;

/**
 * Parser stub: costruisce un AST minimale creando un nodo per ogni riga del contenuto.
 */

public class StubParser implements Parser {

    @Override
    public AST parse(FileSorgente file) {
        if (file == null) {
            throw new IllegalArgumentException("FileSorgente non può essere null");
        }

        String contenuto = file.getContenuto();
        if (contenuto == null) {
            throw new IllegalArgumentException("Contenuto del file non può essere null");
        }

        AST ast = new AST();

        // Split per righe (gestisce \n)
        String[] righe = contenuto.split("\\R", -1);

        for (int i = 0; i < righe.length; i++) {
            int numeroLinea = i + 1;
            String testoRiga = righe[i];

            // Nodo "LINE" sufficiente per regole basate su ricerca di stringhe
            NodoAST nodo = new NodoAST("LINE", testoRiga, numeroLinea);
            ast.aggiungiNodo(nodo);
        }

        return ast;
    }
}