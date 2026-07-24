package com.pitstop.pitstop_parts.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pitstop.pitstop_parts.part.repository.PartRepository;
import com.pitstop.pitstop_parts.part.web.dto.CreatePartRequest;
import com.pitstop.pitstop_parts.part.web.dto.DeductPartItemRequest;
import com.pitstop.pitstop_parts.part.web.dto.DeductPartsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PartApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PartRepository partRepository;

    @BeforeEach
    void setUp() {
        partRepository.deleteAll();
    }

    @Test
    void getAll_whenEmpty_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/parts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void create_returnsCreated() throws Exception {
        CreatePartRequest request = new CreatePartRequest();
        request.setName("Oil Filter");
        request.setSku("OIL-200");
        request.setUnitPrice(new BigDecimal("19.99"));
        request.setQuantityInStock(12);

        mockMvc.perform(post("/api/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("OIL-200"))
                .andExpect(jsonPath("$.name").value("Oil Filter"))
                .andExpect(jsonPath("$.quantityInStock").value(12));
    }

    @Test
    void create_withInvalidBody_returnsBadRequest() throws Exception {
        CreatePartRequest request = new CreatePartRequest();
        request.setName("");
        request.setSku("");
        request.setUnitPrice(new BigDecimal("0"));
        request.setQuantityInStock(-1);

        mockMvc.perform(post("/api/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_andDeduct_andDelete_fullFlow() throws Exception {
        CreatePartRequest request = new CreatePartRequest();
        request.setName("Air Filter");
        request.setSku("AIR-300");
        request.setUnitPrice(new BigDecimal("15.00"));
        request.setQuantityInStock(5);

        MvcResult createResult = mockMvc.perform(post("/api/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> created = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        UUID id = UUID.fromString(created.get("id").toString());

        mockMvc.perform(get("/api/parts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("AIR-300"));

        DeductPartItemRequest item = new DeductPartItemRequest();
        item.setPartId(id);
        item.setQuantity(2);
        DeductPartsRequest deductRequest = new DeductPartsRequest();
        deductRequest.setItems(List.of(item));

        mockMvc.perform(post("/api/parts/deduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deductRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].quantity").value(2));

        mockMvc.perform(get("/api/parts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityInStock").value(3));

        mockMvc.perform(delete("/api/parts/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/parts/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_duplicateSku_returnsBadRequest() throws Exception {
        CreatePartRequest request = new CreatePartRequest();
        request.setName("Spark Plug");
        request.setSku("SPK-400");
        request.setUnitPrice(new BigDecimal("8.50"));
        request.setQuantityInStock(20);

        mockMvc.perform(post("/api/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_whenMissing_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/parts/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
