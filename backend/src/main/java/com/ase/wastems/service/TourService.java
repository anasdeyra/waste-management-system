package com.ase.wastems.service;

import com.ase.wastems.domain.Tour;
import com.ase.wastems.domain.TourStatus;
import com.ase.wastems.repository.TourRepository;
import com.ase.wastems.web.dto.TourResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read operations on collection tours (FR-11, FR-12). */
@Service
@Transactional(readOnly = true)
public class TourService {

    private final TourRepository tourRepository;

    public TourService(TourRepository tourRepository) {
        this.tourRepository = tourRepository;
    }

    public List<TourResponse> findTours() {
        return tourRepository.findAllByOrderByPlannedDateDesc().stream().map(this::toResponse).toList();
    }

    public long countToursPlannedOn(LocalDate date) {
        return tourRepository.findByPlannedDate(date).size();
    }

    public long countToursInProgress() {
        return tourRepository.findByStatus(TourStatus.IN_PROGRESS).size();
    }

    public double totalCollectedKg() {
        return tourRepository.findByStatus(TourStatus.COMPLETED).stream()
                .mapToDouble(Tour::getTotalLoadKg)
                .sum();
    }

    private TourResponse toResponse(Tour tour) {
        List<Long> collectorIds = tour.getCollectors().stream().map(agent -> agent.getId()).toList();
        List<String> collectorNames =
                tour.getCollectors().stream().map(agent -> agent.getFullName()).toList();
        return TourResponse.from(tour, collectorNames, collectorIds);
    }
}
