package com.pitstop.pitstop_parts.part.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class DeductPartItemRequest {

    @NotNull
    private UUID partId;

    @Min(1)
    private int quantity;
}