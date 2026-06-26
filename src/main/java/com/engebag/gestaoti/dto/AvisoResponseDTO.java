package com.engebag.gestaoti.dto;

public record AvisoResponseDTO(
    Long id,
    String titulo,
    String conteudo,
    String urlImagem,
    String empresaAlvo,
    Long idCriador, 
    String nomeCriador,
    String cargoCriador,
    String setorCriador,
    String fotoCriador,
    String dataCriacao
) {}