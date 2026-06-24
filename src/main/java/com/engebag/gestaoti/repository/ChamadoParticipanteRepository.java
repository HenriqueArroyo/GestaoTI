package com.engebag.gestaoti.repository;

import com.engebag.gestaoti.model.Chamado;
import com.engebag.gestaoti.model.ChamadoParticipante;
import com.engebag.gestaoti.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChamadoParticipanteRepository extends JpaRepository<ChamadoParticipante, Long> {

    boolean existsByChamadoAndUsuario(Chamado chamado, User usuario);

    List<ChamadoParticipante> findByUsuario(User usuario);
}