package com.pitstop.pitstop_parts.integration;

import com.pitstop.pitstop_parts.part.exception.InsufficientStockException;
import com.pitstop.pitstop_parts.part.exception.SkuAlreadyExistsException;
import com.pitstop.pitstop_parts.part.model.Part;
import com.pitstop.pitstop_parts.part.repository.PartRepository;
import com.pitstop.pitstop_parts.part.service.PartService;
import com.pitstop.pitstop_parts.part.web.dto.CreatePartRequest;
import com.pitstop.pitstop_parts.part.web.dto.DeductPartItemRequest;
import com.pitstop.pitstop_parts.part.web.dto.DeductPartsRequest;
import com.pitstop.pitstop_parts.part.web.dto.DeductedPartResponse;
import com.pitstop.pitstop_parts.part.web.dto.PartResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PartServiceIntegrationTest {

    @Autowired
    private PartService partService;

    @Autowired
    private PartRepository partRepository;

    @Test
    void createDeductDeleteAndRestore_persistsThroughRepository() {
        CreatePartRequest createRequest = new CreatePartRequest();
        createRequest.setName("Brake Pad");
        createRequest.setSku("BRK-100");
        createRequest.setUnitPrice(new BigDecimal("49.99"));
        createRequest.setQuantityInStock(10);

        PartResponse created = partService.create(createRequest);
        assertNotNull(created.getId());
        assertEquals(1, partRepository.findAllByDeletedAtIsNull().size());

        assertThrows(SkuAlreadyExistsException.class, () -> partService.create(createRequest));

        DeductPartItemRequest item = new DeductPartItemRequest();
        item.setPartId(created.getId());
        item.setQuantity(4);

        DeductPartsRequest deductRequest = new DeductPartsRequest();
        deductRequest.setItems(List.of(item));

        List<DeductedPartResponse> deducted = partService.deduct(deductRequest);
        assertEquals(1, deducted.size());
        assertEquals(4, deducted.get(0).getQuantity());

        Part afterDeduct = partRepository.findByIdAndDeletedAtIsNull(created.getId()).orElseThrow();
        assertEquals(6, afterDeduct.getQuantityInStock());

        DeductPartItemRequest tooMany = new DeductPartItemRequest();
        tooMany.setPartId(created.getId());
        tooMany.setQuantity(100);
        DeductPartsRequest failRequest = new DeductPartsRequest();
        failRequest.setItems(List.of(tooMany));
        assertThrows(InsufficientStockException.class, () -> partService.deduct(failRequest));

        partService.softDelete(created.getId());
        assertTrue(partRepository.findByIdAndDeletedAtIsNull(created.getId()).isEmpty());
        assertEquals(0, partService.getAllActive().size());

        CreatePartRequest restoreRequest = new CreatePartRequest();
        restoreRequest.setName("Brake Pad V2");
        restoreRequest.setSku("BRK-100");
        restoreRequest.setUnitPrice(new BigDecimal("55.00"));
        restoreRequest.setQuantityInStock(8);

        PartResponse restored = partService.create(restoreRequest);
        assertEquals(created.getId(), restored.getId());
        assertEquals("Brake Pad V2", restored.getName());
        assertEquals(8, restored.getQuantityInStock());
        assertEquals(1, partService.getAllActive().size());
    }
}
