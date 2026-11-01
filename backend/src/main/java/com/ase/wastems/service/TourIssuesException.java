package com.ase.wastems.service;

import com.ase.wastems.domain.TourIssue;
import java.util.List;
import java.util.stream.Collectors;

/** 422 refusal of a lifecycle step, carrying every blocking issue (S6 confirm, S7 start/complete). */
public class TourIssuesException extends BusinessRuleException {

    private final List<TourIssue> issues;

    protected TourIssuesException(String action, List<TourIssue> issues) {
        super("The tour cannot be " + action + " ("
                + issues.stream().map(i -> i.code().name()).collect(Collectors.joining(", ")) + "): "
                + issues.stream().map(TourIssue::message).collect(Collectors.joining(" ")));
        this.issues = List.copyOf(issues);
    }

    public List<TourIssue> getIssues() {
        return issues;
    }
}
