package com.engebag.gestaoti.dto;

public record RegisterRequestDTO(String nome, String email, String senha, String role, String empresaAcesso) {
}