
package com.engebag.gestaoti.dto;
 
public record ComentarioResponseDTO(
        Long id,
        Long idUsuario,
        String nomeUsuario,
        String fotoUsuario,
        String conteudo,
        String criadoEm,
        String editadoEm
) {}