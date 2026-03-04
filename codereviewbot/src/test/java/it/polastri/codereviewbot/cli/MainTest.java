package it.polastri.codereviewbot.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import it.polastri.codereviewbot.infrastructure.logger.Logger;
import it.polastri.codereviewbot.infrastructure.loader.ProjectLoader;
import it.polastri.codereviewbot.infrastructure.parser.Parser;

class MainTest {

    private static final Logger NOOP_LOGGER = (level, message) -> { };

    private static final ProjectLoader DUMMY_LOADER = path -> null;
    private static final Parser DUMMY_PARSER = file -> null;

    @Test
    void helpRestituisceExitCode0() {
        int code = Main.run(new String[]{"--help"}, NOOP_LOGGER, DUMMY_LOADER, DUMMY_PARSER);
        assertEquals(0, code);
    }

    @Test
    void progettoMancanteRestituisceExitCode2() {
        int code = Main.run(new String[]{}, NOOP_LOGGER, DUMMY_LOADER, DUMMY_PARSER);
        assertEquals(2, code);
    }

    @Test
    void formatoNonValidoRestituisceExitCode2() {
        int code = Main.run(new String[]{"--project", "./repo", "--format", "TXT"}, NOOP_LOGGER, DUMMY_LOADER, DUMMY_PARSER);
        assertEquals(2, code);
    }
    
    @Test
    void argomentoSconosciutoExit2() {
        int code = Main.run(new String[]{"--boh"}, NOOP_LOGGER, DUMMY_LOADER, DUMMY_PARSER);
        assertEquals(2, code);
    }

    @Test
    void projectSenzaValoreExit2() {
        int code = Main.run(new String[]{"--project"}, NOOP_LOGGER, DUMMY_LOADER, DUMMY_PARSER);
        assertEquals(2, code);
    }
    
    @Test
    void runSenzaDipendenze_helpRestituisce0_eCopreWiring() {
        int code = Main.run(new String[]{"--help"});
        assertEquals(0, code);
    }
}