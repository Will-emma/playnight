package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.SoireeCreationDto;
import com.takima.backskeleton.DTO.SoireeDetailDto;
import com.takima.backskeleton.DTO.SoireeDto;
import com.takima.backskeleton.services.SoireeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RequestMapping("soirees")
@RestController
public class SoireeController {
    private final SoireeService soireeService;

    public SoireeController(SoireeService soireeService) {
        this.soireeService = soireeService;
    }

    @GetMapping("")
    public List<SoireeDto> listSoirees() {
        return soireeService.findAll();
    }

    @GetMapping("/{id}")
    public SoireeDetailDto getSoireeById(@PathVariable Long id) {
        return soireeService.getById(id);
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public SoireeDetailDto addSoiree(@RequestBody SoireeCreationDto soireeCreationDto) {
        return soireeService.creerSoiree(soireeCreationDto);
    }

    @PutMapping("/{id}")
    public SoireeDetailDto updateSoiree(@RequestBody SoireeCreationDto soireeCreationDto, @PathVariable Long id) {
        return soireeService.modifierSoiree(id, soireeCreationDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSoiree(@PathVariable Long id) {
        soireeService.deleteById(id);
    }
}
