package it.polastri.codereviewbot.cli;

import java.util.List;
import java.util.Locale;

import it.polastri.codereviewbot.application.AnalisiService;
import it.polastri.codereviewbot.application.ReportService;
import it.polastri.codereviewbot.domain.*;
import it.polastri.codereviewbot.domain.rules.RegolaMetodoTroppoLungo;
import it.polastri.codereviewbot.domain.rules.RegolaNoSystemOutPrintln;
import it.polastri.codereviewbot.domain.rules.RegolaNoTODO;
import it.polastri.codereviewbot.infrastructure.loader.FileSystemProjectLoader;
import it.polastri.codereviewbot.infrastructure.loader.ProjectLoader;
import it.polastri.codereviewbot.infrastructure.logger.ConsoleLogger;
import it.polastri.codereviewbot.infrastructure.logger.Logger;
import it.polastri.codereviewbot.infrastructure.parser.Parser;
import it.polastri.codereviewbot.infrastructure.parser.StubParser;
import it.polastri.codereviewbot.infrastructure.report.*;

/**
 * Entry point dell'applicazione CodeReviewBot in modalità CLI.
 *
 * La classe si occupa di:
 * - parsare gli argomenti da linea di comando
 * - configurare le dipendenze applicative
 * - orchestrare i casi d'uso principali (CU1 e CU2)
 *
 * Restituisce un exit code coerente con l'esito dell'esecuzione.
 */
public class Main {

    // Delega l'esecuzione al metodo e termina il processo con l'exit code restituito.
    public static void main(String[] args) {
        int exitCode = run(args);
        System.exit(exitCode);
    }

    /**
     * Esegue l'applicazione CLI.
     * args: argomenti da linea di comando
     * exit codes: 0 = successo, 1 = errore generico, 2 = errore input
     */
    static int run(String[] args) {
        Logger logger = new ConsoleLogger();

        // Configurazione dipendenze infrastructure
        ProjectLoader loader = new FileSystemProjectLoader();
        Parser parser = new StubParser();

        return run(args, logger, loader, parser);
    }

    /**
     * Variante di run usabile dai test: consente di passare dipendenze fittizie (stub).
     */
    static int run(String[] args, Logger logger, ProjectLoader loader, Parser parser) {
        try {
            CliOptions opt = CliOptions.parse(args);

            if (opt.help) {
                printHelp(logger);
                return 0;
            }

            // Validazione input minimo
            if (opt.projectPath == null || opt.projectPath.isBlank()) {
                logger.error("Errore: --project è obbligatorio.");
                printHelp(logger);
                return 2;
            }

            ReportFormat format = parseFormat(opt.format);

            List<RegolaAnalisi> regole = List.of(
                    new RegolaNoTODO(),
                    new RegolaNoSystemOutPrintln(),
                    new RegolaMetodoTroppoLungo(30)
            );

            // CU1: Analisi del progetto
            AnalisiService analisiService =
                    new AnalisiService(loader, parser, regole, logger);

            // Configurazione export report
            List<ReportService.ReportExporterBinding> bindings = List.of(
                    new ReportService.ReportExporterBinding(ReportFormat.HTML, new HtmlReportExporter()),
                    new ReportService.ReportExporterBinding(ReportFormat.JSON, new JsonReportExporter()),
                    new ReportService.ReportExporterBinding(ReportFormat.PDF, new PdfReportExporter())
            );

            // CU2: Generazione report
            ReportService reportService =
                    new ReportService(new QualityScoreService(), bindings, logger);

            return eseguiCasiUso(opt, format, analisiService, reportService, logger);

        } catch (IllegalArgumentException e) {
            logger.error("Errore argomenti: " + e.getMessage());
            printHelp(logger);
            return 2;
        } catch (Exception e) {
            logger.error("Errore inatteso", e);
            return 1;
        }
    }

    // Esegue CU1 e CU2 una volta validati gli input e configurate le dipendenze.
    private static int eseguiCasiUso(
            CliOptions opt,
            ReportFormat format,
            AnalisiService analisiService,
            ReportService reportService,
            Logger logger) {

        Analisi analisi = analisiService.eseguiAnalisi(opt.projectPath);

        if (analisi.getStatoAnalisi() != StatoAnalisi.COMPLETATA) {
            logger.error("Analisi fallita. Controllare i log.");
            return 1;
        }

        Report report =
                reportService.generaReportQualita(analisi, format, opt.outputPath);

        // Output sintetico per l'utente
        logger.info("Analisi completata con successo.");
        logger.info("Issue rilevate: " + analisi.getIssues().size());
        logger.info("Quality score: " + report.getScoreQualita() + "/100");

        if (opt.outputPath != null && !opt.outputPath.isBlank()) {
            logger.info("Report esportato in: " + opt.outputPath);
        } else {
            logger.info("Nessun file di output specificato.");
        }

        return 0;
    }

    /**
     * Converte il formato passato da CLI nel corrispondente enum.
     * value: stringa formato (HTML, JSON, PDF)
     */
    private static ReportFormat parseFormat(String value) {
        if (value == null || value.isBlank()) {
            return ReportFormat.HTML; // default
        }
        try {
            return ReportFormat.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Formato non valido: " + value + " (usa HTML, JSON o PDF)");
        }
    }

    // Stampa il messaggio di help della CLI.
    private static void printHelp(Logger logger) {
        logger.info("""
                CodeReviewBot - CLI

                Uso:
                  java -jar <jar-file>.jar --project <path>
                                           [--format HTML|JSON|PDF]
                                           [--out <file>]
                                           [--help]

                Esempi:
                  java -jar <jar-file>.jar --project ./repo --format HTML --out report.html
                  java -jar <jar-file>.jar --project ./repo --format JSON --out report.json

                Note:
                  - Il formato di default è HTML.
                  - Se --out non è specificato, il report non viene scritto su file.
                """);
    }

    // Parser minimale degli argomenti da linea di comando.
    static class CliOptions {
        boolean help;
        String projectPath;
        String format;
        String outputPath;

        static CliOptions parse(String[] args) {
            CliOptions o = new CliOptions();

            int i = 0;
            while (i < args.length) {
                String a = args[i];

                switch (a) {
                    case "--help", "-h" -> {
                        o.help = true;
                        i++;
                    }
                    case "--project", "-p" -> {
                        o.projectPath = next(args, i + 1, "--project richiede un valore");
                        i += 2;
                    }
                    case "--format", "-f" -> {
                        o.format = next(args, i + 1, "--format richiede un valore");
                        i += 2;
                    }
                    case "--out", "-o" -> {
                        o.outputPath = next(args, i + 1, "--out richiede un valore");
                        i += 2;
                    }
                    default -> throw new IllegalArgumentException("Argomento sconosciuto: " + a);
                }
            }

            return o;
        }

        private static String next(String[] args, int index, String err) {
            if (index >= args.length) {
                throw new IllegalArgumentException(err);
            }
            return args[index];
        }
    }
}