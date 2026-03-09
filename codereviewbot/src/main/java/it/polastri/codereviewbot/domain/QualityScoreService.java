package it.polastri.codereviewbot.domain;

import java.util.Objects; 

/**
 * Domain Service: calcolo del punteggio di qualità.
 *
 * Regola semplice (estendibile):
 * - base 100
 * - INFO: -1
 * - WARNING: -2
 * - ERROR: -5
 * Clamp finale tra 0 e 100.
 */

public class QualityScoreService {
	
    public int calcolaScore(Analisi analisi) {
        Objects.requireNonNull(analisi, "Analisi non può essere null");
        if (analisi.getStatoAnalisi() != StatoAnalisi.COMPLETATA) throw new IllegalStateException("Score calcolabile solo se analisi completata");

        int penalita = 0;
        for (Issue issue : analisi.getIssues()) {
            Severita sev = issue.getRegola().getSeverita();
            switch (sev) {
                case ERROR -> penalita += 5;
                case WARNING -> penalita += 2;
                case INFO -> penalita += 1;
                default -> { /* no-op */ }
            }
        }

        int score = 100 - penalita;
        if (score < 0) score = 0;
        if (score > 100) score = 100;
        return score;
    }
}