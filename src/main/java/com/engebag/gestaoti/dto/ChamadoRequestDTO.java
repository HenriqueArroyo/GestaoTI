package com.engebag.gestaoti.dto;

public record ChamadoRequestDTO(
        String titulo, 
        String descricao, 
        String categoria, 
        String criticidade, 
        String empresa
) {}