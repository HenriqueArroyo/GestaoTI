package com.engebag.gestaoti.repository;

import com.engebag.gestaoti.model.Chamado;
import com.engebag.gestaoti.model.ChamadoParticipante;
import com.engebag.gestaoti.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional; // Importação necessária para o Optional

public interface ChamadoParticipanteRepository extends JpaRepository<ChamadoParticipante, Long> {

    // Mantido o que você já tinha
    boolean existsByChamadoAndUsuario(Chamado chamado, User usuario);
    List<ChamadoParticipante> findByUsuario(User usuario);

    // SOLUÇÃO ERRO 1: Método para buscar todos os participantes de um chamado específico
    List<ChamadoParticipante> findByChamado(Chamado chamado);

    // SOLUÇÃO ERRO 2: Método para retornar o objeto inteiro da relação (para poder deletar)
    Optional<ChamadoParticipante> findByChamadoAndUsuario(Chamado chamado, User usuario);
}