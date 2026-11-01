package com.ase.wastems.web.dto;

import com.ase.wastems.domain.TourIssue;
import com.ase.wastems.domain.TourIssueCode;

/** One structural validation finding. */
public record TourIssueResponse(TourIssueCode code, String message) {

    public static TourIssueResponse from(TourIssue i) {
        return new TourIssueResponse(i.code(), i.message());
    }
}
