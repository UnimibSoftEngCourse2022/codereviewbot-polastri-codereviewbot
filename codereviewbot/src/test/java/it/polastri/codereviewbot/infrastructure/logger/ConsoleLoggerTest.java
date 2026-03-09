package it.polastri.codereviewbot.infrastructure.logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleLoggerTest {

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream outContent;

    /** Eseguito prima di ogni test, salva il System.out originale e 
    *lo sostituisce con ByteArrayOutputStream. Permette di leggee l'output prodotto 
    *dal logger.
    */
    @BeforeEach
    void setUp() {
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }
    // Eseguito dopo ogni test, ripristina il System.out originale.
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    // Un messaggio di livello INFO deve essere scritto sulla console standard.
    @Test
    void logInfoescriveMessaggioSuConsole() {
        Logger logger = new ConsoleLogger();

        logger.info("Test message");

        String output = outContent.toString();
        assertTrue(output.contains("INFO"));
        assertTrue(output.contains("Test message"));
    }
}