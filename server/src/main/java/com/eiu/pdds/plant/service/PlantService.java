package com.eiu.pdds.plant.service;

import com.eiu.pdds.dto.plant.PlantDto;

import java.util.List;

public interface PlantService {
    List<PlantDto> getAllPlants();
    PlantDto getPlantByCode(String plantCode);
}
