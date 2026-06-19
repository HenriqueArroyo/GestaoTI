package com.engebag.gestaoti.dto;

public record RegisterRequestDTO(
        String nome, 
        String email, 
        String senha, 
        String role, 
        String empresaAcesso,
        String cargo,
        Long idDepartamento,
        String usuarioRm,
        Boolean utilizaOmaxprensa
) {}