package com.ase.wastems.service;

/** Depot as frozen in an evaluation scenario. */
public record FrozenDepot(String name, double longitude, double latitude, String sourceIdentifier) {}
