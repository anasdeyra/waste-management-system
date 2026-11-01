package com.ase.wastems.web.dto;

import com.ase.wastems.service.DemandProfile;
import java.util.List;

/** Optional parameters of an experiment batch; missing parts default to the fixed 60-scenario matrix. */
public record ExperimentRequest(List<DemandProfile> profiles, List<Long> seeds, List<Integer> sizes,
        Integer repetitions, String label) {}
