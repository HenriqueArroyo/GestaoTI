package com.engebag.gestaoti.dto;

public record RegistroPublicoDTO(
        String nome,
        String email,
        String senha,
        String empresaAcesso, 
        String cargo,       
        Long idDepartamento
) {}