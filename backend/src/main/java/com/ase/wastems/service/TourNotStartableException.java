package com.ase.wastems.service;

import com.ase.wastems.domain.TourIssue;
import java.util.List;

/** 422 refusal of a start, carrying every blocking issue (US-27). */
public class TourNotStartableException extends TourIssuesException {

    public TourNotStartableException(List<TourIssue> issues) {
        super("started", issues);
    }
}
