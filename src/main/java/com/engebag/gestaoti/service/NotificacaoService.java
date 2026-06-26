package com.engebag.gestaoti.service;

import com.engebag.gestaoti.dto.NotificacaoResponseDTO;
import com.engebag.gestaoti.model.Chamado;
import com.engebag.gestaoti.model.Notificacao;
import com.engebag.gestaoti.model.User;
import com.engebag.gestaoti.repository.NotificacaoRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificacaoService(NotificacaoRepository notificacaoRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.notificacaoRepository = notificacaoRepository;
        this.messagingTemplate      = messagingTemplate;
    }

    // ── Listagem REST ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificacaoResponseDTO> listarParaUsuario(Long usuarioId) {
        return notificacaoRepository
                .findByUsuarioIdOrGeral(usuarioId)
                .stream()
                .map(NotificacaoResponseDTO::from)
                .toList();
    }

    // ── Marcar como lida ──────────────────────────────────────────────────────

    @Transactional
    public NotificacaoResponseDTO marcarComoLida(Long notificacaoId, Long usuarioId) {
        Notificacao n = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada"));

        // Apenas o dono ou notificações gerais podem ser marcadas por qualquer usuário
        if (!n.getGeral() && !n.getUsuario().getId().equals(usuarioId)) {
            throw new RuntimeException("Acesso negado a esta notificação");
        }

        n.setLida(true);
        return NotificacaoResponseDTO.from(notificacaoRepository.save(n));
    }

    // ── Criação + envio via WebSocket ─────────────────────────────────────────

    /**
     * Cria uma notificação pessoal para um usuário específico e envia via STOMP
     * usando fila privada (/user/queue/notificacoes → convertAndSendToUser).
     *
     * @param usuario   destinatário
     * @param chamado   chamado relacionado (pode ser null)
     * @param titulo    título da notificação
     * @param mensagem  corpo da mensagem
     */
    @Transactional
    public void criarEEnviarParaUsuario(User usuario, Chamado chamado,
                                        String titulo, String mensagem) {
        Notificacao n = new Notificacao();
        n.setUsuario(usuario);
        n.setGeral(false);
        n.setTitulo(titulo);
        n.setMensagem(mensagem);
        n.setChamado(chamado);

        NotificacaoResponseDTO dto = NotificacaoResponseDTO.from(notificacaoRepository.save(n));

        // Entrega na fila privada do usuário: /user/{email}/queue/notificacoes
        messagingTemplate.convertAndSendToUser(
                usuario.getEmail(),          // principal name (igual ao JWT sub)
                "/queue/notificacoes",
                dto
        );
    }

    /**
     * Cria uma notificação geral (broadcast) e publica no tópico público.
     *
     * @param chamado   chamado relacionado (pode ser null)
     * @param titulo    título
     * @param mensagem  corpo
     */
    @Transactional
    public void criarEEnviarGeral(Chamado chamado, String titulo, String mensagem) {
        Notificacao n = new Notificacao();
        n.setGeral(true);
        n.setTitulo(titulo);
        n.setMensagem(mensagem);
        n.setChamado(chamado);

        NotificacaoResponseDTO dto = NotificacaoResponseDTO.from(notificacaoRepository.save(n));

        // Broadcast para todos os conectados: /topic/notificacoes
        messagingTemplate.convertAndSend("/topic/notificacoes", dto);
    }
}