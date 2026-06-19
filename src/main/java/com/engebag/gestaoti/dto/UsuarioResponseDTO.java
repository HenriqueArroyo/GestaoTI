package com.engebag.gestaoti.dto;

import com.engebag.gestaoti.model.User;
import java.time.LocalDateTime;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        String cargo,
        String role,
        String empresaAcesso,
        Long idDepartamento,
        String usuarioRm,
        Boolean utilizaOmaxprensa,
        String fotoPerfil,
        Boolean primeiroAcesso,
        LocalDateTime ultimoLogin,
        Boolean ativo
) {
    // Construtor inteligente que converte um Model User direto para este DTO
    public UsuarioResponseDTO(User user) {
        this(
            user.getId(), user.getNome(), user.getEmail(), user.getCargo(),
            user.getRole(), user.getEmpresaAcesso(), user.getIdDepartamento(),
            user.getUsuarioRm(), user.getUtilizaOmaxprensa(), user.getFotoPerfil(),
            user.getPrimeiroAcesso(), user.getUltimoLogin(), user.getAtivo()
        );
    }
}