package com.ase.wastems.domain;

/** One structural validation finding of a tour draft. */
public record TourIssue(TourIssueCode code, String message) {}
