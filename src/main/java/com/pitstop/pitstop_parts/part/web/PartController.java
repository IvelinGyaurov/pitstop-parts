package com.pitstop.pitstop_parts.part.web;

import com.pitstop.pitstop_parts.part.web.dto.CreatePartRequest;
import com.pitstop.pitstop_parts.part.web.dto.PartResponse;
import com.pitstop.pitstop_parts.part.service.PartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/parts")
public class PartController {

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @GetMapping
    public List<PartResponse> getAll() {
        return partService.getAllActive();
    }

    @GetMapping("/{id}")
    public PartResponse getById(@PathVariable UUID id) {
        return partService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartResponse create(@Valid @RequestBody CreatePartRequest request) {
        return partService.create(request);
    }
}