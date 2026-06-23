package com.engebag.gestaoti.dto;

public record AtualizarChamadoDTO(
        String status,
        String descricao,
        String categoria,
        String criticidade,
        String empresa,
        Boolean slaCumprido 
) {}