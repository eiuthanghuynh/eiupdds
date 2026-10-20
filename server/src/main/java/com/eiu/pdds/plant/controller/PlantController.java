package com.eiu.pdds.plant.controller;

import com.eiu.pdds.common.api.ApiResponse;
import com.eiu.pdds.dto.plant.PlantDto;
import com.eiu.pdds.plant.service.PlantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plants")
@RequiredArgsConstructor
@Tag(name = "Plant Catalog", description = "Endpoints for querying supported agricultural crops and botanical data")
public class PlantController {

    private final PlantService plantService;

    @GetMapping
    @Operation(summary = "Get all supported plants", description = "Retrieves the list of all supported crops (Potato, Tomato, Rice, Corn, etc.)")
    public ResponseEntity<ApiResponse<List<PlantDto>>> getAllPlants() {
        List<PlantDto> plants = plantService.getAllPlants();
        return ResponseEntity.ok(ApiResponse.success("Supported crops retrieved successfully", plants));
    }

    @GetMapping("/{plantCode}")
    @Operation(summary = "Get plant by code", description = "Retrieves details for a specific crop by its unique code (e.g., POTATO, TOMATO)")
    public ResponseEntity<ApiResponse<PlantDto>> getPlantByCode(
            @Parameter(description = "Unique plant code (case-insensitive)", example = "POTATO")
            @PathVariable String plantCode) {
        PlantDto plant = plantService.getPlantByCode(plantCode);
        return ResponseEntity.ok(ApiResponse.success("Crop details retrieved successfully", plant));
    }
}
