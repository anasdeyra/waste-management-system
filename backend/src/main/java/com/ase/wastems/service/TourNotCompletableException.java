package com.ase.wastems.service;

import com.ase.wastems.domain.TourIssue;
import java.util.List;

/** 422 refusal of a completion while stops are pending (US-29). */
public class TourNotCompletableException extends TourIssuesException {

    public TourNotCompletableException(List<TourIssue> issues) {
        super("completed", issues);
    }
}
