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

public class Main {

    public static void main(String[] args) {
        int exitCode = run(args);
        System.exit(exitCode);
    }

    static int run(String[] args) {
        Logger logger = new ConsoleLogger();

        try {
            CliOptions opt = CliOptions.parse(args);

            if (opt.help) {
                printHelp();
                return 0;
            }

            // Controlli input 
            if (opt.projectPath == null || opt.projectPath.isBlank()) {
                System.err.println("Errore: --project è obbligatorio.");
                printHelp();
                return 2;
            }

            ReportFormat format = parseFormat(opt.format);

            // Dipendenze 
            ProjectLoader loader = new FileSystemProjectLoader();
            Parser parser = new StubParser();

            List<RegolaAnalisi> regole = List.of(
                    new RegolaNoTODO(),
                    new RegolaNoSystemOutPrintln(),
                    new RegolaMetodoTroppoLungo(30)
            );

            AnalisiService analisiService =
                    new AnalisiService(loader, parser, regole, logger);

            List<ReportService.ReportExporterBinding> bindings = List.of(
                    new ReportService.ReportExporterBinding(ReportFormat.HTML, new HtmlReportExporter()),
                    new ReportService.ReportExporterBinding(ReportFormat.JSON, new JsonReportExporter()),
                    new ReportService.ReportExporterBinding(ReportFormat.PDF, new PdfReportExporter())
            );

            ReportService reportService =
                    new ReportService(new QualityScoreService(), bindings, logger);

            // CU1: Analisi
            Analisi analisi = analisiService.eseguiAnalisi(opt.projectPath);

            if (analisi.getStatoAnalisi() != StatoAnalisi.COMPLETATA) {
                System.err.println("Analisi fallita. Controllare i log.");
                return 1;
            }

            // CU2: Report 
            Report report =
                    reportService.generaReportQualita(analisi, format, opt.outputPath);

            // Output per utente tipo
            System.out.println("Analisi completata con successo.");
            System.out.println("Issue rilevate: " + analisi.getIssues().size());
            System.out.println("Quality score: " + report.getScoreQualita() + "/100");

            if (opt.outputPath != null && !opt.outputPath.isBlank()) {
                System.out.println("Report esportato in: " + opt.outputPath);
            } else {
                System.out.println("Report generato in memoria (nessun file di output).");
            }

            return 0;

        } catch (IllegalArgumentException e) {
            System.err.println("Errore argomenti: " + e.getMessage());
            printHelp();
            return 2;
        } catch (Exception e) {
            System.err.println("Errore inatteso: " + e.getMessage());
            return 1;
        }
    }

    private static ReportFormat parseFormat(String value) {
        if (value == null || value.isBlank()) {
        	// default
            return ReportFormat.HTML;
        }
        try {
            return ReportFormat.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Formato non valido: " + value + " (usa HTML, JSON o PDF)");
        }
    }

    private static void printHelp() {
        System.out.println("""
                CodeReviewBot - CLI

                Uso:
                  java -jar codereviewbot.jar --project <path>
                                          [--format HTML|JSON|PDF]
                                          [--out <file>]
                                          [--help]

                Esempi:
                  java -jar codereviewbot.jar --project ./repo --format HTML --out report.html
                  java -jar codereviewbot.jar --project ./repo --format JSON --out report.json

                Note:
                  - Il formato di default è HTML.
                  - Se --out non è specificato, il report non viene scritto su file.
                """);
    }

    // Parser minimale degli argomenti CLI
    static class CliOptions {
        boolean help;
        String projectPath;
        String format;
        String outputPath;

        static CliOptions parse(String[] args) {
            CliOptions o = new CliOptions();

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--help", "-h" -> o.help = true;
                    case "--project", "-p" -> o.projectPath = next(args, ++i, "--project richiede un valore");
                    case "--format", "-f" -> o.format = next(args, ++i, "--format richiede un valore");
                    case "--out", "-o" -> o.outputPath = next(args, ++i, "--out richiede un valore");
                    default -> throw new IllegalArgumentException("Argomento sconosciuto: " + args[i]);
                }
            }
            return o;
        }

        private static String next(String[] args, int i, String err) {
            if (i >= args.length) throw new IllegalArgumentException(err);
            return args[i];
        }
    }
}