package it.polastri.codereviewbot.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import it.polastri.codereviewbot.domain.*;
import it.polastri.codereviewbot.infrastructure.loader.ProjectLoader;
import it.polastri.codereviewbot.infrastructure.parser.Parser;
import it.polastri.codereviewbot.infrastructure.logger.ConsoleLogger;
import it.polastri.codereviewbot.infrastructure.logger.Logger;

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
    private final Logger logger;

    // Costruttore per compatibilità: usa un ConsoleLogger di default.
    public AnalisiService(ProjectLoader projectLoader, Parser parser, List<RegolaAnalisi> regole) {
        this(projectLoader, parser, regole, new ConsoleLogger());
    }

    // Costruttore che permette l’iniezione del logger.
    public AnalisiService(ProjectLoader projectLoader, Parser parser, List<RegolaAnalisi> regole, Logger logger) {
        this.projectLoader = Objects.requireNonNull(projectLoader, "ProjectLoader non può essere null");
        this.parser = Objects.requireNonNull(parser, "Parser non può essere null");
        this.regole = List.copyOf(Objects.requireNonNull(regole, "Lista regole non può essere null"));
        this.logger = Objects.requireNonNull(logger, "Logger non può essere null");
    }

    /**
     * Esegue l'analisi di un progetto identificato dal path.
     * In caso di errore "globale" l'analisi viene marcata FALLITA.
     */
    public Analisi eseguiAnalisi(String projectPath) {
        Objects.requireNonNull(projectPath, "Project path non può essere null");

        logger.info("Avvio analisi progetto: " + projectPath);

        // Carica progetto (può lanciare eccezioni di I/O)
        Progetto progetto = projectLoader.caricaProgetto(projectPath);

        // Crea e avvia l'analisi prima di entrare nel processo (così possiamo marcarla FALLITA in caso di errori)
        Analisi analisi = new Analisi(generaIdAnalisi(), progetto);
        analisi.avvia();

        try {
        	List<FileSorgente> files = progetto.getFileSorgenti();
        	logger.info("Progetto caricato. File sorgente trovati: " + files.size());

            // Analizza tutti i file del progetto
            for (FileSorgente file : progetto.getFileSorgenti()) {
                FileAnalizzato fileAnalizzato = new FileAnalizzato(generaIdFileAnalizzato(), file);
                analisi.aggiungiFileAnalizzato(fileAnalizzato);

                analizzaFile(analisi, fileAnalizzato);
            }

            // Produce il risultato e conclude l'analisi
            RisultatoAnalisi risultato = RisultatoAnalisi.creaDa(analisi.getIssues(), analisi.getFileAnalizzati());
            analisi.concludi(risultato);

            logger.info("Analisi completata. Issue totali: " + analisi.getIssues().size());
            return analisi;

        } catch (Exception e) {
            // Errore "grave" nel processo: marca l'analisi come fallita
            analisi.fallisci();
            logger.error("Errore grave durante l'analisi. Analisi marcata come FALLITA.", e);
            return analisi;
        }
    }

    /**
     * Analizza un singolo file: parsing, validazione AST e applicazione regole.
     * In caso di errore sul singolo file, marca il parsing come fallito e termina senza bloccare l'analisi globale.
     */
    private void analizzaFile(Analisi analisi, FileAnalizzato fileAnalizzato) {
        // Se il linguaggio non è supportato, il file viene ignorato
        if (!fileAnalizzato.isAnalizzabile()) {
            // Log “soft” (non è un errore: è un requisito RD1)
            logger.info("File ignorato (linguaggio non supportato): " + fileAnalizzato.getFileSorgente().getPath());
            return;
        }

        try {
            FileSorgente file = fileAnalizzato.getFileSorgente();

            // Parsing -> AST
            AST ast = parser.parse(file);
            fileAnalizzato.impostaAST(ast);

            // Se parsing non riuscito o AST vuoto -> niente regole
            if (!fileAnalizzato.parsingRiuscito() || ast == null || ast.isEmpty()) {
                logger.warning("Parsing non riuscito o AST vuoto per file: " + file.getPath());
                return;
            }

            // Applicazione regole -> Issue
            applicaRegole(analisi, fileAnalizzato, ast);

        } catch (Exception e) {
            // Errore sul singolo file: segna parsing fallito e continua con i successivi
            fileAnalizzato.marcaParsingFallito(e.getMessage());
            logger.warning("Errore durante analisi file (continuo con gli altri): "
                    + fileAnalizzato.getFileSorgente().getPath()
                    + " - " + e.getMessage());
        }
    }

    // Applica tutte le regole a tutti i nodi rilevanti di un AST e registra nell'analisi le issue prodotte.
    private void applicaRegole(Analisi analisi, FileAnalizzato fileAnalizzato, AST ast) {
        for (NodoAST nodo : ast.getNodiRilevanti()) {
            for (RegolaAnalisi regola : regole) {
                List<Issue> issuesProdotte = nodo.accettaRegola(regola, fileAnalizzato);

                for (Issue issue : issuesProdotte) {
                    analisi.registraIssue(issue);
                }
            }
        }
    }

    private String generaIdAnalisi() {
        return "AN-" + UUID.randomUUID();
    }

    private String generaIdFileAnalizzato() {
        return "FA-" + UUID.randomUUID();
    }
}