package com.ase.wastems.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Body of a proposal acceptance or rejection: the retained evaluation run to decide on. */
public record ProposalDecisionRequest(@NotNull UUID evaluationRunId) {}
