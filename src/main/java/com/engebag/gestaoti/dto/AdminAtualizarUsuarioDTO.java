package com.engebag.gestaoti.dto;

public record AdminAtualizarUsuarioDTO(
        String nome,
        String email,
        String cargo,
        String role, // ADMIN, TECNICO, USER
        String empresaAcesso, // ENGEBAG, BAG_CLEANER, AMBAS
        Long idDepartamento,
        Boolean ativo
) {}