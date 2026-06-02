package com.docint.repository;

import com.docint.entity.SyncJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SyncJobRepository extends JpaRepository<SyncJob, UUID> {

    Page<SyncJob> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
