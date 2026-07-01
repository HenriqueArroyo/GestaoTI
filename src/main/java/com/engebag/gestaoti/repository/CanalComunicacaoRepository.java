package com.engebag.gestaoti.repository;

import com.engebag.gestaoti.model.CanalComunicacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CanalComunicacaoRepository extends JpaRepository<CanalComunicacao, Long> {
    
    // Busca todos os canais (grupos ou chats privados) que um usuário participa
    @Query("SELECT c FROM CanalComunicacao c JOIN c.participantes p WHERE p.id = :userId")
    List<CanalComunicacao> findByParticipanteId(@Param("userId") Long userId);

    // Verifica se já existe um chat privado entre dois usuários específicos
    @Query("SELECT c FROM CanalComunicacao c JOIN c.participantes p " +
           "WHERE c.tipo = 'PRIVADO' AND p.id IN (:user1, :user2) " +
           "GROUP BY c.id HAVING COUNT(DISTINCT p.id) = 2")
    Optional<CanalComunicacao> findChatPrivado(@Param("user1") Long user1, @Param("user2") Long user2);
}
