package com.ase.wastems.service;

import java.util.List;

/** The uploaded CSV is invalid; nothing was imported (HTTP 400 with row errors). */
public class CsvValidationException extends RuntimeException {

    public static final int MAX_REPORTED = 100;

    private final transient List<CsvRowError> errors;
    private final int totalErrors;

    public CsvValidationException(List<CsvRowError> all) {
        super(message(all.size()));
        List<CsvRowError> sorted = new java.util.ArrayList<>(all);
        sorted.sort(java.util.Comparator.comparingInt(CsvRowError::row)
                .thenComparing(e -> e.column() == null ? "" : e.column()));
        this.totalErrors = sorted.size();
        this.errors = List.copyOf(sorted.subList(0, Math.min(MAX_REPORTED, sorted.size())));
    }

    private static String message(int n) {
        return n > MAX_REPORTED
                ? "The CSV has " + n + " errors (first " + MAX_REPORTED + " shown); nothing was imported."
                : "The CSV has " + n + " errors; nothing was imported.";
    }

    public List<CsvRowError> getErrors() {
        return errors;
    }

    public int getTotalErrors() {
        return totalErrors;
    }
}
