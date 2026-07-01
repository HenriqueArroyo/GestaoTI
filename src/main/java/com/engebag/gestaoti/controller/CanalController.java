package com.engebag.gestaoti.controller;

import com.engebag.gestaoti.model.CanalComunicacao;
import com.engebag.gestaoti.model.User;
import com.engebag.gestaoti.repository.CanalComunicacaoRepository;
import com.engebag.gestaoti.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/canais")
public class CanalController {

    @Autowired
    private CanalComunicacaoRepository canalRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // DTO simples em formato Record para receber os parâmetros do frontend React
    public record CriarCanalDTO(String nome, String tipo, Set<Long> usuarioIds) {}

    @PostMapping("/criar")
    public ResponseEntity<?> criarCanal(@RequestBody CriarCanalDTO dto) {
        try {
            CanalComunicacao canal = new CanalComunicacao();
            canal.setNome(dto.nome());
            canal.setTipo(com.engebag.gestaoti.model.TipoCanal.valueOf(dto.tipo()));
            
            // CORRIGIDO: Utilizando Set (HashSet) para bater com a estrutura do modelo CanalComunicacao
            Set<User> participantes = new HashSet<>();
            for (Long id : dto.usuarioIds()) {
                userRepo.findById(id).ifPresent(participantes::add);
            }
            canal.setParticipantes(participantes);
            
            // Salva o novo canal diretamente no banco de dados
            canalRepo.save(canal);

            // Dispara a notificação via WebSocket para os membros envolvidos
            for (User usuario : participantes) {
                messagingTemplate.convertAndSendToUser(
                    usuario.getId().toString(),
                    "/notificacoes",
                    canal
                );
            }

            return ResponseEntity.ok(canal);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao processar criação de canal: " + e.getMessage());
        }
    }
}
