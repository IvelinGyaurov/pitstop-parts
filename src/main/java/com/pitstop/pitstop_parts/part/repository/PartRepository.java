package com.pitstop.pitstop_parts.part.repository;

import com.pitstop.pitstop_parts.part.model.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartRepository extends JpaRepository<Part, UUID> {

    List<Part> findAllByDeletedAtIsNull();

    Optional<Part> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Part> findBySkuAndDeletedAtIsNull(String sku);
}