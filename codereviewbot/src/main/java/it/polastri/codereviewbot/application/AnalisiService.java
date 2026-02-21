package it.polastri.codereviewbot.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import it.polastri.codereviewbot.domain.*;
import it.polastri.codereviewbot.infrastructure.loader.ProjectLoader;
import it.polastri.codereviewbot.infrastructure.parser.Parser;

/**
 * Application Service: coordina il caso d'uso "Esegui analisi".
 *
 * Responsabilità:
 * - caricare il progetto (tramite ProjectLoader)
 * - per ogni file: parsing (Parser) -> AST
 * - applicare le regole ai nodi dell'AST
 * - raccogliere le issue nell'oggetto Analisi
 * - produrre il RisultatoAnalisi finale e concludere l'analisi
 */

public class AnalisiService {

    private final ProjectLoader projectLoader;
    private final Parser parser;
    private final List<RegolaAnalisi> regole;

    public AnalisiService(ProjectLoader projectLoader, Parser parser, List<RegolaAnalisi> regole) {
        this.projectLoader = Objects.requireNonNull(projectLoader, "ProjectLoader non può essere null");
        this.parser = Objects.requireNonNull(parser, "Parser non può essere null");
        this.regole = List.copyOf(Objects.requireNonNull(regole, "Lista regole non può essere null"));
    }

    /**
     * Esegue l'analisi di un progetto identificato dal path.
     * In caso di errore "globale" l'analisi viene marcata FALLITA.
     */
    public Analisi eseguiAnalisi(String projectPath) {
        Objects.requireNonNull(projectPath, "Project path non può essere null");

        // 1) Carica progetto (I/O delegato a infrastructure)
        Progetto progetto = projectLoader.caricaProgetto(projectPath);

        // 2) Crea e avvia l'analisi
        Analisi analisi = new Analisi(generaIdAnalisi(), progetto);
        analisi.avvia();

        try {
            // 3) Analizza tutti i file del progetto
            for (FileSorgente file : progetto.getFileSorgenti()) {

                FileAnalizzato fileAnalizzato = new FileAnalizzato(generaIdFileAnalizzato(file), file);
                analisi.aggiungiFileAnalizzato(fileAnalizzato);

                // Se il linguaggio non è supportato, il file viene ignorato
                if (!fileAnalizzato.isAnalizzabile()) {
                    continue;
                }

                try {
                    // 3.1) Parsing -> AST
                    AST ast = parser.parse(file);
                    fileAnalizzato.impostaAST(ast);
                    
                    // 3.2) Se parsing OK -> applicazione regole
                    if (!fileAnalizzato.parsingRiuscito() || ast == null || ast.isEmpty()) {
                        continue;
                    }

                    // 3.3) Applicazione regole -> Issue
                    applicaRegole(analisi, fileAnalizzato, ast);

                } catch (Exception e) {
                    // Errore sul singolo file: segna parsing fallito e continua con i successivi
                    fileAnalizzato.marcaParsingFallito(e.getMessage());
                }
            }

            // 4) Produce il risultato e conclude l'analisi
            RisultatoAnalisi risultato = RisultatoAnalisi.creaDa(analisi.getIssues(), analisi.getFileAnalizzati());
            analisi.concludi(risultato);

            return analisi;

        } catch (Exception e) {
            // Errore "grave" nel processo: marca l'analisi come fallita
            analisi.fallisci();
            return analisi;
        }
    }

    // Applica tutte le regole a tutti i nodi rilevanti di un AST e registra nell'analisi le issue prodotte.
    private void applicaRegole(Analisi analisi, FileAnalizzato fileAnalizzato, AST ast) {
        for (NodoAST nodo : ast.getNodiRilevanti()) {
            for (RegolaAnalisi regola : regole) {

                // Il nodo delega alla regola la decisione di produrre eventuali issue
                List<Issue> issuesProdotte = nodo.accettaRegola(regola, fileAnalizzato);

                // L'analisi raccoglie tutte le issue prodotte
                for (Issue issue : issuesProdotte) {
                    analisi.registraIssue(issue);
                }
            }
        }
    }

    private String generaIdAnalisi() {
    	return "AN-" + UUID.randomUUID();
    }

    private String generaIdFileAnalizzato(FileSorgente file) {
    	return "FA-" + UUID.randomUUID();
    }
}