package it.polastri.codereviewbot.application;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import it.polastri.codereviewbot.domain.*;
import it.polastri.codereviewbot.domain.rules.RegolaNoSystemOutPrintln;
import it.polastri.codereviewbot.domain.rules.RegolaNoTODO;
import it.polastri.codereviewbot.infrastructure.loader.ProjectLoader;
import it.polastri.codereviewbot.infrastructure.parser.Parser;
import it.polastri.codereviewbot.infrastructure.parser.StubParser;
import it.polastri.codereviewbot.infrastructure.logger.LogLevel;
import it.polastri.codereviewbot.infrastructure.logger.Logger;

class AnalisiServiceTest {

    /**
     * Logger spy per test: registra tutte le chiamate log
     * così da verificare che il servizio produca log nei punti chiave
     * senza dipendere da output su console o timestamp.
     */
    private static class SpyLogger implements Logger {
        static record Entry(LogLevel level, String message) {}

        private final List<Entry> entries = new ArrayList<>();

        @Override
        public void log(LogLevel level, String message) {
            entries.add(new Entry(level, message));
        }

        boolean hasLevel(LogLevel level) {
            return entries.stream().anyMatch(e -> e.level() == level);
        }
    }

	// Il costruttore rifiuta dipendenze nulle.
	@Test
	void costruttoreRifiutaDipendenzeNulle() {
	    ProjectLoader loader = path -> new Progetto("/p");
	    Parser parser = new StubParser();
	    List<RegolaAnalisi> regoleVuote = List.of();
        Logger logger = new SpyLogger();

	    assertThrows(NullPointerException.class, () -> new AnalisiService(null, parser, regoleVuote));
	    assertThrows(NullPointerException.class, () -> new AnalisiService(loader, null, regoleVuote));
	    assertThrows(NullPointerException.class, () -> new AnalisiService(loader, parser, null));

        // (nuovo costruttore) anche il logger non deve essere null
        assertThrows(NullPointerException.class, () -> new AnalisiService(loader, parser, regoleVuote, null));

        // non deve lanciare se tutto è valido
        assertDoesNotThrow(() -> new AnalisiService(loader, parser, regoleVuote, logger));
	}

    // Esegue analisi, applica regole e conclude l'analisi con un risultato.
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

        SpyLogger logger = new SpyLogger();

        AnalisiService service = new AnalisiService(
                loaderFinto,
                parser,
                List.of(new RegolaNoTODO(), new RegolaNoSystemOutPrintln()),
                logger
        );

        Analisi analisi = service.eseguiAnalisi("/p");

        assertEquals(StatoAnalisi.COMPLETATA, analisi.getStatoAnalisi());
        assertNotNull(analisi.getRisultato());

        // 2 issue attese (println + TO_DO)
        assertEquals(2, analisi.getIssues().size());

        // 1 file analizzato
        assertEquals(1, analisi.getFileAnalizzati().size());
        assertEquals(EsitoParsing.OK, analisi.getFileAnalizzati().get(0).getEsitoParsing());
        assertTrue(analisi.getFileAnalizzati().get(0).parsingRiuscito());

        // Deve esserci almeno un INFO durante l'esecuzione
        assertTrue(logger.hasLevel(LogLevel.INFO), "Mi aspetto almeno un log INFO durante l'esecuzione");
    }

    // Se un file non è analizzabile, viene ignorato.
    @Test
    void ignoraFileNonAnalizzabile() {
        Linguaggio soloTxt = new Linguaggio("TXT", List.of(".txt"));
        FileSorgente f = new FileSorgente("A.java", "/p/A.java", soloTxt, "System.out.println(\"x\");");

        Progetto progetto = new Progetto("/p");
        progetto.aggiungiFileSorgente(f);

        ProjectLoader loaderFinto = path -> progetto;

        // Contatore chiamate al parser: deve restare 0
        final int[] chiamateParser = {0};
        Parser parser = file -> {
            chiamateParser[0]++;
            return null;
        };

        SpyLogger logger = new SpyLogger();

        AnalisiService service = new AnalisiService(
                loaderFinto,
                parser,
                List.of(new RegolaNoSystemOutPrintln()),
                logger
        );

        Analisi analisi = service.eseguiAnalisi("/p");

        assertEquals(StatoAnalisi.COMPLETATA, analisi.getStatoAnalisi());
        assertNotNull(analisi.getRisultato());
        assertEquals(1, analisi.getFileAnalizzati().size());
        assertEquals(0, analisi.getIssues().size());
        assertEquals(0, chiamateParser[0], "Il parser non deve essere invocato per file non analizzabili");

        // Ci si aspetta comunque INFO (es. file ignorato / avvio analisi)
        assertTrue(logger.hasLevel(LogLevel.INFO), "Mi aspetto log INFO anche quando un file viene ignorato");
    }

    // Se il parser restituisce AST null, il service non applica regole e non produce issue.
    @Test
    void nonApplicaRegoleSeParserRestituisceAstNull() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));

        FileSorgente f = new FileSorgente(
                "A.java",
                "/p/A.java",
                java,
                "System.out.println(\"x\");\n// TODO: fix"
        );

        Progetto progetto = new Progetto("/p");
        progetto.aggiungiFileSorgente(f);

        ProjectLoader loaderFinto = path -> progetto;

        // AST null -> deve scattare il return in analizzaFile
        Parser parserCheRitornaNull = file -> null;

        SpyLogger logger = new SpyLogger();

        AnalisiService service = new AnalisiService(
                loaderFinto,
                parserCheRitornaNull,
                List.of(new RegolaNoTODO(), new RegolaNoSystemOutPrintln()),
                logger
        );

        Analisi analisi = service.eseguiAnalisi("/p");

        assertEquals(StatoAnalisi.COMPLETATA, analisi.getStatoAnalisi());
        assertNotNull(analisi.getRisultato());

        // Nessuna issue perché non si applicano regole senza AST valido
        assertEquals(0, analisi.getIssues().size());
        assertEquals(1, analisi.getFileAnalizzati().size());

        // Ci si aspetta un WARNING (parsing non riuscito / AST nullo)
        assertTrue(logger.hasLevel(LogLevel.WARNING) || logger.hasLevel(LogLevel.INFO),
                "Mi aspetto almeno INFO e possibilmente WARNING se AST è nullo");
    }

    // Se il parsing fallisce su un file, lo marca in ERROR e continua sugli altri.
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

        SpyLogger logger = new SpyLogger();

        AnalisiService service = new AnalisiService(
                loaderFinto,
                parserCheFallisceSoloSuBad,
                List.of(new RegolaNoSystemOutPrintln()),
                logger
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

        // Ci si aspetta un WARNING (errore sul singolo file, ma si continua)
        assertTrue(logger.hasLevel(LogLevel.WARNING), "Mi aspetto un WARNING quando un file fallisce parsing");
    }

    // Il service rifiuta projectPath null.
    @Test
    void lanciaEccezioneSeProjectPathNullo() {
        ProjectLoader loaderFinto = path -> new Progetto("/p");
        Parser parser = new StubParser();

        AnalisiService service = new AnalisiService(loaderFinto, parser, List.of());

        assertThrows(NullPointerException.class, () -> service.eseguiAnalisi(null));
    }

    // Ogni esecuzione genera una nuova istanza di Analisi, indipendentemente dalle precedenti.
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

    // Se avviene un errore non gestito dal singolo file, il servizio deve marcare l'analisi come fallita.
    @Test
    void marcaAnalisiFallitaSeErroreGraveAvvieneNelProcessoGlobale() {
        // Progetto che lancia eccezione quando si tenta di ottenere i file sorgenti
        Progetto progettoCheEsplode = new Progetto("/p") {
            @Override
            public List<FileSorgente> getFileSorgenti() {
                throw new RuntimeException("Errore grave");
            }
        };

        ProjectLoader loaderFinto = path -> progettoCheEsplode;
        Parser parser = new StubParser();

        SpyLogger logger = new SpyLogger();

        AnalisiService service = new AnalisiService(loaderFinto, parser, List.of(), logger);

        Analisi analisi = assertDoesNotThrow(() -> service.eseguiAnalisi("/p"));

        // Entra nel catch globale
        assertEquals(StatoAnalisi.FALLITA, analisi.getStatoAnalisi());
        assertNull(analisi.getRisultato(), "Se l'analisi fallisce non deve esserci un risultato");

        // Ci si aspetta un ERROR (errore globale)
        assertTrue(logger.hasLevel(LogLevel.ERROR), "Mi aspetto un ERROR quando l'analisi fallisce globalmente");
    }

    // Le regole non vengono applicate se AST vuoto.
    @Test
    void nonApplicaRegoleSeAstVuoto() {
        // File java analizzabile
        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        FileSorgente f = new FileSorgente("A.java", "/p/A.java", java, "System.out.println(\"x\");\n// TODO: fix");

        Progetto progetto = new Progetto("/p");
        progetto.aggiungiFileSorgente(f);

        ProjectLoader loaderFinto = path -> progetto;

        // Parser che restituisce un AST valido ma vuoto
        Parser parserCheRitornaAstVuoto = file -> new AST();

        SpyLogger logger = new SpyLogger();

        AnalisiService service = new AnalisiService(
                loaderFinto,
                parserCheRitornaAstVuoto,
                List.of(new RegolaNoTODO(), new RegolaNoSystemOutPrintln()),
                logger
        );

        Analisi analisi = service.eseguiAnalisi("/p");

        // L'analisi termina correttamente ma senza issue, perché non si applicano regole con AST vuoto
        assertEquals(StatoAnalisi.COMPLETATA, analisi.getStatoAnalisi());
        assertNotNull(analisi.getRisultato());
        assertTrue(analisi.getIssues().isEmpty(), "Con AST vuoto non devono essere applicate regole");
        assertEquals(1, analisi.getFileAnalizzati().size());

        // Ci si aspetta almeno INFO (avvio/completamento analisi)
        assertTrue(logger.hasLevel(LogLevel.INFO), "Mi aspetto almeno un INFO durante l'analisi");
    }
}