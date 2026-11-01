package com.ase.wastems.service;

import com.ase.wastems.domain.TourIssue;
import java.util.List;

/** 422 refusal of a confirmation, carrying every blocking issue (US-25). */
public class TourNotConfirmableException extends TourIssuesException {

    public TourNotConfirmableException(List<TourIssue> issues) {
        super("confirmed", issues);
    }
}
