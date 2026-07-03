package com.pitstop.pitstop_parts.part.web.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class PartResponse {

    private UUID id;

    private String name;

    private String sku;

    private BigDecimal unitPrice;

    private int quantityInStock;
}
