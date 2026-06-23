package com.engebag.gestaoti.dto;

public record AtualizarPerfilDTO(
        String email,
        String senha, 
        String usuarioRm,
        Boolean utilizaOmaxprensa,
        String fotoPerfil
) {}