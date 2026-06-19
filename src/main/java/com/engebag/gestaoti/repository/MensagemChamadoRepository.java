package com.engebag.gestaoti.repository;

import com.engebag.gestaoti.model.MensagemChamado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MensagemChamadoRepository extends JpaRepository<MensagemChamado, Long> {
    // Retorna todo o histórico de um chat ordenado pela data
    List<MensagemChamado> findByChamadoIdOrderByDataEnvioAsc(Long idChamado);
}