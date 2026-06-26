package com.engebag.gestaoti.dto;

import com.engebag.gestaoti.model.Notificacao;

import java.time.LocalDateTime;

public record NotificacaoResponseDTO(
        Long id,
        Long usuarioId,
        boolean geral,
        String titulo,
        String mensagem,
        Long chamadoId,
        boolean lida,
        LocalDateTime criadaEm
) {
    public static NotificacaoResponseDTO from(Notificacao n) {
        return new NotificacaoResponseDTO(
                n.getId(),
                n.getUsuario() != null ? n.getUsuario().getId() : null,
                n.getGeral(),
                n.getTitulo(),
                n.getMensagem(),
                n.getChamado() != null ? n.getChamado().getId() : null,
                n.getLida(),
                n.getCriadaEm()
        );
    }
}