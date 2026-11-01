package com.ase.wastems.service;

/** One problem found in an uploaded scenario CSV; row 1 is the header; column may be null. */
public record CsvRowError(int row, String column, String message) {}
