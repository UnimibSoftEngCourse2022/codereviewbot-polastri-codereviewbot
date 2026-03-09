package it.polastri.codereviewbot.infrastructure.parser;

import it.polastri.codereviewbot.domain.AST;
import it.polastri.codereviewbot.domain.FileSorgente;

/**
 * Componente responsabile del parsing sintattico di un file sorgente.
 *
 * Il Parser analizza il contenuto di un FileAnalizzato e produce:
 * - un AST (Abstract Syntax Tree) in caso di successo
 * - un esito di parsing che indica eventuali errori
 */

public interface Parser {
    AST parse(FileSorgente file);
}