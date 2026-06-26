package com.engebag.gestaoti.dto;

public record AvisoResponseDTO(
        Long id,
        String titulo,
        String conteudo,
        String urlImagem,
        String urlAnexo,          
        String empresaAlvo,
        Long idCriador,
        String nomeCriador,
        String cargoCriador,
        String setorCriador,
        String fotoCriador,
        String dataCriacao,
        String editadoEm,         
        Boolean fixado,           
        long totalCurtidas,      
        long totalComentarios,   
        boolean euCurti,          
        boolean euFavoritei       
) {}