package com.engebag.gestaoti.dto;

public record AvisoResponseDTO(
    Long id,
    String titulo,
    String conteudo,
    String urlImagem,
    String empresaAlvo,
    String nomeCriador,
    String cargoCriador,
    String setorCriador, // <--- NOVO CAMPO
    String fotoCriador,
    String dataCriacao
) {}