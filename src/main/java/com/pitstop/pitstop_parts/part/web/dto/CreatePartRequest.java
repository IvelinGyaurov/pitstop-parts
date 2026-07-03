package com.pitstop.pitstop_parts.part.web.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePartRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 50)
    private String sku;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal unitPrice;

    @Min(0)
    private int quantityInStock;
}
