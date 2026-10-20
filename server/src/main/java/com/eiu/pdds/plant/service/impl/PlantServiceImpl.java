package com.eiu.pdds.plant.service.impl;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.dto.plant.PlantDto;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.plant.repository.PlantRepository;
import com.eiu.pdds.plant.service.PlantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlantServiceImpl implements PlantService {

    private final PlantRepository plantRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PlantDto> getAllPlants() {
        return plantRepository.findByDeletedAtIsNull().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlantDto getPlantByCode(String plantCode) {
        return plantRepository.findByPlantCode(plantCode.toUpperCase())
                .filter(p -> !p.isDeleted())
                .map(this::toDto)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Plant crop not found with code: " + plantCode));
    }

    private PlantDto toDto(Plant plant) {
        return new PlantDto(
                plant.getId(),
                plant.getPlantCode(),
                plant.getCommonName(),
                plant.getScientificName(),
                plant.getIconUrl(),
                plant.getDescription()
        );
    }
}
