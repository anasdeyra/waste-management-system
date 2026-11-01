package com.ase.wastems.repository;

import com.ase.wastems.domain.DemoRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link DemoRecord}. */
public interface DemoRecordRepository extends JpaRepository<DemoRecord, UUID> {

    Optional<DemoRecord> findByDemoKey(String demoKey);

    List<DemoRecord> findAllByOrderByDemoKeyAsc();
}
