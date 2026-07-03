package com.pitstop.pitstop_parts.part.service;

import com.pitstop.pitstop_parts.part.web.dto.CreatePartRequest;
import com.pitstop.pitstop_parts.part.web.dto.PartResponse;
import com.pitstop.pitstop_parts.part.exception.PartNotFoundException;
import com.pitstop.pitstop_parts.part.exception.PartNotFoundExceptionMessage;
import com.pitstop.pitstop_parts.part.exception.SkuAlreadyExistsException;
import com.pitstop.pitstop_parts.part.exception.SkuAlreadyExistsExceptionMessage;
import com.pitstop.pitstop_parts.part.model.Part;
import com.pitstop.pitstop_parts.part.repository.PartRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PartService {

    private final PartRepository partRepository;

    public PartService(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    public List<PartResponse> getAllActive() {
        return partRepository.findAllByDeletedAtIsNull().stream()
                .map(this::toResponse)
                .toList();
    }

    public PartResponse getById(UUID id) {
        Part part = partRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new PartNotFoundException(PartNotFoundExceptionMessage.PART_NOT_FOUND));
        return toResponse(part);
    }

    public PartResponse create(CreatePartRequest request) {
        if (partRepository.findBySkuAndDeletedAtIsNull(request.getSku().trim()).isPresent()) {
            throw new SkuAlreadyExistsException(SkuAlreadyExistsExceptionMessage.SKU_ALREADY_EXISTS);
        }

        Part part = Part.builder()
                .name(request.getName().trim())
                .sku(request.getSku().trim())
                .unitPrice(request.getUnitPrice())
                .quantityInStock(request.getQuantityInStock())
                .build();

        partRepository.save(part);
        log.info("Part created: sku={}, quantity={}", part.getSku(), part.getQuantityInStock());
        return toResponse(part);
    }

    private PartResponse toResponse(Part part) {
        return PartResponse.builder()
                .id(part.getId())
                .name(part.getName())
                .sku(part.getSku())
                .unitPrice(part.getUnitPrice())
                .quantityInStock(part.getQuantityInStock())
                .build();
    }
}