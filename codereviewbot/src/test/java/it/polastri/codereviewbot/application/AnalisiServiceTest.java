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

class AnalisiServiceTest {

    // Esegue analisi, applica regole e conclude l'analisi con un risultato
    @Test
    void esegueAnalisiEClassificaIssue() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));

        FileSorgente a = new FileSorgente(
                "A.java",
                "/p/A.java",
                java,
                "System.out.println(\"x\");\n// TODO: fix\nfine"
        );

        Progetto progetto = new Progetto("/p");
        progetto.aggiungiFileSorgente(a);

        ProjectLoader loaderFinto = path -> progetto;
        Parser parser = new StubParser(); // non restituisce null

        AnalisiService service = new AnalisiService(
                loaderFinto,
                parser,
                List.of(new RegolaNoTODO(), new RegolaNoSystemOutPrintln())
        );

        Analisi analisi = service.eseguiAnalisi("/p");

        assertEquals(StatoAnalisi.COMPLETATA, analisi.getStatoAnalisi());
        assertNotNull(analisi.getRisultato());

        // 2 issue attese (println + TODO)
        assertEquals(2, analisi.getIssues().size());

        // 1 file analizzato
        assertEquals(1, analisi.getFileAnalizzati().size());
        assertEquals(EsitoParsing.OK, analisi.getFileAnalizzati().get(0).getEsitoParsing());
        assertTrue(analisi.getFileAnalizzati().get(0).parsingRiuscito());
    }

    // Se il parsing fallisce su un file, lo marca in ERROR e continua sugli altri
    @Test
    void continuaSeUnFileFallisceParsing() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));

        FileSorgente ok = new FileSorgente("OK.java", "/p/OK.java", java, "System.out.println(\"x\");");
        FileSorgente bad = new FileSorgente("BAD.java", "/p/BAD.java", java, "qualcosa");

        Progetto progetto = new Progetto("/p");
        progetto.aggiungiFileSorgente(ok);
        progetto.aggiungiFileSorgente(bad);

        ProjectLoader loaderFinto = path -> progetto;

        Parser parserCheFallisceSoloSuBad = new Parser() {
            private final Parser okParser = new StubParser();

            @Override
            public AST parse(FileSorgente file) {
                if ("BAD.java".equals(file.getNome())) {
                    throw new RuntimeException("boom");
                }
                return okParser.parse(file); // mai null
            }
        };

        AnalisiService service = new AnalisiService(
                loaderFinto,
                parserCheFallisceSoloSuBad,
                List.of(new RegolaNoSystemOutPrintln())
        );

        Analisi analisi = service.eseguiAnalisi("/p");

        assertEquals(StatoAnalisi.COMPLETATA, analisi.getStatoAnalisi());

        // Solo OK.java produce 1 issue (println)
        assertEquals(1, analisi.getIssues().size());

        FileAnalizzato faOk = analisi.getFileAnalizzati().stream()
                .filter(fa -> "OK.java".equals(fa.getFileSorgente().getNome()))
                .findFirst().orElseThrow();

        FileAnalizzato faBad = analisi.getFileAnalizzati().stream()
                .filter(fa -> "BAD.java".equals(fa.getFileSorgente().getNome()))
                .findFirst().orElseThrow();

        assertEquals(EsitoParsing.OK, faOk.getEsitoParsing());
        assertEquals(EsitoParsing.ERROR, faBad.getEsitoParsing());
        assertNotNull(faBad.getMessaggioErroreParsing());
    }

    // Il service rifiuta projectPath null
    @Test
    void lanciaEccezioneSeProjectPathNullo() {
        ProjectLoader loaderFinto = path -> new Progetto("/p");
        Parser parser = new StubParser();

        AnalisiService service = new AnalisiService(loaderFinto, parser, List.of());

        assertThrows(NullPointerException.class, () -> service.eseguiAnalisi(null));
    }
    
 // Ogni esecuzione genera una nuova istanza di Analisi, indipendentemente dalle precedenti
    @Test
    void generaUnaNuovaAnalisiOgniVolta() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));

        FileSorgente a = new FileSorgente("A.java", "/p/A.java", java, "class A {}");
        Progetto progetto = new Progetto("/p");
        progetto.aggiungiFileSorgente(a);

        ProjectLoader loaderFinto = path -> progetto;
        Parser parser = new StubParser();

        AnalisiService service = new AnalisiService(loaderFinto, parser, List.of());

        Analisi analisi1 = service.eseguiAnalisi("/p");
        Analisi analisi2 = service.eseguiAnalisi("/p");

        assertNotSame(analisi1, analisi2, "Ogni esecuzione deve restituire una nuova istanza di Analisi");
        assertNotEquals(analisi1.getId(), analisi2.getId(), "Ogni Analisi deve avere un id diverso (RD4)");
    }
}