package com.pitstop.pitstop_parts.part.web.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class DeductedPartResponse {

    private UUID partId;
    private String partName;
    private String sku;
    private int quantity;
    private BigDecimal unitPrice;
}