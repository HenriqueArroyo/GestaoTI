package com.engebag.gestaoti.controller;

import com.engebag.gestaoti.dto.MensagemRequestDTO;
import com.engebag.gestaoti.dto.MensagemResponseDTO;
import com.engebag.gestaoti.model.MensagemChamado;
import com.engebag.gestaoti.model.User;
import com.engebag.gestaoti.repository.ChamadoRepository;
import com.engebag.gestaoti.repository.MensagemChamadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class ChatController {

    @Autowired
    private MensagemChamadoRepository mensagemRepository;

    @Autowired
    private ChamadoRepository chamadoRepository;

    // Recebe mensagens em /app/chamado/{id}/enviar
    @MessageMapping("/chamado/{idChamado}/enviar")
    // Distribui para todos que estão inscritos em /topic/chamado/{id}
    @SendTo("/topic/chamado/{idChamado}")
    public MensagemResponseDTO processMessage(@DestinationVariable Long idChamado, MensagemRequestDTO dto, Authentication authentication) {
        
        // O usuário foi injetado pelo nosso ChannelInterceptor no momento do CONNECT
        User remetente = (User) authentication.getPrincipal();

        var chamadoOpt = chamadoRepository.findById(idChamado);
        if (chamadoOpt.isEmpty()) {
            throw new RuntimeException("Chamado não encontrado");
        }

        // Salva a mensagem no banco de dados
        MensagemChamado mensagem = new MensagemChamado();
        mensagem.setChamado(chamadoOpt.get());
        mensagem.setUsuario(remetente);
        mensagem.setMensagem(dto.mensagem());
        mensagem.setTipoMensagem("TEXTO");
        
        // Salvamos e já forçamos a data no objeto para não retornar nulo para o frontend agora
        mensagemRepository.save(mensagem); 
        String dataEnvioStr = LocalDateTime.now().toString();

        // Dispara o DTO (Balão de Chat) para todo mundo que estiver na sala
        return new MensagemResponseDTO(
                mensagem.getId(),
                remetente.getNome(),
                mensagem.getMensagem(),
                dataEnvioStr
        );
    }
}