package com.engebag.gestaoti.controller;

import com.engebag.gestaoti.dto.MensagemEnvioDTO;
import com.engebag.gestaoti.dto.MensagemRetornoDTO;
import com.engebag.gestaoti.dto.UsuarioResumoDTO;
import com.engebag.gestaoti.model.CanalComunicacao;
import com.engebag.gestaoti.model.MensagemComunicacao;
import com.engebag.gestaoti.model.User;
import com.engebag.gestaoti.repository.CanalComunicacaoRepository;
import com.engebag.gestaoti.repository.MensagemComunicacaoRepository;
import com.engebag.gestaoti.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

@Controller
public class BatePapoController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate; // Responsável por enviar mensagens via WebSocket

    @Autowired
    private MensagemComunicacaoRepository mensagemRepo;

    @Autowired
    private CanalComunicacaoRepository canalRepo;

    @Autowired
    private UserRepository userRepo;

    @MessageMapping("/batepapo/enviar") // O React enviará para /app/batepapo/enviar
    @Transactional
    public void processarMensagem(@Payload MensagemEnvioDTO dto) {
        
        // 1. Busca remetente e canal
        User remetente = userRepo.findById(dto.getRemetenteId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
                
        CanalComunicacao canal = canalRepo.findById(dto.getCanalId())
                .orElseThrow(() -> new RuntimeException("Canal não encontrado"));

        // 2. Salva no banco de dados
        MensagemComunicacao novaMensagem = new MensagemComunicacao();
        novaMensagem.setCanal(canal);
        novaMensagem.setRemetente(remetente);
        novaMensagem.setConteudo(dto.getConteudo());
        mensagemRepo.save(novaMensagem);

        // 3. Monta o DTO de retorno
        UsuarioResumoDTO resumo = new UsuarioResumoDTO(remetente);

        MensagemRetornoDTO retorno = new MensagemRetornoDTO();
        retorno.setId(novaMensagem.getId());
        retorno.setCanalId(canal.getId());
        retorno.setConteudo(novaMensagem.getConteudo());
        retorno.setEnviadoEm(novaMensagem.getEnviadoEm());
        retorno.setRemetente(resumo);

        // 4. Dispara a mensagem instantaneamente para todos inscritos no canal
        // O React estará escutando no tópico /topic/canal/{id}
        messagingTemplate.convertAndSend("/topic/canal/" + canal.getId(), retorno);
    }
}