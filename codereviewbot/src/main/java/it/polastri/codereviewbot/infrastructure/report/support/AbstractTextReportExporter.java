package it.polastri.codereviewbot.infrastructure.report.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import it.polastri.codereviewbot.application.exception.ReportExportException;
import it.polastri.codereviewbot.domain.Issue;
import it.polastri.codereviewbot.domain.Report;

/**
 * Classe astratta di supporto per gli exporter di report basati su output testuale.
 * Non rappresenta un formato di output finale, ma una base riutilizzabile
 * per ridurre la duplicazione di codice negli exporter concreti.
 */

public abstract class AbstractTextReportExporter {

    // Valida i parametri comuni a tutti gli exporter.
    protected void valida(Report report, String outputPath) {
        if (report == null) throw new IllegalArgumentException("Report non può essere null");
        if (outputPath == null || outputPath.isBlank()) throw new IllegalArgumentException("OutputPath non valido");
    }

    // Crea le directory padre del file di output, se non esistono.
    protected void ensureParentDir(String outputPath) throws IOException {
        Path out = Path.of(outputPath);
        if (out.getParent() != null) Files.createDirectories(out.getParent());
    }

    /**
     * Azione di scrittura che può lanciare IOException.
     * Serve per evitare duplicazione del try/catch tra metodi di scrittura.
     */
    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }

    /**
     * Esegue un'azione I/O gestendo la creazione della directory padre e
     * il wrapping dell'IOException in ReportExportException.
     */
    private void scriviConGestioneErrori(String outputPath, IoAction action) {
        try {
            ensureParentDir(outputPath);
            action.run();
        } catch (IOException e) {
            throw new ReportExportException("Errore durante l'esportazione su: " + outputPath, e);
        }
    }

    // Scrive una stringa UTF-8 su file.
    protected void scriviUtf8(String outputPath, String content) {
        scriviConGestioneErrori(outputPath,
                () -> Files.writeString(Path.of(outputPath), content, StandardCharsets.UTF_8));
    }

    // Scrive un array di byte su file (usato per PDF).
    protected void scriviBytes(String outputPath, byte[] content) {
        scriviConGestioneErrori(outputPath,
                () -> Files.write(Path.of(outputPath), content));
    }

    /**
     * Genera una rappresentazione testuale leggibile del report,
     * che viene usata come base comune per diversi formati (HTML e PDF)
     * e contiene tutte le informazioni principali.
     */
    protected String renderPlainText(Report report) {
        StringBuilder sb = new StringBuilder(8_192);

        sb.append("CodeReviewBot - Report Qualità\n");
        sb.append("Report ID: ").append(report.getId()).append("\n");
        sb.append("Generato il: ").append(report.getGeneratoIl()).append("\n");
        sb.append("Formato: ").append(report.getFormato()).append("\n");
        sb.append("Quality Score: ").append(report.getScoreQualita()).append("/100\n");
        sb.append("\n");

        sb.append("Analisi ID: ").append(report.getAnalisi().getId()).append("\n");
        sb.append("Progetto: ").append(report.getAnalisi().getProgetto().getProjectPath()).append("\n");
        sb.append("\n");

        sb.append("Classificazione Issue:\n");
        report.getClassificazione().forEach((cat, bySev) ->
                bySev.forEach((sev, count) ->
                        sb.append(" - ")
                          .append(cat)
                          .append(" / ")
                          .append(sev)
                          .append(" = ")
                          .append(count)
                          .append("\n")
                )
        );

        sb.append("\n");
        sb.append("Dettaglio Issue:\n");
        for (Issue issue : report.getAnalisi().getIssues()) {
            sb.append(" - ")
              .append(issue.getFileAnalizzato().getFileSorgente().getPath())
              .append(":").append(issue.getRiga())
              .append(" [").append(issue.getRegola().getId())
              .append(" | ").append(issue.getRegola().getCategoria())
              .append(" | ").append(issue.getRegola().getSeverita())
              .append("] ")
              .append(issue.getMessaggio())
              .append("\n");
        }

        return sb.toString();
    }

    // Restituisce il report sotto forma di lista di righe.
    protected List<String> renderPlainTextLines(Report report) {
        return new ArrayList<>(List.of(renderPlainText(report).split("\\R")));
    }

    // Effettua l'escaping di caratteri speciali per HTML.
    protected String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // Effettua l'escaping di caratteri speciali per JSON.
    protected String escapeJson(String s) {
        return JsonEscaper.escape(s);
    }
}