package com.engebag.gestaoti.dto;

import java.time.LocalDateTime;

public record AvisoRequestDTO(
    String titulo,
    String conteudo,
    String urlImagem,
    String empresaAlvo,
    LocalDateTime dataExpiracao
) {}