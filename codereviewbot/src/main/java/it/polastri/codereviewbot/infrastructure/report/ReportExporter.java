package it.polastri.codereviewbot.infrastructure.report;

import it.polastri.codereviewbot.domain.Report;

/**
 * Componente infrastrutturale responsabile dell'esportazione
 * di un Report su un supporto esterno (file, stream, ecc.).
 */

public interface ReportExporter {
	
    void esporta(Report report, String outputPath);
}