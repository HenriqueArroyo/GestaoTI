package com.engebag.gestaoti.controller;

import com.engebag.gestaoti.model.Departamento;
import com.engebag.gestaoti.repository.DepartamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departamentos")
public class DepartamentoController {

    @Autowired
    private DepartamentoRepository departamentoRepository;

    @GetMapping
    public ResponseEntity<List<Departamento>> listarDepartamentos() {
        // Retorna a lista de departamentos para o frontend preencher o Dropdown
        return ResponseEntity.ok(departamentoRepository.findAll());
    }
}