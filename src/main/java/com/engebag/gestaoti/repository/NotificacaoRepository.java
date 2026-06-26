package com.engebag.gestaoti.repository;

import com.engebag.gestaoti.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    /**
     * Retorna as notificações do usuário (pessoais) + as notificações gerais,
     * ordenadas da mais recente para a mais antiga.
     */
    @Query("""
        SELECT n FROM Notificacao n
        WHERE n.geral = true
           OR n.usuario.id = :usuarioId
        ORDER BY n.criadaEm DESC
        """)
    List<Notificacao> findByUsuarioIdOrGeral(@Param("usuarioId") Long usuarioId);
}