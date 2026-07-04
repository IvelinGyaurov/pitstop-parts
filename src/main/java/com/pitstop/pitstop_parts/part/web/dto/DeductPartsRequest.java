package com.pitstop.pitstop_parts.part.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class DeductPartsRequest {

    @NotEmpty
    @Valid
    private List<DeductPartItemRequest> items;
}