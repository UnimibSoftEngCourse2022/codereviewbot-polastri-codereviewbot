package it.polastri.codereviewbot.infrastructure.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import it.polastri.codereviewbot.application.exception.ReportExportException;
import it.polastri.codereviewbot.domain.Issue;
import it.polastri.codereviewbot.domain.Report;

/**
 * Implementazione concreta di ReportExporter che
 * esporta il report in formato testuale leggibile.
 */

public class TextReportExporter implements ReportExporter {

    @Override
    public void esporta(Report report, String outputPath) {
        if (report == null)
            throw new IllegalArgumentException("Report non può essere null");
        if (outputPath == null || outputPath.isBlank())
            throw new IllegalArgumentException("OutputPath non valido");

        // costruzione del contenuto testuale (aggregazione di tutti i dati)
        StringBuilder sb = new StringBuilder();

        sb.append("=== CodeReviewBot - Report Qualità ===\n");
        sb.append("Report ID: ").append(report.getId()).append("\n");
        sb.append("Generato il: ").append(report.getGeneratoIl()).append("\n");
        sb.append("Formato richiesto: ").append(report.getFormato()).append("\n");
        sb.append("Quality Score: ").append(report.getScoreQualita()).append("/100\n");
        sb.append("\n");

        sb.append("Analisi ID: ").append(report.getAnalisi().getId()).append("\n");
        sb.append("Progetto: ").append(report.getAnalisi().getProgetto().getProjectPath()).append("\n");
        sb.append("\n");

        sb.append("--- Classificazione Issue ---\n");
        report.getClassificazione().forEach((categoria, bySeverita) -> {
            sb.append(categoria).append(":\n");
            bySeverita.forEach((sev, count) ->
                sb.append("  ").append(sev).append(" = ").append(count).append("\n")
            );
        });

        sb.append("\n--- Dettaglio Issue ---\n");
        for (Issue issue : report.getAnalisi().getIssues()) {
            sb.append("- ")
              .append(issue.getFileAnalizzato().getFileSorgente().getPath())
              .append(":")
              .append(issue.getRiga())
              .append(" [")
              .append(issue.getRegola().getId())
              .append(" | ")
              .append(issue.getRegola().getCategoria())
              .append(" | ")
              .append(issue.getRegola().getSeverita())
              .append("] ")
              .append(issue.getMessaggio())
              .append("\n");
        }

        try {
            Path out = Path.of(outputPath);
            if (out.getParent() != null) {
                Files.createDirectories(out.getParent());
            }
            Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ReportExportException(
                "Errore durante l'esportazione del report su: " + outputPath, e
            );
        }
    }
}