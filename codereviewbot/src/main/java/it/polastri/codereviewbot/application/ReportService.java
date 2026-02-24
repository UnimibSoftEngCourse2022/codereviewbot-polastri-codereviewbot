package it.polastri.codereviewbot.application;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import it.polastri.codereviewbot.domain.Analisi;
import it.polastri.codereviewbot.domain.Report;
import it.polastri.codereviewbot.domain.ReportFormat;
import it.polastri.codereviewbot.domain.QualityScoreService;
import it.polastri.codereviewbot.domain.StatoAnalisi;
import it.polastri.codereviewbot.infrastructure.report.ReportExporter;
import it.polastri.codereviewbot.infrastructure.logger.ConsoleLogger;
import it.polastri.codereviewbot.infrastructure.logger.Logger;

/**
 * - Calcola score (QualityScoreService)
 * - Crea Report (Report.creaDa)
 * - Esporta il report (ReportExporter) nel formato richiesto
 */

public class ReportService {

    private final QualityScoreService qualityScoreService;
    // ad ogni formato, corrisponde un ReportExporter specifico
    private final Map<ReportFormat, ReportExporter> exporters;
    private final Logger logger;

    // Costruttore per compatibilità: usa un ConsoleLogger di default.
    public ReportService(QualityScoreService qualityScoreService, List<ReportExporterBinding> bindings) {
        this(qualityScoreService, bindings, new ConsoleLogger());
    }

    // Costruttore che permette l’iniezione del logger.
    public ReportService(QualityScoreService qualityScoreService, List<ReportExporterBinding> bindings, Logger logger) {
        this.qualityScoreService = Objects.requireNonNull(qualityScoreService, "QualityScoreService non può essere null");
        Objects.requireNonNull(bindings, "Bindings non può essere null");
        this.logger = Objects.requireNonNull(logger, "Logger non può essere null");

        Map<ReportFormat, ReportExporter> tmp = new EnumMap<>(ReportFormat.class);
        for (ReportExporterBinding b : bindings) {
            Objects.requireNonNull(b, "Binding non può essere null");
            if (tmp.containsKey(b.formato())) {
                throw new IllegalArgumentException("Exporter duplicato per formato: " + b.formato());
            }
            tmp.put(b.formato(), b.exporter());
        }
        this.exporters = Map.copyOf(tmp);
    }

    public Report generaReportQualita(Analisi analisi, ReportFormat formato) {
        return generaReportQualita(analisi, formato, null);
    }

    public Report generaReportQualita(Analisi analisi, ReportFormat formato, String outputPath) {
        Objects.requireNonNull(analisi, "Analisi non può essere null");
        Objects.requireNonNull(formato, "Formato non può essere null");

        logger.info("Generazione report richiesta. Formato: " + formato);

        // un report può essere generato solo da un’analisi completata
        if (analisi.getStatoAnalisi() != StatoAnalisi.COMPLETATA) {
            logger.error("Report non generabile: analisi non completata (stato=" + analisi.getStatoAnalisi() + ")");
            throw new IllegalStateException("Il report può essere generato solo da un'analisi completata");
        }

        int score = qualityScoreService.calcolaScore(analisi);
        logger.info("Quality score calcolato: " + score);

        Report report = Report.creaDa(analisi, formato, score);

        // export opzionale, esporta su file sse outputPath inserito
        if (outputPath != null && !outputPath.isBlank()) {
            ReportExporter exporter = exporters.get(formato);

            if (exporter == null) {
                logger.error("Nessun exporter registrato per formato: " + formato);
                throw new IllegalStateException("Nessun exporter registrato per formato: " + formato);
            }

            logger.info("Esportazione report su: " + outputPath);

            try {
                exporter.esporta(report, outputPath);
                logger.info("Esportazione completata.");
            } catch (RuntimeException e) {
                logger.error("Errore durante l'esportazione del report.", e);
                throw e;
            }
        }

        return report;
    }

    // Associazione formato -> exporters (esempio new ReportExporterBinding(ReportFormat.HTML, new TextReportExporter()))
    public static record ReportExporterBinding(ReportFormat formato, ReportExporter exporter) {
        public ReportExporterBinding {
            Objects.requireNonNull(formato, "Formato non può essere null");
            Objects.requireNonNull(exporter, "Exporter non può essere null");
        }
    }
}