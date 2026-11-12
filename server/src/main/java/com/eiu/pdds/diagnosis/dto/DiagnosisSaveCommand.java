package com.eiu.pdds.diagnosis.dto;

import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.dto.diagnosis.BoundingBoxDto;
import com.eiu.pdds.plant.entity.Plant;

import java.util.List;
import java.util.UUID;

public record DiagnosisSaveCommand(
        UUID userId,
        Plant plant,
        String s3ImageUrl,
        int imageWidth,
        int imageHeight,
        Double latitude,
        Double longitude,
        String deviceInfo,
        Disease dominantDisease,
        Double dominantConfidence,
        int inferenceTimeMs,
        String rawAiJson,
        List<BoundingBoxDto> boundingBoxes
) {}
