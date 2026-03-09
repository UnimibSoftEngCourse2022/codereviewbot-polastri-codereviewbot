package it.polastri.codereviewbot.infrastructure.report;

import it.polastri.codereviewbot.domain.Issue;
import it.polastri.codereviewbot.domain.Report;
import it.polastri.codereviewbot.infrastructure.report.support.AbstractTextReportExporter;

/**
 * Exporter concreto che genera un report in formato JSON.
 * Non utilizza il rendering testuale neutro, ma costruisce
 * una struttura JSON esplicita e strutturata.
 */

public class JsonReportExporter extends AbstractTextReportExporter implements ReportExporter {

    // Costanti per evitare duplicazione di literals.
    private static final String NL = "\n";
    private static final String IND1 = "  ";
    private static final String IND2 = "    ";
    private static final String IND3 = "      ";
    private static final String QUOTE = "\"";
    private static final String COMMA_NL = "," + NL;

    // Esporta il report in formato Json.
    @Override
    public void esporta(Report report, String outputPath) {
        valida(report, outputPath);

        StringBuilder json = new StringBuilder(16_384);

        json.append("{").append(NL);

        // Campi base
        json.append(IND1).append(q("reportId")).append(": ").append(q(report.getId())).append(COMMA_NL);
        json.append(IND1).append(q("generatedAt")).append(": ").append(q(report.getGeneratoIl().toString())).append(COMMA_NL);
        json.append(IND1).append(q("format")).append(": ").append(q(report.getFormato().name())).append(COMMA_NL);
        json.append(IND1).append(q("qualityScore")).append(": ").append(report.getScoreQualita()).append(COMMA_NL);

        // Analisi
        json.append(IND1).append(q("analysis")).append(": {").append(NL);
        json.append(IND2).append(q("analysisId")).append(": ").append(q(report.getAnalisi().getId())).append(COMMA_NL);
        json.append(IND2).append(q("projectPath")).append(": ").append(q(report.getAnalisi().getProgetto().getProjectPath())).append(NL);
        json.append(IND1).append("}").append(COMMA_NL);

        // Classificazione
        json.append(IND1).append(q("classification")).append(": [").append(NL);
        boolean firstClass = true;
        for (var entryCat : report.getClassificazione().entrySet()) {
            var categoria = entryCat.getKey();
            var bySev = entryCat.getValue();
            for (var entrySev : bySev.entrySet()) {
                if (!firstClass) json.append(COMMA_NL);
                firstClass = false;

                json.append(IND2).append("{")
                    .append(q("category")).append(": ").append(q(categoria.name())).append(", ")
                    .append(q("severity")).append(": ").append(q(entrySev.getKey().name())).append(", ")
                    .append(q("count")).append(": ").append(entrySev.getValue())
                    .append("}");
            }
        }
        json.append(NL).append(IND1).append("]").append(COMMA_NL);

        // Issues
        json.append(IND1).append(q("issues")).append(": [").append(NL);
        boolean firstIssue = true;
        for (Issue issue : report.getAnalisi().getIssues()) {
            if (!firstIssue) json.append(COMMA_NL);
            firstIssue = false;

            json.append(IND2).append("{").append(NL);
            json.append(IND3).append(q("file")).append(": ").append(q(issue.getFileAnalizzato().getFileSorgente().getPath())).append(COMMA_NL);
            json.append(IND3).append(q("line")).append(": ").append(issue.getRiga()).append(COMMA_NL);
            json.append(IND3).append(q("ruleId")).append(": ").append(q(issue.getRegola().getId())).append(COMMA_NL);
            json.append(IND3).append(q("category")).append(": ").append(q(issue.getRegola().getCategoria().name())).append(COMMA_NL);
            json.append(IND3).append(q("severity")).append(": ").append(q(issue.getRegola().getSeverita().name())).append(COMMA_NL);
            json.append(IND3).append(q("message")).append(": ").append(q(issue.getMessaggio())).append(NL);
            json.append(IND2).append("}");
        }
        json.append(NL).append(IND1).append("]").append(NL);

        json.append("}").append(NL);

        scriviUtf8(outputPath, json.toString());
    }

    // Converte una stringa in JSON string, includendo virgolette e escaping.
    private String q(String value) {
        return QUOTE + escapeJson(value) + QUOTE;
    }
}