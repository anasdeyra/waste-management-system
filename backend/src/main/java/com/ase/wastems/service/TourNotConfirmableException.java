package com.ase.wastems.service;

import com.ase.wastems.domain.TourIssue;
import java.util.List;
import java.util.stream.Collectors;

/** 422 refusal of a confirmation, carrying every blocking issue (US-25). */
public class TourNotConfirmableException extends BusinessRuleException {

    private final List<TourIssue> issues;

    public TourNotConfirmableException(List<TourIssue> issues) {
        super("The tour cannot be confirmed ("
                + issues.stream().map(i -> i.code().name()).collect(Collectors.joining(", ")) + "): "
                + issues.stream().map(TourIssue::message).collect(Collectors.joining(" ")));
        this.issues = List.copyOf(issues);
    }

    public List<TourIssue> getIssues() {
        return issues;
    }
}
