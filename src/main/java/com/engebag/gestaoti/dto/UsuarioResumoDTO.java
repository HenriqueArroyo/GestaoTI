package com.engebag.gestaoti.dto;

import com.engebag.gestaoti.model.User;

public record UsuarioResumoDTO(
        Long id,
        String nome,
        String email,
        String fotoPerfil
) {
    // Construtor que facilita a conversão
    public UsuarioResumoDTO(User user) {
        this(user.getId(), user.getNome(), user.getEmail(), user.getFotoPerfil());
    }
}