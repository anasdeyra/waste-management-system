package com.ase.wastems.repository;

import com.ase.wastems.domain.SourceSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link SourceSnapshot}. */
public interface SourceSnapshotRepository extends JpaRepository<SourceSnapshot, UUID> {

    Optional<SourceSnapshot> findBySourceDataset(String sourceDataset);

    List<SourceSnapshot> findAllByOrderBySourceDatasetAsc();
}
