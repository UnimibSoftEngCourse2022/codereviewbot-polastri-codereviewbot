package it.polastri.codereviewbot.application.exception;

public class ReportExportException extends RuntimeException {

    public ReportExportException(String message, Throwable cause) {
        super(message, cause);
    }
}