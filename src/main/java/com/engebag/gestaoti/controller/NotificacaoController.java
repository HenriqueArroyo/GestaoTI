package com.engebag.gestaoti.controller;

import com.engebag.gestaoti.dto.NotificacaoResponseDTO;
import com.engebag.gestaoti.model.User;
import com.engebag.gestaoti.service.NotificacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    /**
     * GET /notificacoes
     * Retorna as notificações do usuário logado + as gerais.
     */
    @GetMapping
    public ResponseEntity<List<NotificacaoResponseDTO>> listar(
            @AuthenticationPrincipal User usuarioLogado) {

        return ResponseEntity.ok(
                notificacaoService.listarParaUsuario(usuarioLogado.getId())
        );
    }

    /**
     * PATCH /notificacoes/{id}/lida
     * Marca a notificação como lida.
     */
    @PatchMapping("/{id}/lida")
    public ResponseEntity<NotificacaoResponseDTO> marcarLida(
            @PathVariable Long id,
            @AuthenticationPrincipal User usuarioLogado) {

        return ResponseEntity.ok(
                notificacaoService.marcarComoLida(id, usuarioLogado.getId())
        );
    }
}