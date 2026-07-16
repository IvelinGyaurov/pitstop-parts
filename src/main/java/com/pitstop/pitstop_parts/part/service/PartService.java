package com.pitstop.pitstop_parts.part.service;

import com.pitstop.pitstop_parts.part.exception.*;
import com.pitstop.pitstop_parts.part.web.dto.*;
import com.pitstop.pitstop_parts.part.model.Part;
import com.pitstop.pitstop_parts.part.repository.PartRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        String sku = request.getSku().trim();
        String name = request.getName().trim();

        if (partRepository.findBySkuAndDeletedAtIsNull(sku).isPresent()) {
            throw new SkuAlreadyExistsException(SkuAlreadyExistsExceptionMessage.SKU_ALREADY_EXISTS);
        }

        Optional<Part> existing = partRepository.findBySku(sku);
        if (existing.isPresent() && existing.get().isDeleted()) {
            Part part = existing.get();
            part.setDeletedAt(null);
            part.setName(name);
            part.setUnitPrice(request.getUnitPrice());
            part.setQuantityInStock(request.getQuantityInStock());
            partRepository.save(part);

            log.info("Part restored: sku={}, quantity={}", part.getSku(), part.getQuantityInStock());
            return toResponse(part);
        }

        Part part = Part.builder()
                .name(name)
                .sku(sku)
                .unitPrice(request.getUnitPrice())
                .quantityInStock(request.getQuantityInStock())
                .build();

        partRepository.save(part);
        log.info("Part created: sku={}, quantity={}", part.getSku(), part.getQuantityInStock());
        return toResponse(part);
    }

    public List<DeductedPartResponse> deduct(DeductPartsRequest request) {
        List<DeductedPartResponse> result = new ArrayList<>();

        for (DeductPartItemRequest item : request.getItems()) {
            Part part = partRepository.findByIdAndDeletedAtIsNull(item.getPartId())
                    .orElseThrow(() -> new PartNotFoundException(PartNotFoundExceptionMessage.PART_NOT_FOUND));

            if (part.getQuantityInStock() < item.getQuantity()) {
                throw new InsufficientStockException(InsufficientStockExceptionMessage.INSUFFICIENT_STOCK);
            }

            part.setQuantityInStock(part.getQuantityInStock() - item.getQuantity());
            partRepository.save(part);

            log.info("Part deducted: sku={}, quantity={}, remaining={}",
                    part.getSku(), item.getQuantity(), part.getQuantityInStock());

            result.add(DeductedPartResponse.builder()
                    .partId(part.getId())
                    .partName(part.getName())
                    .sku(part.getSku())
                    .quantity(item.getQuantity())
                    .unitPrice(part.getUnitPrice())
                    .build());
        }

        return result;
    }

    public void softDelete(UUID id) {
        Part part = partRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new PartNotFoundException(PartNotFoundExceptionMessage.PART_NOT_FOUND));

        part.setDeletedAt(LocalDateTime.now());
        partRepository.save(part);
        log.info("Part soft-deleted: sku={}", part.getSku());
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