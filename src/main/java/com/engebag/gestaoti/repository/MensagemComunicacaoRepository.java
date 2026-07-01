package com.engebag.gestaoti.repository;

import com.engebag.gestaoti.model.MensagemComunicacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensagemComunicacaoRepository extends JpaRepository<MensagemComunicacao, Long> {
    
    // Busca o histórico de mensagens de um canal específico, ordenado pela data
    List<MensagemComunicacao> findByCanalIdOrderByEnviadoEmAsc(Long canalId);
}