package com.engebag.gestaoti.dto;
 
public record ComentarioResponseDTO(
        Long id,
        Long idUsuario,
        String nomeUsuario,
        String fotoUsuario,
        String conteudo,
        Long idPai,        // null se for raiz
        String criadoEm,
        String editadoEm
) {}