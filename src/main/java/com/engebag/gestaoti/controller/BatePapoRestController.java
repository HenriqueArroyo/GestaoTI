package com.engebag.gestaoti.controller;

import com.engebag.gestaoti.dto.MensagemRetornoDTO; // Reutilize seu DTO de retorno
import com.engebag.gestaoti.repository.MensagemComunicacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/batepapo")
public class BatePapoRestController {

    @Autowired
    private MensagemComunicacaoRepository mensagemRepo;

    // Rota para carregar histórico: GET /api/batepapo/historico/{canalId}
    @GetMapping("/historico/{canalId}")
    public ResponseEntity<List<MensagemRetornoDTO>> carregarHistorico(@PathVariable Long canalId) {
        var mensagens = mensagemRepo.findByCanalIdOrderByEnviadoEmAsc(canalId);
        
        // Converte entidades para DTOs (você pode fazer um método de conversão no DTO)
        List<MensagemRetornoDTO> dtos = mensagens.stream().map(msg -> {
            MensagemRetornoDTO dto = new MensagemRetornoDTO();
            dto.setId(msg.getId());
            dto.setCanalId(msg.getCanal().getId());
            dto.setConteudo(msg.getConteudo());
            dto.setEnviadoEm(msg.getEnviadoEm());
            // Aqui você mapeia o remetente...
            return dto;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
}