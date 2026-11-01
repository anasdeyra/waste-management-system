package com.ase.wastems.service;

import com.ase.wastems.domain.TourStatus;
import java.util.UUID;

/** One demonstration tour: its target and current lifecycle state. */
public record DemoTourState(String key, UUID tourId, TourStatus targetStatus, TourStatus status) {}
