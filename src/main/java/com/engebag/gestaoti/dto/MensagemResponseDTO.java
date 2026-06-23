package com.engebag.gestaoti.dto;

public record MensagemResponseDTO(
        Long id, 
        String remetenteNome, 
        String mensagem, 
        String dataEnvio,
        String tipoMensagem,       
        String urlArquivo,         
        String nomeOriginalArquivo 
) {}