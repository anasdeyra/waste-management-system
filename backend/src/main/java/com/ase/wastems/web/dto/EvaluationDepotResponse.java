package com.ase.wastems.web.dto;

/** Depot as frozen in an evaluation run. */
public record EvaluationDepotResponse(String name, double longitude, double latitude, String sourceIdentifier) {}
