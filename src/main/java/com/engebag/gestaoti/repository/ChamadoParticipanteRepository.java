package com.engebag.gestaoti.repository;

import com.engebag.gestaoti.model.Chamado;
import com.engebag.gestaoti.model.ChamadoParticipante;
import com.engebag.gestaoti.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChamadoParticipanteRepository extends JpaRepository<ChamadoParticipante, Long> {
    
    // Verifica rapidamente se já existe o vínculo antes de salvar
    boolean existsByChamadoAndUsuario(Chamado chamado, User usuario);

    Optional<ChamadoParticipante> findByChamadoAndUsuario(Chamado chamado, User usuario);
}