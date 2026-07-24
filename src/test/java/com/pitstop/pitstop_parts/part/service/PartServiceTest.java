package com.pitstop.pitstop_parts.part.service;

import com.pitstop.pitstop_parts.part.exception.InsufficientStockException;
import com.pitstop.pitstop_parts.part.exception.PartNotFoundException;
import com.pitstop.pitstop_parts.part.exception.SkuAlreadyExistsException;
import com.pitstop.pitstop_parts.part.model.Part;
import com.pitstop.pitstop_parts.part.repository.PartRepository;
import com.pitstop.pitstop_parts.part.web.dto.CreatePartRequest;
import com.pitstop.pitstop_parts.part.web.dto.DeductPartItemRequest;
import com.pitstop.pitstop_parts.part.web.dto.DeductPartsRequest;
import com.pitstop.pitstop_parts.part.web.dto.DeductedPartResponse;
import com.pitstop.pitstop_parts.part.web.dto.PartResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartServiceTest {

    @Mock
    private PartRepository partRepository;

    @InjectMocks
    private PartService partService;

    @Test
    void getAllActive_mapsParts() {
        Part part = activePart("OIL-001", 10);
        when(partRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(part));

        List<PartResponse> result = partService.getAllActive();

        assertEquals(1, result.size());
        assertEquals("OIL-001", result.get(0).getSku());
        assertEquals(10, result.get(0).getQuantityInStock());
    }

    @Test
    void getById_whenFound_returnsPart() {
        UUID id = UUID.randomUUID();
        Part part = activePart("BRK-001", 5);
        part.setId(id);
        when(partRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(part));

        PartResponse result = partService.getById(id);

        assertEquals(id, result.getId());
        assertEquals("BRK-001", result.getSku());
    }

    @Test
    void getById_whenMissing_throws() {
        UUID id = UUID.randomUUID();
        when(partRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThrows(PartNotFoundException.class, () -> partService.getById(id));
    }

    @Test
    void create_whenSkuFree_savesNewPart() {
        CreatePartRequest request = createRequest("Oil Filter", "OIL-001", "25.50", 20);
        when(partRepository.findBySkuAndDeletedAtIsNull("OIL-001")).thenReturn(Optional.empty());
        when(partRepository.findBySku("OIL-001")).thenReturn(Optional.empty());
        when(partRepository.save(any(Part.class))).thenAnswer(invocation -> {
            Part saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        PartResponse result = partService.create(request);

        assertEquals("OIL-001", result.getSku());
        assertEquals("Oil Filter", result.getName());
        verify(partRepository).save(any(Part.class));
    }

    @Test
    void create_whenActiveSkuExists_throws() {
        CreatePartRequest request = createRequest("Oil Filter", "OIL-001", "25.50", 20);
        when(partRepository.findBySkuAndDeletedAtIsNull("OIL-001"))
                .thenReturn(Optional.of(activePart("OIL-001", 5)));

        assertThrows(SkuAlreadyExistsException.class, () -> partService.create(request));
        verify(partRepository, never()).save(any());
    }

    @Test
    void create_whenSoftDeletedSkuExists_restores() {
        CreatePartRequest request = createRequest("Oil Filter Pro", "OIL-001", "30.00", 15);
        Part deleted = activePart("OIL-001", 2);
        deleted.setDeletedAt(LocalDateTime.now().minusDays(1));
        deleted.setId(UUID.randomUUID());

        when(partRepository.findBySkuAndDeletedAtIsNull("OIL-001")).thenReturn(Optional.empty());
        when(partRepository.findBySku("OIL-001")).thenReturn(Optional.of(deleted));
        when(partRepository.save(any(Part.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PartResponse result = partService.create(request);

        assertNull(deleted.getDeletedAt());
        assertEquals("Oil Filter Pro", deleted.getName());
        assertEquals(15, deleted.getQuantityInStock());
        assertEquals(new BigDecimal("30.00"), deleted.getUnitPrice());
        assertEquals("OIL-001", result.getSku());
    }

    @Test
    void deduct_whenEnoughStock_reducesQuantity() {
        UUID partId = UUID.randomUUID();
        Part part = activePart("OIL-001", 10);
        part.setId(partId);

        DeductPartItemRequest item = new DeductPartItemRequest();
        item.setPartId(partId);
        item.setQuantity(3);

        DeductPartsRequest request = new DeductPartsRequest();
        request.setItems(List.of(item));

        when(partRepository.findByIdAndDeletedAtIsNull(partId)).thenReturn(Optional.of(part));
        when(partRepository.save(any(Part.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<DeductedPartResponse> result = partService.deduct(request);

        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getQuantity());
        assertEquals(7, part.getQuantityInStock());
        verify(partRepository).save(part);
    }

    @Test
    void deduct_whenInsufficientStock_throws() {
        UUID partId = UUID.randomUUID();
        Part part = activePart("OIL-001", 2);
        part.setId(partId);

        DeductPartItemRequest item = new DeductPartItemRequest();
        item.setPartId(partId);
        item.setQuantity(5);

        DeductPartsRequest request = new DeductPartsRequest();
        request.setItems(List.of(item));

        when(partRepository.findByIdAndDeletedAtIsNull(partId)).thenReturn(Optional.of(part));

        assertThrows(InsufficientStockException.class, () -> partService.deduct(request));
        verify(partRepository, never()).save(any());
    }

    @Test
    void deduct_whenPartMissing_throws() {
        UUID partId = UUID.randomUUID();
        DeductPartItemRequest item = new DeductPartItemRequest();
        item.setPartId(partId);
        item.setQuantity(1);

        DeductPartsRequest request = new DeductPartsRequest();
        request.setItems(List.of(item));

        when(partRepository.findByIdAndDeletedAtIsNull(partId)).thenReturn(Optional.empty());

        assertThrows(PartNotFoundException.class, () -> partService.deduct(request));
    }

    @Test
    void softDelete_whenFound_setsDeletedAt() {
        UUID id = UUID.randomUUID();
        Part part = activePart("OIL-001", 10);
        part.setId(id);

        when(partRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(part));
        when(partRepository.save(any(Part.class))).thenAnswer(invocation -> invocation.getArgument(0));

        partService.softDelete(id);

        assertNotNull(part.getDeletedAt());
        ArgumentCaptor<Part> captor = ArgumentCaptor.forClass(Part.class);
        verify(partRepository).save(captor.capture());
        assertNotNull(captor.getValue().getDeletedAt());
    }

    @Test
    void softDelete_whenMissing_throws() {
        UUID id = UUID.randomUUID();
        when(partRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThrows(PartNotFoundException.class, () -> partService.softDelete(id));
    }

    private Part activePart(String sku, int quantity) {
        return Part.builder()
                .name("Test Part")
                .sku(sku)
                .unitPrice(new BigDecimal("10.00"))
                .quantityInStock(quantity)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }

    private CreatePartRequest createRequest(String name, String sku, String price, int qty) {
        CreatePartRequest request = new CreatePartRequest();
        request.setName(name);
        request.setSku(sku);
        request.setUnitPrice(new BigDecimal(price));
        request.setQuantityInStock(qty);
        return request;
    }
}
