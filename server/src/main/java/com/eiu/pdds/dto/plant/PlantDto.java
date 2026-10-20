package com.eiu.pdds.dto.plant;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Plant crop DTO representing agricultural crops supported by EIU PDDS.
 */
@Schema(description = "Supported crop record")
public record PlantDto(
        @Schema(description = "Unique identifier of the plant", example = "11111111-1111-1111-1111-111111111101")
        UUID id,

        @Schema(description = "Unique plant code", example = "POTATO")
        String plantCode,

        @Schema(description = "Common crop name", example = "Potato (Khoai tây)")
        String commonName,

        @Schema(description = "Botanical binomial nomenclature", example = "Solanum tuberosum")
        String scientificName,

        @Schema(description = "URL to crop vector/raster icon", example = "https://cdn.eiu.edu.vn/icons/potato.svg")
        String iconUrl,

        @Schema(description = "Agronomic overview and cultivation context")
        String description
) {}
