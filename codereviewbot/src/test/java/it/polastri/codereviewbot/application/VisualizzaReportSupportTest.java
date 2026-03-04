package it.polastri.codereviewbot.application;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

import it.polastri.codereviewbot.domain.*;
import it.polastri.codereviewbot.domain.rules.RegolaNoSystemOutPrintln;
import it.polastri.codereviewbot.domain.rules.RegolaNoTODO;
import it.polastri.codereviewbot.infrastructure.loader.ProjectLoader;
import it.polastri.codereviewbot.infrastructure.parser.Parser;
import it.polastri.codereviewbot.infrastructure.parser.StubParser;

/* Il test crea un progetto con due file A e B, esegue l'analisi, prende le issue prodotte dall'analisi e 
 * le filtra per filepath. Verifica poi che selezionando il filepath di A si ottengano solo 
 * le issue del file A e selezionando il filepath di B solo quelle del file B.
 */

class VisualizzaRevisioneSupportTest {

    // le issue devono essere filtrabili per file aperto (filePath)
    @Test
    void getIssuesPerFilePath_restituisceSoloLeIssueDelFileRichiesto() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));

        FileSorgente a = new FileSorgente("A.java", "/p/A.java", java, "System.out.println(\"x\");\n// TODO: fix\nclass A {}");

        FileSorgente b = new FileSorgente("B.java", "/p/B.java", java, "System.out.println(\"y\");\nclass B {}");

        Progetto progetto = new Progetto("/p");
        progetto.aggiungiFileSorgente(a);
        progetto.aggiungiFileSorgente(b);

        ProjectLoader loaderFinto = path -> progetto;
        Parser parser = new StubParser();

        AnalisiService service = new AnalisiService(loaderFinto, parser, List.of(new RegolaNoTODO(), new RegolaNoSystemOutPrintln()));

        Analisi analisi = service.eseguiAnalisi("/p");
        assertEquals(StatoAnalisi.COMPLETATA, analisi.getStatoAnalisi());

        List<Issue> issueFileA = analisi.getIssuesPerFilePath("/p/A.java");
        List<Issue> issueFileB = analisi.getIssuesPerFilePath("/p/B.java");

        // A: println + TO_DO = 2
        assertEquals(2, issueFileA.size(), "Sul file A.java mi aspetto 2 issue (println + TODO)");

        // B: solo println = 1
        assertEquals(1, issueFileB.size(), "Sul file B.java mi aspetto 1 issue (println)");

        // Verifica: nessuna issue del file A appartiene a B (e viceversa)
        assertTrue(issueFileA.stream()
                .allMatch(i -> "/p/A.java".equals(i.getFileAnalizzato().getFileSorgente().getPath())));
        assertTrue(issueFileB.stream()
                .allMatch(i -> "/p/B.java".equals(i.getFileAnalizzato().getFileSorgente().getPath())));
    }
}