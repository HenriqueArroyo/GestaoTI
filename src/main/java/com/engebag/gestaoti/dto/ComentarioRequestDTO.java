package com.engebag.gestaoti.dto;
 
public record ComentarioRequestDTO(
        String conteudo,
        Long idPai     // null = comentário raiz; preenchido = resposta
) {}