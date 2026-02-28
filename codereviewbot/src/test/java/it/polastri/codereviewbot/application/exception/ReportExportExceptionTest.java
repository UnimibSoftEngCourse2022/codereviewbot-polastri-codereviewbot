package it.polastri.codereviewbot.application.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ReportExportExceptionTest {

	// Test per costruttore
    @Test
    void costruttoreImpostaMessaggioECausa() {
    	// Simulazione RunTimeException
        Throwable causa = new RuntimeException("Errore I/O simulato");

        // Crea un'eccezione custom
        ReportExportException exception =
                new ReportExportException("Errore durante esportazione report", causa);

        // Il messaggio viene preservato
        assertEquals("Errore durante esportazione report", exception.getMessage());

        // La causa viene preservata
        assertSame(causa, exception.getCause());
    }
}