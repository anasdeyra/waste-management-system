package com.ase.wastems.web.dto;

/** A row-level problem of an uploaded CSV (row 1 is the header; column omitted for record-level errors). */
public record CsvRowErrorResponse(int row, String column, String message) {}
