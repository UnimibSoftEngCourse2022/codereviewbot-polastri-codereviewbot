package it.polastri.codereviewbot.infrastructure.logger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoggerTest {

	/** I metodi default (INFO, ERROR, WARNING) dell'interfaccia logger devono 
	* invocare correttamente il metodo log con giusto livello di severità.
	*/
    @Test
    void infoWarningErrorChiamanoLogConLivelloCorretto() {

        class TestLogger implements Logger {
            LogLevel lastLevel;
            String lastMessage;

            @Override
            public void log(LogLevel level, String message) {
                this.lastLevel = level;
                this.lastMessage = message;
            }
        }

        TestLogger logger = new TestLogger();

        logger.info("info");
        assertEquals(LogLevel.INFO, logger.lastLevel);

        logger.warning("warn");
        assertEquals(LogLevel.WARNING, logger.lastLevel);

        logger.error("err");
        assertEquals(LogLevel.ERROR, logger.lastLevel);
    }
    
    /**
     * L'overload error deve invocare log con livello ERROR
     * e concatenare correttamente il messaggio dell'eccezione.
     */
    @Test
    void errorConEccezioneInoltraLivelloERiportaMessaggioEccezione() {

        class TestLogger implements Logger {
            LogLevel lastLevel;
            String lastMessage;

            @Override
            public void log(LogLevel level, String message) {
                this.lastLevel = level;
                this.lastMessage = message;
            }
        }

        TestLogger logger = new TestLogger();
        RuntimeException ex = new RuntimeException("boom");

        logger.error("Errore export", ex);

        assertEquals(LogLevel.ERROR, logger.lastLevel);
        assertEquals("Errore export - boom", logger.lastMessage);
    }
}